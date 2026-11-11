package br.com.beca.transactions.adapters.in.web.security;

import br.com.beca.transactions.application.port.out.UserAuthenticationPort;
import br.com.beca.transactions.domain.exception.AuthServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidCredentialsException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Autenticação básica (e-mail e senha) validada contra o MS1.
 * O principal da requisição passa a ser o {@link AuthenticatedUser}, com o id do usuário.
 */
@Component
public class UsersServiceAuthenticationProvider implements AuthenticationProvider {

    private final UserAuthenticationPort userAuthenticationPort;

    public UsersServiceAuthenticationProvider(UserAuthenticationPort userAuthenticationPort) {
        this.userAuthenticationPort = userAuthenticationPort;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        Object credentials = authentication.getCredentials();
        if (email == null || email.isBlank() || credentials == null) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        try {
            AuthenticatedUser user = userAuthenticationPort.authenticate(email.trim(), credentials.toString());
            return UsernamePasswordAuthenticationToken.authenticated(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException("Credenciais inválidas.");
        } catch (AuthServiceUnavailableException exception) {
            throw new AuthenticationServiceException("Serviço de usuários indisponível.", exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
