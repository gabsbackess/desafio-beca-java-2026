package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.FindUserByIdUseCase;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class FindUserByIdService implements FindUserByIdUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public FindUserByIdService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User findById(Long id) {
        return userRepositoryPort.findById(id);
    }
}

