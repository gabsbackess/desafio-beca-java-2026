package br.com.beca.transactions.domain.exception;

/**
 * O serviço externo de saldo e limites (API Mock) não respondeu.
 */
public class AccountServiceUnavailableException extends RuntimeException {
    public AccountServiceUnavailableException(String message) {
        super(message);
    }

    public AccountServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
