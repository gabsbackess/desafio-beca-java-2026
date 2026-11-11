package br.com.beca.transactions.adapters.out.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Resposta do POST /users/authenticate do MS1 (nunca traz senha). Campos extras são ignorados. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsersAuthResponse(UUID id, String name, String email) {
}
