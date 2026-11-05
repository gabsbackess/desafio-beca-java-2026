
package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private CreateUserService createUserService;

    @Test
    void shouldCreateUserSuccessfully() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "12345678901",
                "senha123"
        );

        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCriptografada");

        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = createUserService.create(user);

        assertEquals("Maria", createdUser.getName());
        assertEquals("maria@email.com", createdUser.getEmail());
        assertEquals("senhaCriptografada", createdUser.getPassword());

        verify(userRepositoryPort).save(any(User.class));
    }

    @Test
    void shouldNormalizeEmailBeforeSaving() {
        User user = new User(
                "Maria",
                " Maria@Email.com ",
                "12345678901",
                "senha123"
        );

        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCriptografada");

        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = createUserService.create(user);

        assertEquals("maria@email.com", createdUser.getEmail());
    }

    @Test
    void shouldEncodePasswordBeforeSaving() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "12345678901",
                "senha123"
        );

        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCriptografada");

        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        createUserService.create(user);

        verify(passwordEncoderPort).encode("senha123");
    }

    @Test
    void shouldSaveUserWithEncryptedPassword() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "12345678901",
                "senha123"
        );

        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCriptografada");

        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = createUserService.create(user);

        assertEquals("senhaCriptografada", createdUser.getPassword());
        verify(userRepositoryPort).save(argThat(savedUser ->
                savedUser.getPassword().equals("senhaCriptografada")
        ));
    }
}


 