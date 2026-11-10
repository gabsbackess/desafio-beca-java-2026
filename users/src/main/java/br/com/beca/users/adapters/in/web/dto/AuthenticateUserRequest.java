package br.com.beca.users.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthenticateUserRequest {
    @Schema(example = "maria.silva@example.com")
    @NotBlank(message = "E-mail é obrigatório.")
    private String email;
    @Schema(example = "Senha@123")
    @NotBlank(message = "Senha é obrigatória.")
    private String password;

    public AuthenticateUserRequest() {
    }

    public AuthenticateUserRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
