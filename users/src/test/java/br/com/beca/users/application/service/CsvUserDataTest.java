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
        CsvUserData data = new CsvUserData(
                "Maria",
                "maria@email.com",
                "12345678901",
                "senha123"
        );
        assertTrue(validator.validate(data).isEmpty());
    }
    @Test
    void shouldRejectWhenNameIsBlank() {
        CsvUserData data = new CsvUserData(
                "",
                "maria@email.com",
                "12345678901",
                "senha123"
        );
        assertHasViolation(data);
    }
    @Test
    void shouldRejectWhenEmailIsInvalid() {
        CsvUserData data = new CsvUserData(
                "Maria",
                "email-invalido",
                "12345678901",
                "senha123"
        );
        assertHasViolation(data);
    }
    @Test
    void shouldRejectWhenCpfHasLessThanElevenDigits() {
        CsvUserData data = new CsvUserData(
                "Maria",
                "maria@email.com",
                "123456789",
                "senha123"
        );
        assertHasViolation(data);
    }
    @Test
    void shouldRejectWhenCpfContainsLetters() {
        CsvUserData data = new CsvUserData(
                "Maria",
                "maria@email.com",
                "1234567890A",
                "senha123"
        );
        assertHasViolation(data);
    }
    @Test
    void shouldRejectWhenEmailIsBlank() {
        CsvUserData data = new CsvUserData(
                "Maria",
                "",
                "12345678901",
                "senha123"
        );
        assertHasViolation(data);
    }
    @Test
    void shouldRejectWhenPasswordIsBlank() {
        CsvUserData data = new CsvUserData(
                "Maria",
                "maria@email.com",
                "12345678901",
                ""
        );
        assertHasViolation(data);
    }
    private void assertHasViolation(CsvUserData data) {
        Set<ConstraintViolation<CsvUserData>> violations =
                validator.validate(data);
        assertFalse(violations.isEmpty());
    }
}

