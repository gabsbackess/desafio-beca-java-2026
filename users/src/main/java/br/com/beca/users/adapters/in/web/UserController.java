package br.com.beca.users.adapters.in.web;

import br.com.beca.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.users.adapters.in.web.dto.CreateUserResponse;
import br.com.beca.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.application.port.in.*;
import br.com.beca.users.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private static final String USER_EXAMPLE = """
            {"id": 1, "name": "Maria Silva", "email": "maria.silva@example.com", "cpf": "52998224725", "createdAt": "2026-11-10T14:30:00"}
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

    @Operation(summary = "Listar usuários")
    @ApiResponse(responseCode = "200", description = "Lista de usuários (sem a senha)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "[" + USER_EXAMPLE + "]")))
    @GetMapping
    public List<CreateUserResponse> list() {
        List<User> users = listUsersUseCase.list();

        return users.stream()
                .map(userWebMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Buscar usuário pelo id")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = USER_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Id inválido (não numérico)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Valor inválido para o parâmetro 'id'.\"}")))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/{id}")
    public CreateUserResponse findById(@PathVariable Long id) {
        User user = findUserByIdUseCase.findById(id);
        return userWebMapper.toResponse(user);
    }

    @Operation(summary = "Atualizar usuário",
            description = "Atualiza nome, e-mail e CPF. A senha é opcional: só é trocada quando vier preenchida.")
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
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userWebMapper.toDomain(request);
        User updatedUser = updateUserUseCase.update(id, user);
        return userWebMapper.toResponse(updatedUser);
    }

    @Operation(summary = "Excluir usuário")
    @ApiResponse(responseCode = "204", description = "Usuário excluído")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUserUseCase.delete(id);

    }

}
