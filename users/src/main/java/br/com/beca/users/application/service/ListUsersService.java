package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.ListUsersUseCase;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListUsersService implements ListUsersUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public ListUsersService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public List<User> list() {
        return userRepositoryPort.list();
    }
}

