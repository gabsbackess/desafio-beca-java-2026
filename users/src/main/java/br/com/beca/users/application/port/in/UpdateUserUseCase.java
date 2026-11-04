package br.com.beca.users.application.port.in;
import br.com.beca.users.domain.model.User;

    public interface UpdateUserUseCase {
        User update(Long id, User user);
    }

