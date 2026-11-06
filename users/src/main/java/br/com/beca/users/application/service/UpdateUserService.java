package br.com.beca.users.application.service;
import br.com.beca.users.application.port.in.UpdateUserUseCase;
import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import br.com.beca.users.domain.validation.CpfValidator;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserService implements UpdateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public UpdateUserService(UserRepositoryPort userRepositoryPort,
                             PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }
    @Override
    public User update(Long id, User user) {
        User existingUser = userRepositoryPort.findById(id);
        String encryptedPassword;
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            encryptedPassword = existingUser.getPassword();
        } else {
            encryptedPassword = passwordEncoderPort.encode(user.getPassword());
        }
        String normalizedCpf = CpfValidator.normalize(user.getCpf());
        User userWithEncryptedPassword = new User(
                id,
                user.getName(),
                user.getEmail(),
                normalizedCpf,
                encryptedPassword,
                existingUser.getCreatedAt());
        return userRepositoryPort.update(id, userWithEncryptedPassword);
    }
}


