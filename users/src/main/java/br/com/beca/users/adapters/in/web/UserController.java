package br.com.beca.users.adapters.in.web;

import java.util.UUID;
import br.com.beca.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.users.adapters.in.web.dto.CreateUserResponse;
import br.com.beca.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.adapters.in.web.security.AuthenticatedUser;
import br.com.beca.users.application.port.in.*;
import br.com.beca.users.domain.exception.AccessForbiddenException;
import br.com.beca.users.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "CRUD de usuários")
public class UserController {
    private static final String VALIDATION_ERROR_EXAMPLE = """
            {"status": 400, "errors": {"email": "Necessário informar endereço de e-mail válido.", "cpf": "CPF inválido."}}
            """;
    private static final String NOT_FOUND_EXAMPLE = """
            {"status": 404, "message": "Usuário não encontrado."}
            """;
    private static final String CONFLICT_EXAMPLE = """
            {"status": 409, "message": "CPF ou e-mail já cadastrado."}
            """;
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"status": 401, "message": "Credenciais inválidas ou ausentes."}
            """;
    private static final String FORBIDDEN_EXAMPLE = """
            {"status": 403, "message": "Você só pode alterar ou excluir o seu próprio usuário."}
            """;
    private static final String SUMMARY_EXAMPLE = """
            {"id": "11111111-2222-3333-4444-555555555555", "name": "João Santos"}
            """;
    private static final String USER_EXAMPLE = """
            {"id": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "name": "Maria Silva", "email": "maria.silva@example.com", "cpf": "52998224725", "createdAt": "2026-11-10T14:30:00"}
            """;

    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UserWebMapper userWebMapper;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;


    public UserController(CreateUserUseCase createUserUseCase,
                          ListUsersUseCase listUsersUseCase, UserWebMapper userWebMapper,
                          FindUserByIdUseCase findUserByIdUseCase, UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.userWebMapper = userWebMapper;
        this.findUserByIdUseCase = findUserByIdUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @Operation(summary = "Cadastrar usuário",
            description = "Cria um usuário. O CPF aceita pontos e traço e é salvo só com dígitos; "
                    + "o e-mail é salvo sem espaços e em minúsculas; a senha é criptografada com BCrypt.")
    @ApiResponse(responseCode = "201", description = "Usuário criado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = USER_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (lista os campos com erro)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = CONFLICT_EXAMPLE)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse create (@Valid @RequestBody CreateUserRequest request){
        User user = userWebMapper.toDomain(request);
        User savedUser = createUserUseCase.create(user);
        CreateUserResponse response = userWebMapper.toResponse(savedUser);
        return response;
    }

    @Operation(summary = "Listar usuários",
            description = "Exige autenticação básica (e-mail e senha). O seu próprio cadastro vem completo; os "
                    + "demais usuários vêm só com id e nome (sem e-mail, CPF nem data de cadastro).",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Lista de usuários (sem a senha): o seu cadastro completo e "
            + "os demais só com id e nome",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "[" + USER_EXAMPLE + "," + SUMMARY_EXAMPLE + "]")))
    @GetMapping
    public List<CreateUserResponse> list(@AuthenticationPrincipal AuthenticatedUser caller) {
        UUID viewerId = viewerId(caller);
        List<User> users = listUsersUseCase.list();

        return users.stream()
                .map(user -> userWebMapper.toResponseFor(user, viewerId))
                .toList();
    }

    @Operation(summary = "Buscar usuário pelo id",
            description = "Exige autenticação básica (e-mail e senha). O próprio usuário recebe os dados completos; "
                    + "qualquer outro (e o MS2, pela chave interna) recebe só id e nome.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Usuário encontrado (completo para o dono; id e nome para os "
            + "demais)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = USER_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Id inválido (não é um UUID)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Valor inválido para o parâmetro 'id'.\"}")))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/{id}")
    public CreateUserResponse findById(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser caller) {
        User user = findUserByIdUseCase.findById(id);
        return userWebMapper.toResponseFor(user, viewerId(caller));
    }

    @Operation(summary = "Atualizar usuário",
            description = "Atualiza nome, e-mail e CPF. A senha é opcional: só é trocada quando vier preenchida. "
                    + "Exige autenticação básica e só vale para o próprio usuário.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Só o próprio usuário pode fazer isto",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Usuário atualizado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = USER_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (lista os campos com erro)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = CONFLICT_EXAMPLE)))
    @PutMapping("/{id}")
    public CreateUserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        requireOwner(caller, id);
        User user = userWebMapper.toDomain(request);
        User updatedUser = updateUserUseCase.update(id, user);
        return userWebMapper.toResponse(updatedUser);
    }

    @Operation(summary = "Excluir usuário",
            description = "Exige autenticação básica e só vale para o próprio usuário.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Só o próprio usuário pode fazer isto",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "204", description = "Usuário excluído")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser caller) {
        requireOwner(caller, id);
        deleteUserUseCase.delete(id);
    }

    /** Id de quem consulta; nulo para o MS2 (chave interna), que só recebe a visão resumida. */
    private static UUID viewerId(AuthenticatedUser caller) {
        return caller == null ? null : caller.id();
    }

    /** Alterar e excluir valem só para o próprio usuário autenticado. */
    private static void requireOwner(AuthenticatedUser caller, UUID id) {
        if (caller == null || !caller.id().equals(id)) {
            throw new AccessForbiddenException("Você só pode alterar ou excluir o seu próprio usuário.");
        }
    }

}
