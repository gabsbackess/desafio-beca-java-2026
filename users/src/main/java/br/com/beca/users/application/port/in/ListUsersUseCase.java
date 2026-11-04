package br.com.beca.users.application.port.in;

import java.util.List;
import br.com.beca.users.domain.model.User;

public interface ListUsersUseCase {
    List<User> list();
}
