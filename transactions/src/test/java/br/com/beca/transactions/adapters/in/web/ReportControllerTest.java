package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.transactions.adapters.in.web.security.SecurityConfig;
import br.com.beca.transactions.adapters.in.web.security.UsersServiceAuthenticationProvider;
import br.com.beca.transactions.application.port.in.AnalyzeExpensesUseCase;
import br.com.beca.transactions.application.port.in.GenerateTransactionsReportUseCase;
import br.com.beca.transactions.application.port.out.UserAuthenticationPort;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.ExpenseSummary;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, UsersServiceAuthenticationProvider.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyzeExpensesUseCase analyzeExpensesUseCase;
    @MockitoBean
    private GenerateTransactionsReportUseCase generateTransactionsReportUseCase;
    @MockitoBean
    private UserAuthenticationPort userAuthenticationPort;

    @BeforeEach
    void setUp() {
        when(userAuthenticationPort.authenticate("maria@email.com", "Senha@123"))
                .thenReturn(new AuthenticatedUser(Ids.USER, "Maria", "maria@email.com"));
    }

    @Test
    void shouldReturn401WithoutCredentials() throws Exception {
        mockMvc.perform(get("/expenses/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/transactions/report")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnExpenseSummaryOfTheAuthenticatedUser() throws Exception {
        ExpenseSummary summary = new ExpenseSummary(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), new BigDecimal("300.00"),
                List.of(new ExpenseSummary.CategoryTotal(Category.FOOD, new BigDecimal("200.00")),
                        new ExpenseSummary.CategoryTotal(Category.LEISURE, new BigDecimal("100.00"))),
                List.of(new ExpenseSummary.DayTotal(LocalDate.of(2026, 11, 3), new BigDecimal("300.00"))),
                List.of(new ExpenseSummary.MonthTotal(YearMonth.of(2026, 11), new BigDecimal("300.00"))));
        when(analyzeExpensesUseCase.analyze(eq(Ids.USER), eq(null), eq(null))).thenReturn(summary);

        mockMvc.perform(get("/expenses/summary").with(httpBasic("maria@email.com", "Senha@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-11-01"))
                .andExpect(jsonPath("$.to").value("2026-11-30"))
                .andExpect(jsonPath("$.total").value(300.00))
                .andExpect(jsonPath("$.byCategory.length()").value(2))
                .andExpect(jsonPath("$.byCategory[0].category").value("FOOD"))
                .andExpect(jsonPath("$.byDay[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.byMonth[0].month").value("2026-11"));
    }

    @Test
    void shouldPassTheGivenPeriodToTheUseCase() throws Exception {
        when(analyzeExpensesUseCase.analyze(any(), any(), any())).thenReturn(new ExpenseSummary(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), new BigDecimal("0.00"),
                List.of(), List.of(), List.of()));

        mockMvc.perform(get("/expenses/summary").with(httpBasic("maria@email.com", "Senha@123"))
                        .param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00));
    }

    @Test
    void shouldReturn400WhenPeriodIsInverted() throws Exception {
        when(analyzeExpensesUseCase.analyze(any(), any(), any()))
                .thenThrow(new InvalidTransactionException("A data inicial não pode ser depois da data final."));

        mockMvc.perform(get("/expenses/summary").with(httpBasic("maria@email.com", "Senha@123"))
                        .param("from", "2026-11-20").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data inicial não pode ser depois da data final."));
    }

    @Test
    void shouldReturn400WhenDateIsInvalid() throws Exception {
        mockMvc.perform(get("/expenses/summary").with(httpBasic("maria@email.com", "Senha@123"))
                        .param("from", "ontem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'from'."));
    }

    @Test
    void shouldDownloadCsvWithUtf8BomAndExcelFriendlyHeaders() throws Exception {
        when(generateTransactionsReportUseCase.generateCsv(eq(Ids.USER), eq(null), eq(null)))
                .thenReturn("data;tipo\r\n05/11/2026 14:30;Transferência\r\n");

        MvcResult result = mockMvc.perform(get("/transactions/report")
                        .with(httpBasic("maria@email.com", "Senha@123")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transacoes.csv\""))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertEquals((byte) 0xEF, body[0]);
        assertEquals((byte) 0xBB, body[1]);
        assertEquals((byte) 0xBF, body[2]);
        String text = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("data;tipo"));
        assertTrue(text.contains("Transferência"));
    }
}
