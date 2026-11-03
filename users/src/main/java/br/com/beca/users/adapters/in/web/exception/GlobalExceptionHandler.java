package br.com.beca.users.adapters.in.web.exception;

import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationException(
            MethodArgumentNotValidException exception) {
        return exception.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
    }
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUserAlreadyExistsException (UserAlreadyExistsException exception){
        return exception.getMessage();
    }
}
