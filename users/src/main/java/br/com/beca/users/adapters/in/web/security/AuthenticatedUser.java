package br.com.beca.users.adapters.in.web.security;

import java.util.UUID;

/** Usuário que passou na autenticação básica. Nunca carrega a senha. */
public record AuthenticatedUser(UUID id, String email) {
}
