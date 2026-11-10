package br.com.beca.users.application.service;

import java.util.UUID;
import br.com.beca.users.application.port.in.DeleteUserUseCase;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DeleteUserService implements DeleteUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    public DeleteUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }
    @Override
    public void delete(UUID id) {
        userRepositoryPort.deleteById(id);
    }

}

