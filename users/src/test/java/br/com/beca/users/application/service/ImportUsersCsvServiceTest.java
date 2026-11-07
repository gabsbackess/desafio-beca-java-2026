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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                new byte[0]
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "O arquivo CSV está vazio.",
                response.error().get(0)
        );
    }
    @Test
    void shouldReturnErrorWhenFileHasInvalidExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.txt",
                "text/plain",
                "algum conteúdo".getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Formato de arquivo inválido. Envie um arquivo .csv.",
                response.error().get(0)
        );
    }
    @Test
    void shouldReturnErrorWhenCsvHeaderIsInvalid() {
        String csvContent = """
            nome,email,cpf,senha
            Maria,maria@email.com,12345678901,senha123
            """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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
    void shouldReturnErrorWhenFileCannotBeRead() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("users.csv");
        when(file.getInputStream())
                .thenThrow(new IOException("Falha na leitura"));
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
        assertEquals(0, response.totalLines());
        assertEquals(0, response.registeredUsers());
        assertEquals(1, response.usersError());
        assertEquals(
                "Não foi possível ler o arquivo CSV.",
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );
        ImportUsersCsvResponse response =
                importUserCsvService.importUsers(file);
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


