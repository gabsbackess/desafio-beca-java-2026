package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class CreateUserService implements CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public CreateUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User create(User user) {
        return userRepositoryPort.save(user);
    }

}

