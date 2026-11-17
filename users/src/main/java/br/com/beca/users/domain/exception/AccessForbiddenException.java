package br.com.beca.users.domain.exception;

/** O usuário autenticado tentou mexer em dados de outro usuário. */
public class AccessForbiddenException extends RuntimeException {

    public AccessForbiddenException(String message) {
        super(message);
    }
}
