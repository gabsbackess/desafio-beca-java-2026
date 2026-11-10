package br.com.beca.users.application.service;

import br.com.beca.users.adapters.in.web.validation.ValidCpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CsvUserData(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "CPF é obrigatório.")
        @ValidCpf
        String cpf,

        @NotBlank(message = "Senha é obrigatória.")
        String password) {
}
