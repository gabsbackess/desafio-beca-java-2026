package br.com.beca.users.application.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUserDataTest {

    private static final String VALID_CPF = "52998224725";

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldNotHaveViolationsWhenDataIsValid() {
        CsvUserData data = new CsvUserData("Maria", "maria@email.com", VALID_CPF, "senha123");

        assertTrue(validator.validate(data).isEmpty());
    }

    @Test
    void shouldRejectWhenNameIsBlank() {
        assertHasViolation(new CsvUserData("", "maria@email.com", VALID_CPF, "senha123"));
    }

    @Test
    void shouldRejectWhenEmailIsInvalid() {
        assertHasViolation(new CsvUserData("Maria", "email-invalido", VALID_CPF, "senha123"));
    }

    @Test
    void shouldRejectWhenCpfHasLessThanElevenDigits() {
        assertHasViolation(new CsvUserData("Maria", "maria@email.com", "123456789", "senha123"));
    }

    @Test
    void shouldRejectWhenCpfContainsLetters() {
        assertHasViolation(new CsvUserData("Maria", "maria@email.com", "1234567890A", "senha123"));
    }

    @Test
    void shouldRejectWhenCpfHasElevenDigitsButInvalidCheckDigits() {
        assertHasViolation(new CsvUserData("Maria", "maria@email.com", "12345678901", "senha123"));
    }

    @Test
    void shouldRejectWhenCpfHasAllDigitsEqual() {
        assertHasViolation(new CsvUserData("Maria", "maria@email.com", "11111111111", "senha123"));
    }

    @Test
    void shouldRejectWhenEmailIsBlank() {
        assertHasViolation(new CsvUserData("Maria", "", VALID_CPF, "senha123"));
    }

    @Test
    void shouldRejectWhenPasswordIsBlank() {
        assertHasViolation(new CsvUserData("Maria", "maria@email.com", VALID_CPF, ""));
    }

    private void assertHasViolation(CsvUserData data) {
        Set<ConstraintViolation<CsvUserData>> violations = validator.validate(data);
        assertFalse(violations.isEmpty());
    }
}
