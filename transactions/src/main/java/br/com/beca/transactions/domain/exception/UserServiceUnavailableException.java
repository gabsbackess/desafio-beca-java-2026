package br.com.beca.transactions.domain.exception;

/**
 * O serviço de usuários (MS1) não respondeu ao conferir se um usuário existe.
 */
public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(String message) {
        super(message);
    }

    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
