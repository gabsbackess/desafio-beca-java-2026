package br.com.beca.users.application.port.out;
import java.util.Optional;
import java.util.UUID;
import br.com.beca.users.domain.model.User;

import java.util.List;

public interface UserRepositoryPort {
    User save(User user);
    List<User> list();
    User findById(UUID id);
    Optional<User> findByEmail(String email);
    User update (UUID id ,User user);
    void deleteById (UUID id);
}
