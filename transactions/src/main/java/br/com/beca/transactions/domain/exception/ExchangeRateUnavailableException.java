package br.com.beca.transactions.domain.exception;

/** A BrasilAPI não respondeu ou não tem cotação. */
public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(String message) {
        super(message);
    }

    public ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
