package br.com.beca.users.application.service;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.UserNotFoundException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @InjectMocks
    private UpdateUserService updateUserService;
    private final Long userId = 1L;
    private final LocalDateTime creationDate =

            LocalDateTime.of(2026, 1, 10, 10, 0);

    private User createExistingUser() {
        return new User(
                userId,
                "Maria",
                "maria@email.com",
                "52998224725",
                "senhaAntigaCodificada",
                creationDate
        );
    }

    @Test
    void shouldPreserveOldPasswordWhenNewPasswordIsNotProvided() {
        // Arrange
        User existingUser = createExistingUser();
        User userRequest = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                null

        );
        when(userRepositoryPort.findById(userId))
                .thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        // Act
        User updatedUser = updateUserService.update(userId, userRequest);
        // Assert
        assertEquals(
                "senhaAntigaCodificada",
                updatedUser.getPassword()
        );
        verify(passwordEncoderPort, never()).encode(any(String.class));
        verify(userRepositoryPort).update(eq(userId), any(User.class));
    }

    @Test
    void shouldEncodeNewPasswordWhenProvided() {
        // Arrange
        User existingUser = createExistingUser();
        User userRequest = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                "novaSenha123"
        );
        when(userRepositoryPort.findById(userId))
                .thenReturn(existingUser);
        when(passwordEncoderPort.encode("novaSenha123"))
                .thenReturn("novaSenhaCodificada");
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        // Act
        User updatedUser = updateUserService.update(userId, userRequest);
        // Assert
        assertEquals("novaSenhaCodificada", updatedUser.getPassword());
        verify(passwordEncoderPort).encode("novaSenha123");
        verify(userRepositoryPort).update(eq(userId), any(User.class));
    }

    @Test
    void shouldNormalizeCpfBeforeUpdating() {
        // Arrange
        User existingUser = createExistingUser();
        User userRequest = new User(
                "Maria",
                "maria@email.com",
                "529.982.247-25",
                null
        );
        when(userRepositoryPort.findById(userId))
                .thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        // Act
        User updatedUser = updateUserService.update(userId, userRequest);
        // Assert
        assertEquals("52998224725", updatedUser.getCpf());
    }

    @Test
    void shouldPreserveOriginalCreationDate() {
        // Arrange
        User existingUser = createExistingUser();
        User userRequest = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                null
        );
        when(userRepositoryPort.findById(userId))
                .thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        // Act
        User updatedUser = updateUserService.update(userId, userRequest);
        // Assert
        assertEquals(creationDate, updatedUser.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        // Arrange
        User userRequest = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                null
        );
        when(userRepositoryPort.findById(userId))
                .thenThrow(new UserNotFoundException("Usuário não encontrado"));
        // Act and Assert
        assertThrows(
                UserNotFoundException.class,
                () -> updateUserService.update(userId, userRequest)
        );
        verify(userRepositoryPort, never()).update(anyLong(), any(User.class));
        verifyNoInteractions(passwordEncoderPort);
    }
}