package br.com.beca.users.adapters.in.web.dto;

import java.util.UUID;
import java.time.LocalDateTime;

public class CreateUserResponse {
    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private LocalDateTime createdAt;

    public CreateUserResponse (){}
    public CreateUserResponse (UUID id, String name, String email, String cpf, LocalDateTime createdAt ){
        this.id=id;
        this.name=name;
        this.email=email;
        this.cpf=cpf;
        this.createdAt=createdAt;
    }

    public UUID getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
