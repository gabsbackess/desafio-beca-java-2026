package br.com.beca.users.adapters.in.web.security;

import br.com.beca.users.application.port.in.AuthenticateUserUseCase;
import br.com.beca.users.domain.exception.InvalidCredentialsException;
import br.com.beca.users.domain.model.User;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Autenticação básica (e-mail e senha) usando o mesmo caso de uso de {@code POST /users/authenticate}.
 * O principal da requisição passa a ser o {@link AuthenticatedUser}, com o id do usuário.
 */
@Component
public class UsersAuthenticationProvider implements AuthenticationProvider {

    private static final String INVALID_CREDENTIALS = "Credenciais inválidas.";

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public UsersAuthenticationProvider(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        Object credentials = authentication.getCredentials();
        if (email == null || email.isBlank() || credentials == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        try {
            User user = authenticateUserUseCase.authenticate(email.trim(), credentials.toString());
            return UsernamePasswordAuthenticationToken.authenticated(
                    new AuthenticatedUser(user.getId(), user.getEmail()),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
