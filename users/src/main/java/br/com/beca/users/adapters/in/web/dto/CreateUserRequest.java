package br.com.beca.users.adapters.in.web.dto;

import br.com.beca.users.validation.ValidCpf;
import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @Schema(example = "Maria Silva")
    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
    private String name;
    @Schema(example = "maria.silva@example.com")
    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "Necessário informar endereço de e-mail válido.")
    @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
    private String email;
    @Schema(description = "Aceita com ou sem pontuação.", example = "529.982.247-25")
    @NotBlank(message = "CPF é obrigatório.")
    @ValidCpf
    private String cpf;
    @Schema(example = "Senha@123")
    @NotBlank(message = "Senha é obrigatória.")
    @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres.")
    private String password;

    public CreateUserRequest(){
    }
    public CreateUserRequest (String name, String email, String cpf, String password){
        this.name=name;
        this.email=email;
        this.cpf=cpf;
        this.password=password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getPassword() {
        return password;
    }
}
