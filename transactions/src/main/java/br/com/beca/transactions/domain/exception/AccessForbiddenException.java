package br.com.beca.transactions.domain.exception;

/** O usuário autenticado tentou acessar dados de outro usuário. */
public class AccessForbiddenException extends RuntimeException {
    public AccessForbiddenException(String message) {
        super(message);
    }
}
