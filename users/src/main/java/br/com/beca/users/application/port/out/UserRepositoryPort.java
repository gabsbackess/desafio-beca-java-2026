package br.com.beca.users.application.port.out;
import br.com.beca.users.domain.model.User;

public interface UserRepositoryPort {
    User save(User user);
}
