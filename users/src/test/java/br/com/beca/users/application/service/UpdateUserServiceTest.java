package br.com.beca.users.application.service;
import br.com.beca.users.application.port.out.PasswordEncoderPort;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.UserNotFoundException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
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

    private User existingUser;
    @BeforeEach
    void setUp() {
        existingUser = createExistingUser();
    }

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
    void shouldKeepCurrentPasswordWhenNewPasswordIsNull() {

        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                null
        );

        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
        verify(userRepositoryPort).update(eq(userId), any(User.class));

    }

    @Test
    void shouldKeepCurrentPasswordWhenNewPasswordIsEmpty() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                ""
        );

        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    void shouldKeepCurrentPasswordWhenNewPasswordContainsOnlySpaces() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                "   "

        );
        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals(existingUser.getPassword(), updatedUser.getPassword());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    void shouldEncodeNewPasswordWhenPasswordIsProvided() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                "novaSenha123"
        );
        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(passwordEncoderPort.encode("novaSenha123"))
                .thenReturn("novaSenhaCodificada");
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals("novaSenhaCodificada", updatedUser.getPassword());
        verify(passwordEncoderPort).encode("novaSenha123");
    }

    @Test
    void shouldSaveUpdatedUserInRepository() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                null
        );

        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        updateUserService.update(userId, user);
        verify(userRepositoryPort).findById(userId);
        verify(userRepositoryPort).update(eq(userId), any(User.class));
    }
    @Test
    void shouldNormalizeEmailWhenUpdatingUser() {
        User user = new User(
                "Maria Silva",
                " Maria@Email.com ",
                "52998224725",
                null
        );

        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals("maria@email.com", updatedUser.getEmail());
    }
    @Test
    void shouldNormalizeCpfBeforeUpdating() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "529.982.247-25",
                null
        );

        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals("52998224725", updatedUser.getCpf());
    }

    @Test
    void shouldPreserveOriginalCreationDate() {
        User user = new User(
                "Maria Silva",
                "maria@email.com",
                "52998224725",
                null
        );
        when(userRepositoryPort.findById(userId)).thenReturn(existingUser);
        when(userRepositoryPort.update(eq(userId), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        User updatedUser = updateUserService.update(userId, user);
        assertEquals(creationDate, updatedUser.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        User user = new User(
                "Maria",
                "maria@email.com",
                "52998224725",
                null
        );

        when(userRepositoryPort.findById(userId))
                .thenThrow(new UserNotFoundException("Usuário não encontrado"));
        assertThrows(
                UserNotFoundException.class,
                () -> updateUserService.update(userId, user)
        );
        verify(userRepositoryPort, never()).update(anyLong(), any(User.class));
        verifyNoInteractions(passwordEncoderPort);
    }
}
