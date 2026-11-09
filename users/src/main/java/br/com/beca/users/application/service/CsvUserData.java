package br.com.beca.users.application.service;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CsvUserData(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "CPF é obrigatório.")
        @Pattern(
                regexp = "\\d{11}",
                message = "O CPF deve conter exatamente 11 dígitos numéricos."
        )
        String cpf,

        @NotBlank(message = "Senha é obrigatória.")
        String password) {
}
