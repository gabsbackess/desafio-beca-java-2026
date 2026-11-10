package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.AuthenticateUserUseCase;
import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.InvalidCredentialsException;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final String INVALID_CREDENTIALS = "Credenciais inválidas.";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public AuthenticateUserService(UserRepositoryPort userRepositoryPort,
                                   PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public User authenticate(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        if (rawPassword == null || !passwordEncoderPort.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        return user;
    }
}
