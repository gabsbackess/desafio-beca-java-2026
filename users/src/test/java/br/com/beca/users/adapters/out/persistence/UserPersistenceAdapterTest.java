package br.com.beca.users.adapters.out.persistence;

import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import br.com.beca.users.adapters.out.persistence.mapper.UserMapper;
import br.com.beca.users.adapters.out.persistence.repository.UserJpaRepository;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserPersistenceAdapter adapter;
    private final User user = new User("Maria", "maria@email.com", "52998224725", "hash");

    @BeforeEach
    void setUp() {
        adapter = new UserPersistenceAdapter(userJpaRepository, new UserMapper());
    }

    @Test
    void shouldThrowUserAlreadyExistsWhenUniqueConstraintIsViolated() {
        when(userJpaRepository.save(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicado", new SQLException("duplicate key", "23505")));

        assertThrows(UserAlreadyExistsException.class, () -> adapter.save(user));
    }

    @Test
    void shouldNotReportDuplicateWhenOtherIntegrityErrorOccurs() {
        DataIntegrityViolationException notNull = new DataIntegrityViolationException(
                "campo nulo", new SQLException("null value", "23502"));
        when(userJpaRepository.save(any(UserEntity.class))).thenThrow(notNull);

        DataIntegrityViolationException thrown =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.save(user));

        assertSame(notNull, thrown);
    }

    @Test
    void shouldFindUserByEmail() {
        UserEntity entity = new UserEntity();
        entity.setName("Maria");
        entity.setEmail("maria@email.com");
        entity.setCpf("52998224725");
        entity.setPassword("hash");
        when(userJpaRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(entity));

        Optional<User> result = adapter.findByEmail("maria@email.com");

        assertTrue(result.isPresent());
        assertEquals("maria@email.com", result.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenEmailIsNotFound() {
        when(userJpaRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        assertTrue(adapter.findByEmail("x@email.com").isEmpty());
    }
}
