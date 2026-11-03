package br.com.beca.users.adapters.in.web.dto;

import java.time.LocalDateTime;

public class CreateUserResponse {
    private Long id;
    private String name;
    private String email;
    private String cpf;
    private LocalDateTime createdAt;

    public CreateUserResponse (){}
    public CreateUserResponse (Long id, String name, String email, String cpf, LocalDateTime createdAt ){
        this.id=id;
        this.name=name;
        this.email=email;
        this.cpf=cpf;
        this.createdAt=createdAt;
    }

    public Long getId() {
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
