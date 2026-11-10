package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.InvalidCredentialsException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @InjectMocks
    private AuthenticateUserService authenticateUserService;

    private final User user = new User(UUID.randomUUID(), "Maria", "maria@email.com",
            "52998224725", "hash-bcrypt", LocalDateTime.of(2026, 1, 10, 10, 0));

    @Test
    void shouldReturnUserWhenCredentialsAreValid() {
        when(userRepositoryPort.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("Senha@123", "hash-bcrypt")).thenReturn(true);

        User result = authenticateUserService.authenticate("maria@email.com", "Senha@123");

        assertSame(user, result);
    }

    @Test
    void shouldNormalizeEmailBeforeSearching() {
        when(userRepositoryPort.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("Senha@123", "hash-bcrypt")).thenReturn(true);

        authenticateUserService.authenticate("  Maria@Email.com ", "Senha@123");

        verify(userRepositoryPort).findByEmail("maria@email.com");
    }

    @Test
    void shouldThrowInvalidCredentialsWhenPasswordIsWrong() {
        when(userRepositoryPort.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("errada", "hash-bcrypt")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> authenticateUserService.authenticate("maria@email.com", "errada"));

        org.junit.jupiter.api.Assertions.assertEquals("Credenciais inválidas.", exception.getMessage());
    }

    @Test
    void shouldThrowSameErrorWhenEmailDoesNotExist() {
        when(userRepositoryPort.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> authenticateUserService.authenticate("ninguem@email.com", "Senha@123"));

        org.junit.jupiter.api.Assertions.assertEquals("Credenciais inválidas.", exception.getMessage());
        verifyNoInteractions(passwordEncoderPort);
    }
}
