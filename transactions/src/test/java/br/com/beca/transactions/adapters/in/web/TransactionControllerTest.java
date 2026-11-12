package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.dto.TransactionWebMapper;
import br.com.beca.transactions.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.transactions.adapters.in.web.security.SecurityConfig;
import br.com.beca.transactions.adapters.in.web.security.UsersServiceAuthenticationProvider;
import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.in.GetAccountUseCase;
import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.in.ListTransactionsUseCase;
import br.com.beca.transactions.application.port.out.UserAuthenticationPort;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.AuthServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidCredentialsException;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TransactionWebMapper.class,
        SecurityConfig.class, UsersServiceAuthenticationProvider.class})
class TransactionControllerTest {

    private static final String EMAIL = "maria@email.com";
    private static final String PASSWORD = "Senha@123";

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
    @MockitoBean
    private UserAuthenticationPort userAuthenticationPort;

    @BeforeEach
    void setUp() {
        when(userAuthenticationPort.authenticate(EMAIL, PASSWORD))
                .thenReturn(new AuthenticatedUser(Ids.USER, "Maria", EMAIL));
        when(userAuthenticationPort.authenticate(EMAIL, "errada"))
                .thenThrow(new InvalidCredentialsException("Credenciais inválidas."));
    }

    private static RequestPostProcessor maria() {
        return httpBasic(EMAIL, PASSWORD);
    }

    private static Transaction transaction(TransactionStatus status, String reason, List<ExchangeRate> rates) {
        return new Transaction(Ids.TX_ID, Ids.USER, null, TransactionType.PURCHASE, new BigDecimal("89.90"),
                Category.FOOD, "mercado", status, reason, LocalDateTime.of(2026, 11, 10, 10, 0), rates);
    }

    private static String validBody() {
        return """
                {"type":"PURCHASE","amount":89.90,"category":"FOOD","description":"mercado"}
                """;
    }

    // ----- autenticação -----

    @Test
    void shouldReturn401InStandardFormatWithoutCredentials() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas ou ausentes."));

