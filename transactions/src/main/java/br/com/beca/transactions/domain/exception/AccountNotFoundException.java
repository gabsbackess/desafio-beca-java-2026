package br.com.beca.transactions.domain.exception;

/**
 * O serviço de saldo respondeu, mas não existe conta para o usuário.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
