package br.com.beca.transactions.adapters.in.web.exception;

import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mesmo formato de erro do MS1: {status, message} ou {status, errors:{campo: mensagem}}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou mal formatado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + exception.getName() + "'.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + exception.getParameterName() + "'.");
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransaction(InvalidTransactionException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotFound(TransactionNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccessForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleAccessForbidden(AccessForbiddenException exception) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAccountServiceUnavailable(
            AccountServiceUnavailableException exception) {
        log.warn("API Mock indisponível: {}", exception.getMessage(), exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço de saldo indisponível no momento. Tente novamente mais tarde.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para esta rota.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado.");
    }

    /** Rede de segurança: erro não previsto vira 500 com mensagem fixa; o detalhe fica só no log. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(RuntimeException exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return build(HttpStatus.valueOf(errorResponse.getStatusCode().value()),
                    "Não foi possível processar a requisição.");
        }
        log.error("Erro inesperado ao processar a requisição.", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.value());
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
