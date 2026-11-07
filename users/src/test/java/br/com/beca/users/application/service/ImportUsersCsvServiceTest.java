package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImportUsersCsvServiceTest {

    @Mock
    private CreateUserUseCase createUserUseCase;

    @Mock
    private Validator validator;

    @InjectMocks
    private ImportUsersCsvService importUserCsvService;

    @Test
    void shouldReturnErrorWhenFileIsEmpty() {
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(new byte[0]);
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "O arquivo CSV está vazio.",
                response.error().get(0)
        );
    }
    @Test
    void shouldReturnErrorWhenCsvHeaderIsInvalid() {
        String csvContent = """
            nome,email,cpf,senha
            Maria,maria@email.com,12345678901,senha123
            """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Cabeçalho inválido. Esperado: name, email, cpf, password.",
                response.error().get(0)
        );
    }
    @Test
    void shouldImportUserWhenCsvIsValid() {
        when(validator.validate(any(CsvUserData.class)))
                .thenReturn(Set.of());
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(1, response.registeredUsers());
        assertEquals(0, response.usersError());
        verify(createUserUseCase).create(any(User.class));
    }
    @Test
    void shouldRejectUserWhenEmailIsInvalid() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<CsvUserData> violation =
                mock(ConstraintViolation.class);
        when(violation.getMessage())
                .thenReturn("E-mail inválido.");
        when(validator.validate(any(CsvUserData.class)))
                .thenReturn(Set.of(violation));
        String csvContent = """
           name,email,cpf,password
           Maria,email-invalido,12345678901,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 2: E-mail inválido.",
                response.error().get(0)
        );
        verify(createUserUseCase, never())
                .create(any(User.class));
    }
    @Test
    void shouldReturnErrorWhenUserAlreadyExists() {
        when(validator.validate(any(CsvUserData.class)))
                .thenReturn(Set.of());
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new UserAlreadyExistsException(
                        "CPF ou e-mail já cadastrado."
                ));
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 2: CPF ou e-mail já cadastrado.",
                response.error().get(0)
        );
        verify(createUserUseCase).create(any(User.class));
    }
    @Test
    void shouldReturnErrorWhenCsvIsMalformed() {
        String csvContent = """
           name,email,cpf,password
           "Maria,maria@email.com,12345678901,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(0, response.totalLines());
        assertEquals(
                "Arquivo CSV malformado ou inválido.",
                response.error().get(0)
        );
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldRejectUserWhenCpfIsInvalid() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<CsvUserData> violation =
                mock(ConstraintViolation.class);
        when(violation.getMessage())
                .thenReturn(
                        "O CPF deve conter exatamente 11 dígitos numéricos."
                );
        when(validator.validate(any(CsvUserData.class)))
                .thenReturn(Set.of(violation));
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 2: O CPF deve conter exatamente 11 dígitos numéricos.",
                response.error().get(0)
        );
        verify(createUserUseCase, never())
                .create(any(User.class));
    }
    @Test
    void shouldReturnErrorWhenCsvLineHasMissingColumns() {
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 2: quantidade de colunas inválida. Esperadas: 4.",
                response.error().get(0)
        );
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldReturnErrorWhenCsvLineHasExtraColumns() {
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901,senha123,extra
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(1, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 2: quantidade de colunas inválida. Esperadas: 4.",
                response.error().get(0)
        );
        verifyNoInteractions(createUserUseCase);
    }
    @Test
    void shouldImportValidUsersAndRejectInvalidUsers() {
        when(validator.validate(any(CsvUserData.class)))
                .thenAnswer(invocation -> {
                    CsvUserData data = invocation.getArgument(0);
                    if (data.email().equals("email-invalido")) {
                        @SuppressWarnings("unchecked")
                        ConstraintViolation<CsvUserData> violation =
                                mock(ConstraintViolation.class);
                        when(violation.getMessage())
                                .thenReturn("E-mail inválido.");
                        return Set.of(violation);
                    }
                    return Set.of();
                });
        String csvContent = """
           name,email,cpf,password
           Maria,maria@email.com,12345678901,senha123
           João,email-invalido,12345678902,senha123
           Ana,ana@email.com,12345678903,senha123
           """;
        byte[] csvContentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(csvContentBytes);
        assertEquals(3, response.totalLines());
        assertEquals(2, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Linha 3: E-mail inválido.",
                response.error().get(0)
        );
        verify(createUserUseCase, org.mockito.Mockito.times(2))
                .create(any(User.class));
    }
}


