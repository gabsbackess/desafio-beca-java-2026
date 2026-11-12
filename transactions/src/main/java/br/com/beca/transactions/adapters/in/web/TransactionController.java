package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.dto.BalanceResponse;
import br.com.beca.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.transactions.adapters.in.web.dto.TransactionResponse;
import br.com.beca.transactions.adapters.in.web.dto.TransactionWebMapper;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.in.GetAccountUseCase;
import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.in.ListTransactionsUseCase;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            description = "A resposta traz o id e o status (APPROVED ou REJECTED). Se rejeitada, traz o motivo. "
                    + "Retirada, compra e transferência debitam a conta de quem envia; o depósito credita. "
                    + "Limitação: a transferência não credita a conta do destinatário.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Compra", value = """
                    {"type": "PURCHASE", "amount": 89.90, "category": "FOOD", "description": "Mercado do mês"}
                    """),
            @ExampleObject(name = "Depósito", value = """
                    {"type": "DEPOSIT", "amount": 3000.00, "description": "Salário"}
                    """),
            @ExampleObject(name = "Retirada", value = """
                    {"type": "WITHDRAWAL", "amount": 200.00, "description": "Saque"}
                    """),
            @ExampleObject(name = "Transferência", value = """
                    {"type": "TRANSFER", "amount": 100.00, "destinationUserId": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "description": "Pix"}
                    """)}))
    @ApiResponse(responseCode = "201", description = "Transação gravada (aprovada ou rejeitada)",
            content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "Aprovada", value = """
                            {"id": "0b8f9c4e-2a1d-4c55-9a39-6a2f0d7a1e10", "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "destinationUserId": null, "type": "PURCHASE", "amount": 89.90, "category": "FOOD", "description": "Mercado do mês", "status": "APPROVED", "rejectionReason": null, "createdAt": "2026-11-10T14:30:00", "conversionAvailable": true, "conversionNote": null, "conversions": [{"currency": "USD", "rate": 5.3300, "rateDate": "2026-11-10", "convertedAmount": 16.87}, {"currency": "EUR", "rate": 6.1500, "rateDate": "2026-11-10", "convertedAmount": 14.62}]}
                            """),
                    @ExampleObject(name = "Rejeitada", value = """
                            {"id": "5d1c7e22-8b34-4f0a-b0de-3c9a1f2e4b77", "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "destinationUserId": null, "type": "WITHDRAWAL", "amount": 5000.00, "category": "OTHER", "description": "Saque", "status": "REJECTED", "rejectionReason": "Saldo insuficiente.", "createdAt": "2026-11-10T14:31:00", "conversionAvailable": true, "conversionNote": null, "conversions": [{"currency": "USD", "rate": 5.3300, "rateDate": "2026-11-10", "convertedAmount": 938.09}, {"currency": "EUR", "rate": 6.1500, "rateDate": "2026-11-10", "convertedAmount": 813.01}]}
                            """)}))
    @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 400, "errors": {"amount": "O valor deve ser maior que zero."}}
                    """)))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 401, "message": "Credenciais inválidas ou ausentes."}
                    """)))
    @ApiResponse(responseCode = "503", description = "MS1 (usuários) indisponível: não foi possível validar o login",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 503, "message": "Serviço de autenticação indisponível no momento. Tente novamente mais tarde."}
                    """)))
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request,
                                      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return mapper.toResponse(createTransactionUseCase.create(mapper.toCommand(request, user.id())));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Consulta uma transação pelo id")
    @ApiResponse(responseCode = "403", description = "A transação é de outro usuário",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 403, "message": "Acesso negado."}
                    """)))
    @ApiResponse(responseCode = "404", description = "Transação não encontrada",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 404, "message": "Transação não encontrada."}
                    """)))
    public TransactionResponse findById(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return mapper.toResponse(getTransactionUseCase.findById(id, user.id()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lista as transações do usuário autenticado",
            description = "Filtros opcionais por período (from e to, formato AAAA-MM-DD, inclusivos) e por tipo. "
                    + "Se userId for informado e não for o do usuário autenticado, a resposta é 403.")
    @ApiResponse(responseCode = "403", description = "userId de outro usuário")
    public List<TransactionResponse> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        requireSameUser(userId, user);
        return listTransactionsUseCase.list(user.id(), from, to, type).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/accounts/{userId}/balance")
    @Operation(summary = "Saldo e limites atuais do usuário, lidos da API Mock")
    @ApiResponse(responseCode = "200", description = "Saldo e limites",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "balance": 2000.00, "perTransactionLimit": 500.00, "dailyLimit": 1000.00}
                    """)))
    @ApiResponse(responseCode = "403", description = "userId de outro usuário")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada na API Mock")
    @ApiResponse(responseCode = "503", description = "API Mock indisponível",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"status": 503, "message": "Serviço de saldo indisponível no momento. Tente novamente mais tarde."}
                    """)))
    public BalanceResponse balance(@PathVariable UUID userId, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        requireSameUser(userId, user);
        return mapper.toBalanceResponse(userId, getAccountUseCase.getAccount(userId));
    }

    /** Ninguém consulta nem cria dados em nome de outro usuário. */
    private static void requireSameUser(UUID requestedUserId, AuthenticatedUser user) {
        if (requestedUserId != null && !requestedUserId.equals(user.id())) {
            throw new AccessForbiddenException("Acesso negado.");
        }
    }
}
