package br.com.beca.users.adapters.in.web.security;

import br.com.beca.users.application.port.in.AuthenticateUserUseCase;
import br.com.beca.users.domain.exception.InvalidCredentialsException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersAuthenticationProviderTest {

    private static final UUID ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;
    @InjectMocks
    private UsersAuthenticationProvider provider;

    @Test
    void shouldReturnAuthenticatedUserWithoutPasswordWhenCredentialsAreValid() {
        when(authenticateUserUseCase.authenticate("maria@email.com", "Senha@123"))
                .thenReturn(new User(ID, "Maria", "maria@email.com", "52998224725", "hash",
                        LocalDateTime.of(2026, 1, 10, 10, 0)));

        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(" maria@email.com ", "Senha@123"));

        assertTrue(result.isAuthenticated());
        assertEquals(new AuthenticatedUser(ID, "maria@email.com"), result.getPrincipal());
        assertEquals(null, result.getCredentials());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void shouldThrowBadCredentialsWhenUseCaseRejectsCredentials() {
        when(authenticateUserUseCase.authenticate("maria@email.com", "errada"))
                .thenThrow(new InvalidCredentialsException("Credenciais inválidas."));

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("maria@email.com", "errada")));
    }

    @Test
    void shouldThrowBadCredentialsWhenEmailIsBlank() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("  ", "Senha@123")));
        verifyNoInteractions(authenticateUserUseCase);
    }
}
