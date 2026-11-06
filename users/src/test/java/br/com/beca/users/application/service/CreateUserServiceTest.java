package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
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
    void shouldCreateUserWithEncodedPassword() {
        // Arrange: preparar os dados
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senha123"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Act: executar o cadastro
        User createdUser = createUserService.create(user);
        // Assert: verificar o resultado
        assertNotNull(createdUser);
        assertEquals("senhaCodificada", createdUser.getPassword());
        verify(passwordEncoderPort).encode("senha123");
        verify(userRepositoryPort).save(any(User.class));
    }
    @Test
    void shouldEncodeOriginalPassword() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senha123"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        createUserService.create(user);
        verify(passwordEncoderPort, times(1)).encode("senha123");
    }
    @Test
    void shouldSaveUserOnlyOnce() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senha123"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        createUserService.create(user);
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }
    @Test
    void shouldReturnUserSavedByRepository() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senha123"
        );
        User savedUser = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senhaCodificada"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenReturn(savedUser);
        User result = createUserService.create(user);
        assertSame(savedUser, result);
    }
    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "senha123"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenThrow(new RuntimeException("Erro ao salvar usuário"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> createUserService.create(user)
        );
        assertEquals("Erro ao salvar usuário", exception.getMessage());
    }
    @Test
    void shouldNormalizeCpfWhenCreatingUser() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "529.982.247-25",
                "senha123"
        );
        when(passwordEncoderPort.encode("senha123"))
                .thenReturn("senhaCodificada");
        when(userRepositoryPort.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User createdUser = createUserService.create(user);
        assertEquals("52998224725", createdUser.getCpf());
    }
}
