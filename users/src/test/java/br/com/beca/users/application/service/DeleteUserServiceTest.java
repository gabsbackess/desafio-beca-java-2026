package br.com.beca.users.application.service;

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
        Long id = 1L;

        deleteUserService.delete(id);

        verify(userRepositoryPort).deleteById(id);
    }

    @Test
    void shouldNotDeleteUserWithDifferentId() {
        Long id = 1L;

        deleteUserService.delete(id);

        verify(userRepositoryPort, never()).deleteById(2L);
    }
}