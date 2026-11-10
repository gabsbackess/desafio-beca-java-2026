package br.com.beca.users.adapters.in.web;

import br.com.beca.users.adapters.in.web.dto.AuthenticateUserRequest;
import br.com.beca.users.adapters.in.web.dto.CreateUserResponse;
import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.application.port.in.AuthenticateUserUseCase;
import br.com.beca.users.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Autenticação", description = "Conferência de credenciais (usada pelo MS2)")
public class UserAuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final UserWebMapper userWebMapper;

    public UserAuthController(AuthenticateUserUseCase authenticateUserUseCase,
                              UserWebMapper userWebMapper) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.userWebMapper = userWebMapper;
    }

    @Operation(summary = "Autenticar usuário",
            description = "Confere e-mail e senha. Em caso de sucesso devolve os dados do usuário (sem a senha), "
                    + "incluindo o id, que o MS2 usa para identificar o dono das transações. "
                    + "E-mail inexistente e senha errada devolvem a mesma resposta.")
    @ApiResponse(responseCode = "200", description = "Credenciais válidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"id": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "name": "Maria Silva", "email": "maria.silva@example.com", "cpf": "52998224725", "createdAt": "2026-11-10T14:30:00"}
                            """)))
    @ApiResponse(responseCode = "400", description = "E-mail ou senha ausentes",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"status": 400, "errors": {"password": "Senha é obrigatória."}}
                            """)))
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"status": 401, "message": "Credenciais inválidas."}
                            """)))
    @PostMapping("/authenticate")
    public CreateUserResponse authenticate(@Valid @RequestBody AuthenticateUserRequest request) {
        User user = authenticateUserUseCase.authenticate(request.getEmail(), request.getPassword());
        return userWebMapper.toResponse(user);
    }
}
