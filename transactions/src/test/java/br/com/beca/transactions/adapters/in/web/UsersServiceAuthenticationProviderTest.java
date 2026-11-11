package br.com.beca.transactions.adapters.in.web;

import br.com.beca.transactions.adapters.in.web.security.UsersServiceAuthenticationProvider;
import br.com.beca.transactions.application.port.out.UserAuthenticationPort;
import br.com.beca.transactions.domain.exception.AuthServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidCredentialsException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceAuthenticationProviderTest {

    @Mock
    private UserAuthenticationPort userAuthenticationPort;

    @InjectMocks
    private UsersServiceAuthenticationProvider provider;

    @Test
    void shouldReturnAuthenticatedTokenWithUserAsPrincipalAndNoCredentials() {
        AuthenticatedUser user = new AuthenticatedUser(Ids.USER, "Maria", "maria@email.com");
        when(userAuthenticationPort.authenticate("maria@email.com", "Senha@123")).thenReturn(user);

        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("maria@email.com", "Senha@123"));

        assertTrue(result.isAuthenticated());
        assertEquals(user, result.getPrincipal());
        assertNull(result.getCredentials());
    }

    @Test
    void shouldTranslateInvalidCredentialsToBadCredentials() {
        when(userAuthenticationPort.authenticate("maria@email.com", "errada"))
                .thenThrow(new InvalidCredentialsException("x"));

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("maria@email.com", "errada")));
    }

    @Test
    void shouldTranslateUnavailableMs1ToAuthenticationServiceException() {
        when(userAuthenticationPort.authenticate("maria@email.com", "x"))
                .thenThrow(new AuthServiceUnavailableException("fora do ar"));

        assertThrows(AuthenticationServiceException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("maria@email.com", "x")));
    }

    @Test
    void shouldSupportUsernamePasswordTokens() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }
}
