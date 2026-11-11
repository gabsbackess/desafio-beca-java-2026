package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.dto.BalanceResponse;
import br.com.beca.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.transactions.adapters.in.web.dto.TransactionResponse;
import br.com.beca.transactions.adapters.in.web.dto.TransactionWebMapper;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.in.GetAccountUseCase;
import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.in.ListTransactionsUseCase;
import br.com.beca.transactions.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Transações", description = "Registro, consulta e saldo")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final TransactionWebMapper mapper;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                 GetTransactionUseCase getTransactionUseCase,
                                 ListTransactionsUseCase listTransactionsUseCase,
                                 GetAccountUseCase getAccountUseCase,
                                 TransactionWebMapper mapper) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma transação",
            description = "A resposta traz o id e o status (APPROVED ou REJECTED). Se rejeitada, traz o motivo.")
    @ApiResponse(responseCode = "201", description = "Transação gravada (aprovada ou rejeitada)")
    @ApiResponse(responseCode = "400", description = "Corpo inválido")
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return mapper.toResponse(createTransactionUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Consulta uma transação pelo id")
    @ApiResponse(responseCode = "404", description = "Transação não encontrada")
    public TransactionResponse findById(@PathVariable UUID id) {
        return mapper.toResponse(getTransactionUseCase.findById(id));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lista as transações de um usuário",
            description = "Filtros opcionais por período (from e to, formato AAAA-MM-DD, inclusivos) e por tipo.")
    public List<TransactionResponse> list(
            @RequestParam UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type) {
        return listTransactionsUseCase.list(userId, from, to, type).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/accounts/{userId}/balance")
    @Operation(summary = "Saldo e limites atuais do usuário, lidos da API Mock")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada na API Mock")
    @ApiResponse(responseCode = "503", description = "API Mock indisponível")
    public BalanceResponse balance(@PathVariable UUID userId) {
        return mapper.toBalanceResponse(userId, getAccountUseCase.getAccount(userId));
    }
}
