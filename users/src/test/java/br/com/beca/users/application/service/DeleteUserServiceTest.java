package br.com.beca.users.application.service;

import java.util.UUID;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private DeleteUserService deleteUserService;

    @Test
    void shouldDeleteUserById() {
        UUID id = UUID.randomUUID();

        deleteUserService.delete(id);

        verify(userRepositoryPort).deleteById(id);
    }

    @Test
    void shouldNotDeleteUserWithDifferentId() {
        UUID id = UUID.randomUUID();

        deleteUserService.delete(id);

        verify(userRepositoryPort, never()).deleteById(UUID.randomUUID());
    }
}