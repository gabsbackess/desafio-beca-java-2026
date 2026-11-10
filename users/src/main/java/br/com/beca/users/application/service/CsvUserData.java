package br.com.beca.users.application.service;

import br.com.beca.users.validation.ValidCpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CsvUserData(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "CPF é obrigatório.")
        @ValidCpf
        String cpf,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres.")
        String password) {
}
