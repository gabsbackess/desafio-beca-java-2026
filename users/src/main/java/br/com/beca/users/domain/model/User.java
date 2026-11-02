package br.com.beca.users.domain.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String name;
    private String email;
    private String cpf;
    private String password;
    private LocalDateTime createdAt;

    public User() {
    }
    public User(String name, String email, String cpf, String password) {
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.password = password;
        this.createdAt = LocalDateTime.now();
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

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User(Long id, String name, String email, String cpf, String password, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.password = password;
        this.createdAt = createdAt;

    }

}

