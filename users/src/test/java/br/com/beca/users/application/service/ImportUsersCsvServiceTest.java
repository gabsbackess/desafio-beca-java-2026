package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.domain.exception.InvalidCsvException;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportUsersCsvServiceTest {

    private static final String CPF_1 = "52998224725";
    private static final String CPF_2 = "11144477735";
    private static final String CPF_3 = "39053344705";
    private static final String CPF_INVALID_CHECK_DIGITS = "12345678901";

    @Mock
    private CreateUserUseCase createUserUseCase;

    private ValidatorFactory validatorFactory;
    private Validator validator;
    private ImportUsersCsvService importUserCsvService;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        importUserCsvService = new ImportUsersCsvService(createUserUseCase, validator);
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    private byte[] bytes(String csv) {
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(new byte[0]));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldThrowExceptionWhenCsvHeaderIsInvalid() {
        String csv = """
                nome,email,cpf,senha
                Maria,maria@email.com,%s,123456
                """.formatted(CPF_1);

        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(bytes(csv)));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldThrowInvalidCsvExceptionWhenHeaderHasEmptyColumn() {
        String csv = """
                name,,cpf,password
                Maria,maria@email.com,%s,123456
                """.formatted(CPF_1);

        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(bytes(csv)));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldThrowInvalidCsvExceptionWhenHeaderHasTrailingComma() {
        String csv = """
                name,email,cpf,password,
                Maria,maria@email.com,%s,123456,
                """.formatted(CPF_1);

        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(bytes(csv)));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldImportUserWhenCsvIsValid() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                """.formatted(CPF_1);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(1, response.registeredUsers());
        assertEquals(0, response.usersError());
        verify(createUserUseCase, times(1)).create(any(User.class));
    }

    @Test
    void shouldRejectUserWhenEmailIsInvalid() {
        String csv = """
                name,email,cpf,password
                Maria,email-invalido,%s,123456
                """.formatted(CPF_1);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldReturnFixedErrorWhenUserAlreadyExists() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                """.formatted(CPF_1);
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("Detalhe interno do conflito."));

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals("Linha 2: usuário já cadastrado.", response.error().get(0));
        assertFalse(response.error().get(0).contains("Detalhe interno"));
        verify(createUserUseCase, times(1)).create(any(User.class));
    }

    @Test
    void shouldThrowInvalidCsvExceptionWhenCsvHasUnclosedQuote() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                "Joao,joao@email.com,%s,123456
                """.formatted(CPF_1, CPF_2);

        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(bytes(csv)));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldRejectUserWhenCpfHasLessThanElevenDigits() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,12345,123456
                """;

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldRejectUserWhenCpfHasInvalidCheckDigits() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                """.formatted(CPF_INVALID_CHECK_DIGITS);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertTrue(response.error().get(0).contains("CPF inválido."));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldReturnErrorWhenCsvLineHasMissingColumns() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s
                """.formatted(CPF_1);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldReturnErrorWhenCsvLineHasExtraColumns() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456,extra
                """.formatted(CPF_1);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldImportValidUsersAndRejectInvalidUsers() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                Joao,email-invalido,%s,123456
                Ana,ana@email.com,%s,123456
                """.formatted(CPF_1, CPF_2, CPF_3);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(2, response.registeredUsers());
        assertEquals(1, response.usersError());
        verify(createUserUseCase, times(2)).create(any(User.class));
    }

    @Test
    void shouldContinueImportWithFixedMessageWhenUnexpectedErrorOccurs() {
        String csv = """
                name,email,cpf,password
                Maria,maria@email.com,%s,123456
                Joao,joao@email.com,%s,123456
                Ana,ana@email.com,%s,123456
                """.formatted(CPF_1, CPF_2, CPF_3);
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new RuntimeException("Erro inesperado"))
                .thenReturn(null);

        var response = importUserCsvService.importUsers(bytes(csv));

        assertEquals(2, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals("Linha 2: erro ao cadastrar usuário.", response.error().get(0));
        assertFalse(response.error().get(0).contains("Erro inesperado"));
        verify(createUserUseCase, times(3)).create(any(User.class));
    }

    @Test
    void shouldRejectCsvWhenItExceedsMaximumRecords() {
        StringBuilder csv = new StringBuilder("name,email,cpf,password\n");
        for (int i = 1; i <= 1001; i++) {
            csv.append("Maria").append(i)
                    .append(",maria").append(i)
                    .append("@email.com,").append(CPF_1).append(",123456\n");
        }

        assertThrows(InvalidCsvException.class, () -> importUserCsvService.importUsers(bytes(csv.toString())));
        verify(createUserUseCase, never()).create(any(User.class));
    }
}
