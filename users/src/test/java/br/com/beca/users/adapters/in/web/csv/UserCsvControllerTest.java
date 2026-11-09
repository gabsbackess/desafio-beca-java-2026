package br.com.beca.users.adapters.in.web.csv;
import br.com.beca.users.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.application.port.in.ImportUsersCsvUseCase;
import br.com.beca.users.application.service.CsvUserData;
import br.com.beca.users.domain.exception.InvalidCsvException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCsvController.class)
@Import(GlobalExceptionHandler.class)
class UserCsvControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ImportUsersCsvUseCase importUsersCsvUseCase;

    @Test
    void shouldReturnBadRequestWhenCsvIsEmpty() throws Exception {
        when(importUsersCsvUseCase.importUsers(any(byte[].class)))
                .thenThrow(new InvalidCsvException("O arquivo CSV está vazio."));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );
        mockMvc.perform(multipart("/users/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCsvHeaderIsInvalid() throws Exception {
        when(importUsersCsvUseCase.importUsers(any(byte[].class)))
                .thenThrow(new InvalidCsvException("Cabeçalho inválido."));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "nome,email,cpf,senha".getBytes()
        );
        mockMvc.perform(multipart("/users/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOkWhenCsvIsValid() throws Exception {
        when(importUsersCsvUseCase.importUsers(any(byte[].class)))
                .thenReturn(new ImportUsersCsvResponse(
                        1, 1, 0, List.of()
                ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "name,email,cpf,password\nMaria,maria@email.com,12345678901,senha123"
                        .getBytes()
        );
        mockMvc.perform(multipart("/users/import").file(file))
                .andExpect(status().isOk());
    }
}