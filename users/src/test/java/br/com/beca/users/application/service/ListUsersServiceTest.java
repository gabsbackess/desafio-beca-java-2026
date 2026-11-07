package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private ListUsersService listUsersService;

    @Test
    void shouldReturnAllUsers() {
        List<User> users = List.of(
                new User("Maria", "maria@email.com", "12345678901", "senha"),
                new User("João", "joao@email.com", "12345678902", "senha")
        );

        when(userRepositoryPort.list()).thenReturn(users);

        List<User> result = listUsersService.list();

        assertEquals(2, result.size());
        assertEquals(users, result);
        verify(userRepositoryPort).list();
    }

    @Test
    void shouldReturnEmptyListWhenNoUsersExist() {
        when(userRepositoryPort.list()).thenReturn(List.of());

        List<User> result = listUsersService.list();

        assertEquals(0, result.size());
        verify(userRepositoryPort).list();
    }
}