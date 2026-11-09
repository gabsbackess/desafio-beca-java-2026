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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ImportUsersCsvServiceTest {
    @Mock
    private CreateUserUseCase createUserUseCase;
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private ImportUsersCsvService importUserCsvService;
    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        importUserCsvService = new ImportUsersCsvService(
                createUserUseCase,
                validator
        );
    }
    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }
    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        byte[] csv = new byte[0];
        assertThrows(
                InvalidCsvException.class,
                () -> importUserCsvService.importUsers(csv)
        );
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldThrowExceptionWhenCsvHeaderIsInvalid() {
        String csv = """
               nome,email,cpf,senha
               Maria,maria@email.com,12345678901,123456
               """;
        assertThrows(
                InvalidCsvException.class,
                () -> importUserCsvService.importUsers(
                        csv.getBytes(StandardCharsets.UTF_8)
                )
        );
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldImportUserWhenCsvIsValid() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901,123456
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(1, response.registeredUsers());
        assertEquals(0, response.usersError());
        verify(createUserUseCase, times(1)).create(any(User.class));
    }
    @Test
    void shouldRejectUserWhenEmailIsInvalid() {
        String csv = """
               name,email,cpf,password
               Maria,email-invalido,12345678901,123456
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldReturnErrorWhenUserAlreadyExists() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901,123456
               """;
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("Usuário já existe."));
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verify(createUserUseCase, times(1)).create(any(User.class));
    }
    @Test
    void shouldReturnErrorWhenCsvIsMalformed() {
        String csv = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901,123456
           "Joao,joao@email.com,12345678902,123456
           """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(1, response.error().size());
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldRejectUserWhenCpfIsInvalid() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345,123456
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldReturnErrorWhenCsvLineHasMissingColumns() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldReturnErrorWhenCsvLineHasExtraColumns() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901,123456,extra
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldImportValidUsersAndRejectInvalidUsers() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901,123456
               Joao,email-invalido,12345678902,123456
               Ana,ana@email.com,12345678903,123456
               """;
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(2, response.registeredUsers());
        assertEquals(1, response.usersError());
        verify(createUserUseCase, times(2)).create(any(User.class));
    }
    @Test
    void shouldContinueImportWhenUnexpectedErrorOccurs() {
        String csv = """
               name,email,cpf,password
               Maria,maria@email.com,12345678901,123456
               Joao,joao@email.com,12345678902,123456
               Ana,ana@email.com,12345678903,123456
               """;
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new RuntimeException("Erro inesperado"))
                .thenReturn(null);
        var response = importUserCsvService.importUsers(
                csv.getBytes(StandardCharsets.UTF_8)
        );
        assertEquals(2, response.registeredUsers());
        assertEquals(1, response.usersError());
        verify(createUserUseCase, times(3)).create(any(User.class));
    }
    @Test
    void shouldRejectCsvWhenItExceedsMaximumRecords() {
        StringBuilder csv = new StringBuilder(
                "name,email,cpf,password\n"
        );
        for (int i = 1; i <= 1001; i++) {
            csv.append("Maria")
                    .append(i)
                    .append(",maria")
                    .append(i)
                    .append("@email.com,12345678901,123456\n");
        }
        assertThrows(
                InvalidCsvException.class,
                () -> importUserCsvService.importUsers(
                        csv.toString().getBytes(StandardCharsets.UTF_8)
                )
        );
        verify(createUserUseCase, never()).create(any(User.class));
    }
}