        verifyNoInteractions(listTransactionsUseCase);
    }

    @Test
    void shouldReturn401WhenPasswordIsWrong() throws Exception {
        mockMvc.perform(get("/transactions").with(httpBasic(EMAIL, "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void shouldReturn503WhenUsersServiceIsDown() throws Exception {
        when(userAuthenticationPort.authenticate(EMAIL, "qualquer"))
                .thenThrow(new AuthServiceUnavailableException("fora do ar"));

        mockMvc.perform(get("/transactions").with(httpBasic(EMAIL, "qualquer")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // ----- criação -----

    @Test
    void shouldCreateTransactionForTheAuthenticatedUser() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenReturn(transaction(TransactionStatus.APPROVED, null, List.of()));

        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(Ids.TX_ID.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createTransactionUseCase).create(captor.capture());
        assertEquals(Ids.USER, captor.getValue().userId());
    }

    @Test
    void shouldIgnoreUserIdSentInTheBodyAndUseTheAuthenticatedUser() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenReturn(transaction(TransactionStatus.APPROVED, null, List.of()));

        mockMvc.perform(post("/transactions").with(maria()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"type\":\"DEPOSIT\",\"amount\":10}".formatted(Ids.OTHER_USER)))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createTransactionUseCase).create(captor.capture());
        assertEquals(Ids.USER, captor.getValue().userId());
    }

    @Test
    void shouldReturn201WithReasonWhenTransactionIsRejected() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenReturn(transaction(TransactionStatus.REJECTED, "Saldo insuficiente.", List.of()));

        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Saldo insuficiente."));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou mal formatado."));
    }

    @Test
    void shouldReturn400WhenTypeIsUnknown() throws Exception {
        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PIX\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500WithFixedMessageWhenUnexpectedErrorOccurs() throws Exception {
        when(createTransactionUseCase.create(any(CreateTransactionCommand.class)))
                .thenThrow(new RuntimeException("detalhe interno"));

        mockMvc.perform(post("/transactions").with(maria())
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno. Tente novamente mais tarde."));
    }

    // ----- consulta e conversão -----

    @Test
    void shouldReturnTransactionWithConvertedValuesRateAndDate() throws Exception {
        List<ExchangeRate> rates = List.of(
                new ExchangeRate(Currency.USD, new BigDecimal("5.00"), LocalDate.of(2026, 11, 10)),
                new ExchangeRate(Currency.EUR, new BigDecimal("6.00"), LocalDate.of(2026, 11, 9)));
        when(getTransactionUseCase.findById(Ids.TX_ID, Ids.USER))
                .thenReturn(transaction(TransactionStatus.APPROVED, null, rates));

        mockMvc.perform(get("/transactions/{id}", Ids.TX_ID).with(maria()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(89.90))
                .andExpect(jsonPath("$.conversionAvailable").value(true))
                .andExpect(jsonPath("$.conversions[0].currency").value("USD"))
                .andExpect(jsonPath("$.conversions[0].rate").value(5.00))
                .andExpect(jsonPath("$.conversions[0].rateDate").value("2026-11-10"))
                .andExpect(jsonPath("$.conversions[0].convertedAmount").value(17.98))
                .andExpect(jsonPath("$.conversions[1].currency").value("EUR"))
                .andExpect(jsonPath("$.conversions[1].convertedAmount").value(14.98));
    }

    @Test
    void shouldSignalThatConversionIsUnavailableWhenThereAreNoRates() throws Exception {
        when(getTransactionUseCase.findById(Ids.TX_ID, Ids.USER))
                .thenReturn(transaction(TransactionStatus.APPROVED, null, List.of()));

        mockMvc.perform(get("/transactions/{id}", Ids.TX_ID).with(maria()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionAvailable").value(false))
                .andExpect(jsonPath("$.conversionNote").exists())
                .andExpect(jsonPath("$.conversions.length()").value(0));
    }

    @Test
    void shouldReturn404WhenTransactionDoesNotExist() throws Exception {
        when(getTransactionUseCase.findById(Ids.UNKNOWN, Ids.USER))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada."));

        mockMvc.perform(get("/transactions/{id}", Ids.UNKNOWN).with(maria()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transação não encontrada."));
    }

    @Test
    void shouldReturn403WhenTransactionBelongsToAnotherUser() throws Exception {
        when(getTransactionUseCase.findById(Ids.TX_ID, Ids.USER))
                .thenThrow(new AccessForbiddenException("Acesso negado."));

        mockMvc.perform(get("/transactions/{id}", Ids.TX_ID).with(maria()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado."));
    }

    @Test
    void shouldReturn400WhenIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/transactions/abc").with(maria()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    // ----- listagem -----

    @Test
    void shouldListOwnTransactionsWithFilters() throws Exception {
        when(listTransactionsUseCase.list(eq(Ids.USER), eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 30)), eq(TransactionType.PURCHASE)))
                .thenReturn(List.of(transaction(TransactionStatus.APPROVED, null, List.of())));

        mockMvc.perform(get("/transactions").with(maria())
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-30")
                        .param("type", "PURCHASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(Ids.TX_ID.toString()));
    }

    @Test
    void shouldAcceptUserIdParameterWhenItIsTheAuthenticatedUser() throws Exception {
        when(listTransactionsUseCase.list(eq(Ids.USER), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/transactions").with(maria()).param("userId", Ids.USER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenListingTransactionsOfAnotherUser() throws Exception {
        mockMvc.perform(get("/transactions").with(maria()).param("userId", Ids.OTHER_USER.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado."));

        verifyNoInteractions(listTransactionsUseCase);
    }

    // ----- saldo -----

    @Test
    void shouldReturnOwnBalanceFromAccountService() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER)).thenReturn(
                new AccountLimits(new BigDecimal("1500.00"), new BigDecimal("500.00"), new BigDecimal("1000.00")));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER).with(maria()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.perTransactionLimit").value(500.00))
                .andExpect(jsonPath("$.dailyLimit").value(1000.00));
    }

    @Test
    void shouldReturn403WhenAskingBalanceOfAnotherUser() throws Exception {
        mockMvc.perform(get("/accounts/{userId}/balance", Ids.OTHER_USER).with(maria()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getAccountUseCase);
    }

    @Test
    void shouldReturn503WhenAccountServiceIsDown() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER))
                .thenThrow(new AccountServiceUnavailableException("fora do ar"));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER).with(maria()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Serviço de saldo indisponível no momento. Tente novamente mais tarde."));
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws Exception {
        when(getAccountUseCase.getAccount(Ids.USER))
                .thenThrow(new AccountNotFoundException("Conta do usuário não encontrada no serviço de saldo."));

        mockMvc.perform(get("/accounts/{userId}/balance", Ids.USER).with(maria()))
                .andExpect(status().isNotFound());
    }
}
