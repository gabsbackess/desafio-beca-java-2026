package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.dto.ExpenseSummaryResponse;
import br.com.beca.transactions.adapters.in.web.dto.ExpenseSummaryResponse.CategoryTotalResponse;
import br.com.beca.transactions.adapters.in.web.dto.ExpenseSummaryResponse.DayTotalResponse;
import br.com.beca.transactions.adapters.in.web.dto.ExpenseSummaryResponse.MonthTotalResponse;
import br.com.beca.transactions.application.port.in.AnalyzeExpensesUseCase;
import br.com.beca.transactions.application.port.in.GenerateTransactionsReportUseCase;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.domain.model.ExpenseSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Análise de despesas e relatório CSV. Sempre do usuário autenticado. */
@RestController
@Tag(name = "Relatórios", description = "Análise de despesas e relatório CSV")
public class ReportController {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final AnalyzeExpensesUseCase analyzeExpensesUseCase;
    private final GenerateTransactionsReportUseCase generateTransactionsReportUseCase;

    public ReportController(AnalyzeExpensesUseCase analyzeExpensesUseCase,
                            GenerateTransactionsReportUseCase generateTransactionsReportUseCase) {
        this.analyzeExpensesUseCase = analyzeExpensesUseCase;
        this.generateTransactionsReportUseCase = generateTransactionsReportUseCase;
    }

    @GetMapping("/expenses/summary")
    @Operation(summary = "Resumo das despesas do usuário autenticado",
            description = "Total por categoria, por dia e por mês, e o total geral. Só contam as saídas aprovadas "
                    + "(retirada, transferência enviada e compra). Sem datas, vale o mês atual.")
    @ApiResponse(responseCode = "200", description = "Resumo do período",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                            {"from": "2026-11-01", "to": "2026-11-30", "total": 540.50, "byCategory": [{"category": "TRANSPORT", "total": 230.00}, {"category": "FOOD", "total": 205.50}, {"category": "LEISURE", "total": 105.00}], "byDay": [{"date": "2026-11-10", "total": 540.50}], "byMonth": [{"month": "2026-11", "total": 540.50}]}
                            """)))
    @ApiResponse(responseCode = "400", description = "Período inválido (data inicial depois da final)")
    public ExpenseSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return toResponse(analyzeExpensesUseCase.analyze(user.id(), from, to));
    }

    @GetMapping("/transactions/report")
    @Operation(summary = "Baixa o relatório de transações em CSV",
            description = "Arquivo para o Excel em português (UTF-8 com BOM, separador ponto e vírgula, vírgula "
                    + "decimal). Colunas: data, tipo, categoria, descricao, valor_brl, status, motivo, valor_usd, "
                    + "valor_eur. Sem datas, vale o mês atual.")
    @ApiResponse(responseCode = "200", description = "Arquivo CSV", content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "text/csv"))
    public ResponseEntity<byte[]> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        byte[] body = withBom(generateTransactionsReportUseCase.generateCsv(user.id(), from, to));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("transacoes.csv").build().toString())
                .body(body);
    }

    private static byte[] withBom(String csv) {
        byte[] text = csv.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + text.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(text, 0, result, UTF8_BOM.length, text.length);
        return result;
    }

    private static ExpenseSummaryResponse toResponse(ExpenseSummary summary) {
        return new ExpenseSummaryResponse(
                summary.from(),
                summary.to(),
                summary.total(),
                summary.byCategory().stream()
                        .map(item -> new CategoryTotalResponse(item.category(), item.total())).toList(),
                summary.byDay().stream()
                        .map(item -> new DayTotalResponse(item.date(), item.total())).toList(),
                summary.byMonth().stream()
                        .map(item -> new MonthTotalResponse(item.month().toString(), item.total())).toList());
    }
}
