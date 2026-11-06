package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import br.com.beca.users.domain.validation.CpfValidator;
import org.springframework.stereotype.Service;

@Service
public class CreateUserService implements CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public CreateUserService(UserRepositoryPort userRepositoryPort,
                             PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public User create(User user) {
        String encryptedPassword = passwordEncoderPort.encode(user.getPassword());

        String normalizedCpf = CpfValidator.normalize(user.getCpf());
        User userWithEncryptedPassword = new User(
                user.getName(),
                user.getEmail(),
                normalizedCpf,
                encryptedPassword);
        return userRepositoryPort.save(userWithEncryptedPassword);
    }
}

