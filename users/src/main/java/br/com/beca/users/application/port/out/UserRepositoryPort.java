package br.com.beca.users.application.port.out;
import br.com.beca.users.domain.model.User;

import java.util.List;

public interface UserRepositoryPort {
    User save(User user);
    List<User> list();
    User findById(Long id);
    User update (Long id ,User user);
    void deleteById (Long id);
}
