package br.com.beca.users.application.port.in;

import java.util.UUID;
import br.com.beca.users.domain.model.User;

public interface FindUserByIdUseCase {
    User findById(UUID id);
}
