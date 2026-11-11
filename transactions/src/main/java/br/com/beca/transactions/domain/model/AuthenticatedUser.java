package br.com.beca.transactions.domain.model;

import java.util.UUID;

/** Usuário que passou na autenticação (dados vindos do MS1, nunca a senha). */
public record AuthenticatedUser(UUID id, String name, String email) {
}
