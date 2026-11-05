package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindUserByIdServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private FindUserByIdService findUserByIdService;

    @Test
    void shouldFindUserById() {
        Long id = 1L;

        User user = new User(
                id,
                "Maria",
                "maria@email.com",
                "12345678901",
                "senhaCriptografada",
                null
        );

        when(userRepositoryPort.findById(id)).thenReturn(user);

        User result = findUserByIdService.findById(id);

        assertSame(user, result);
        verify(userRepositoryPort).findById(id);
    }

    @Test
    void shouldPassCorrectIdToRepository() {
        Long id = 10L;

        when(userRepositoryPort.findById(id)).thenReturn(null);

        findUserByIdService.findById(id);

        verify(userRepositoryPort).findById(10L);
    }
}