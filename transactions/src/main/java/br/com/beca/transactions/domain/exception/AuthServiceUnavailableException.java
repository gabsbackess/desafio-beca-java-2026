package br.com.beca.transactions.domain.exception;

/** O MS1 (serviço de usuários) não respondeu na autenticação. */
public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthServiceUnavailableException(String message) {
        super(message);
    }
}
