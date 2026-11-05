
package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UpdateUserService updateUserService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User(
                1L,
                "Maria",
                "maria@email.com",
                "12345678901",
                "$2a$10$senhaCriptografada",
                LocalDateTime.now()
        );
    }

    @Test
    void shouldKeepCurrentPasswordWhenNewPasswordIsNull() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "12345678901",
                null
        );

        when(userRepositoryPort.findById(1L)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(1L), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        User updatedUser = updateUserService.update(1L, user);

        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
        verify(userRepositoryPort).update(eq(1L), any(User.class));
    }

    @Test
    void shouldKeepCurrentPasswordWhenNewPasswordIsEmpty() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "12345678901",
                ""
        );

        when(userRepositoryPort.findById(1L)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(1L), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        User updatedUser = updateUserService.update(1L, user);

        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    void shouldKeepCurrentPasswordWhenNewPasswordContainsOnlySpaces() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "12345678901",
                "   "
        );

        when(userRepositoryPort.findById(1L)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(1L), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        User updatedUser = updateUserService.update(1L, user);

        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    void shouldEncodeNewPasswordWhenPasswordIsProvided() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "12345678901",
                "novaSenha123"
        );

        when(userRepositoryPort.findById(1L)).thenReturn(existingUser);
        when(passwordEncoderPort.encode("novaSenha123"))
                .thenReturn("$2a$10$novaSenhaCriptografada");
        when(userRepositoryPort.update(eq(1L), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        User updatedUser = updateUserService.update(1L, user);

        assertEquals("$2a$10$novaSenhaCriptografada", updatedUser.getPassword());
        verify(passwordEncoderPort).encode("novaSenha123");
    }

    @Test
    void shouldSaveUpdatedUserInRepository() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "12345678901",
                null
        );

        when(userRepositoryPort.findById(1L)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(1L), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        updateUserService.update(1L, user);

        verify(userRepositoryPort).findById(1L);
        verify(userRepositoryPort).update(eq(1L), any(User.class));
    }
}

