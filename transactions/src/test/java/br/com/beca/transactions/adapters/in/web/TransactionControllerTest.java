package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.dto.TransactionWebMapper;
import br.com.beca.transactions.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.in.GetAccountUseCase;
import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.in.ListTransactionsUseCase;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TransactionWebMapper.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTransactionUseCase createTransactionUseCase;
    @MockitoBean
    private GetTransactionUseCase getTransactionUseCase;
    @MockitoBean
    private ListTransactionsUseCase listTransactionsUseCase;
    @MockitoBean
    private GetAccountUseCase getAccountUseCase;

    private static Transaction transaction(TransactionStatus status, String reason) {
        return new Transaction(Ids.TX_ID, Ids.USER, null, TransactionType.PURCHASE,
                new BigDecimal("89.90"), Category.FOOD, "mercado", status, reason,
                LocalDateTime.of(2026, 11, 10, 10, 0));
    }

    private static String validBody() {
        return """
                {"userId":"%s","type":"PURCHASE","amount":89.90,"category":"FOOD","description":"mercado"}
                """.formatted(Ids.USER);
    }

    @Test
    void shouldReturn201WithIdAndStatusWhenTransactionIsApproved() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenReturn(transaction(TransactionStatus.APPROVED, null));

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(Ids.TX_ID.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn201WithReasonWhenTransactionIsRejected() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenReturn(transaction(TransactionStatus.REJECTED, "Saldo insuficiente."));

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Saldo insuficiente."));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content("{\"userId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou mal formatado."));
    }

    @Test
    void shouldReturn400WhenTypeIsUnknown() throws Exception {
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"type\":\"PIX\",\"amount\":10}".formatted(Ids.USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn500WithFixedMessageWhenUnexpectedErrorOccurs() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenThrow(new RuntimeException("detalhe interno"));

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno. Tente novamente mais tarde."));
    }

    @Test
    void shouldReturnTransactionById() throws Exception {
        when(getTransactionUseCase.findById(Ids.TX_ID)).thenReturn(transaction(TransactionStatus.APPROVED, null));

        mockMvc.perform(get("/transactions/{id}", Ids.TX_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(89.90))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    void shouldReturn404WhenTransactionDoesNotExist() throws Exception {
        when(getTransactionUseCase.findById(Ids.UNKNOWN))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada."));

        mockMvc.perform(get("/transactions/{id}", Ids.UNKNOWN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transação não encontrada."));
    }

    @Test
    void shouldReturn400WhenIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/transactions/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    @Test
    void shouldListTransactionsWithFilters() throws Exception {
        when(listTransactionsUseCase.list(eq(Ids.USER), eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 30)), eq(TransactionType.PURCHASE)))
                .thenReturn(List.of(transaction(TransactionStatus.APPROVED, null)));

        mockMvc.perform(get("/transactions")
                        .param("userId", Ids.USER.toString())
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-30")
                        .param("type", "PURCHASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(Ids.TX_ID.toString()));
    }

    @Test
    void shouldReturn400WhenUserIdParameterIsMissing() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: 'userId'."));
    }

    @Test
    void shouldReturnBalanceFromAccountService() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER)).thenReturn(
                new AccountLimits(new BigDecimal("1500.00"), new BigDecimal("500.00"), new BigDecimal("1000.00")));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.perTransactionLimit").value(500.00))
                .andExpect(jsonPath("$.dailyLimit").value(1000.00));
    }

    @Test
    void shouldReturn503WhenAccountServiceIsDown() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER))
                .thenThrow(new AccountServiceUnavailableException("fora do ar"));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Serviço de saldo indisponível no momento. Tente novamente mais tarde."));
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER))
                .thenThrow(new AccountNotFoundException("Conta do usuário não encontrada no serviço de saldo."));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER))
                .andExpect(status().isNotFound());
    }
}
