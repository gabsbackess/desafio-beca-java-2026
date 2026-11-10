package br.com.beca.users.adapters.in.web;

import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.adapters.in.web.exception.GlobalExceptionHandler;
import br.com.beca.users.application.port.in.AuthenticateUserUseCase;
import br.com.beca.users.domain.exception.InvalidCredentialsException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthController.class)
@Import({GlobalExceptionHandler.class, UserWebMapper.class})
class UserAuthControllerTest {

    private static final UUID ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Test
    void shouldReturn200WithUserAndWithoutPasswordWhenCredentialsAreValid() throws Exception {
        when(authenticateUserUseCase.authenticate(anyString(), anyString()))
                .thenReturn(new User(ID, "Maria", "maria@email.com", "52998224725", "hash-bcrypt",
                        LocalDateTime.of(2026, 1, 10, 10, 0)));

        mockMvc.perform(post("/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"password\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(authenticateUserUseCase.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Credenciais inválidas."));

        mockMvc.perform(post("/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
    }

    @Test
    void shouldReturn400WhenFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
