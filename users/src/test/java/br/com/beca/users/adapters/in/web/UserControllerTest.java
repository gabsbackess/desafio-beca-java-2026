package br.com.beca.users.adapters.in.web;

import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.in.DeleteUserUseCase;
import br.com.beca.users.application.port.in.FindUserByIdUseCase;
import br.com.beca.users.application.port.in.ListUsersUseCase;
import br.com.beca.users.application.port.in.UpdateUserUseCase;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.exception.UserNotFoundException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, UserWebMapper.class})
class UserControllerTest {

    private static final String VALID_CPF = "52998224725";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;
    @MockitoBean
    private ListUsersUseCase listUsersUseCase;
    @MockitoBean
    private FindUserByIdUseCase findUserByIdUseCase;
    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;
    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    private static String body(String name, String email, String cpf, String password) {
        return """
                {"name":"%s","email":"%s","cpf":"%s","password":"%s"}
                """.formatted(name, email, cpf, password);
    }

    private static User savedUser() {
        return new User(1L, "Maria", "maria@email.com", VALID_CPF, "hash-bcrypt",
                LocalDateTime.of(2026, 1, 10, 10, 0));
    }

    @Test
    void shouldReturn201AndNeverExposePasswordWhenUserIsCreated() throws Exception {
        when(createUserUseCase.create(any(User.class))).thenReturn(savedUser());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maria", "maria@email.com", VALID_CPF, "Senha@123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn400WithAllInvalidFieldsWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "email-invalido", "12345678901", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.cpf").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldReturn400WithStandardFormatWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou mal formatado."));
    }

    @Test
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("CPF ou e-mail já cadastrado."));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maria", "maria@email.com", VALID_CPF, "Senha@123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF ou e-mail já cadastrado."));
    }

    @Test
    void shouldReturn500WithFixedMessageWhenUnexpectedErrorOccurs() throws Exception {
        when(createUserUseCase.create(any(User.class)))
                .thenThrow(new RuntimeException("detalhe interno que não pode vazar"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maria", "maria@email.com", VALID_CPF, "Senha@123")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno. Tente novamente mais tarde."));
    }

    @Test
    void shouldListUsersWithoutPassword() throws Exception {
        when(listUsersUseCase.list()).thenReturn(List.of(savedUser()));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maria"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void shouldReturn404WhenUserIsNotFound() throws Exception {
        when(findUserByIdUseCase.findById(99L))
                .thenThrow(new UserNotFoundException("Usuário não encontrado."));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
    }

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    @Test
    void shouldUpdateUserWithoutPassword() throws Exception {
        when(updateUserUseCase.update(eq(1L), any(User.class))).thenReturn(savedUser());

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria","email":"maria@email.com","cpf":"%s"}
                                """.formatted(VALID_CPF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn204WhenUserIsDeleted() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownUser() throws Exception {
        doThrow(new UserNotFoundException("Usuário não encontrado."))
                .when(deleteUserUseCase).delete(99L);

        mockMvc.perform(delete("/users/99"))
                .andExpect(status().isNotFound());
    }
}
