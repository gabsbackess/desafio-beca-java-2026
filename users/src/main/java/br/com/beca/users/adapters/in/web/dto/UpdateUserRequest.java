package br.com.beca.users.adapters.in.web.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public class UpdateUserRequest {
    @NotBlank
    private String name;
    @NotBlank
    @Email(message = "Necessário informar endereço de e-mail válido.")
    private String email;
    @NotBlank
    private String cpf;
    @NotBlank
    private String password;
    public UpdateUserRequest() {
    }
    public UpdateUserRequest(String name, String email, String cpf, String password) {
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.password = password;
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