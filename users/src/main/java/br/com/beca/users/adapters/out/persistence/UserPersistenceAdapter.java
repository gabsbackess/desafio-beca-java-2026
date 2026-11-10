package br.com.beca.users.adapters.out.persistence;

import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import br.com.beca.users.adapters.out.persistence.mapper.UserMapper;
import br.com.beca.users.adapters.out.persistence.repository.UserJpaRepository;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.exception.UserNotFoundException;
import br.com.beca.users.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    // Código SQLSTATE do PostgreSQL para violação de unicidade (unique_violation).
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String ALREADY_EXISTS_MESSAGE = "CPF ou e-mail já cadastrado.";

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        try {
            UserEntity entity = userMapper.toEntity(user);
            UserEntity savedEntity = userJpaRepository.save(entity);
            return userMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception);
        }
    }

    @Override
    public List<User> list() {
        List<UserEntity> entities = userJpaRepository.findAll();
        return userMapper.toDomainList(entities);
    }

    @Override
    public User findById(Long id) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
        return userMapper.toDomain(entity);
    }

    @Override
    public User update(Long id, User user) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setCpf(user.getCpf());
        entity.setPassword(user.getPassword());
        try {
            UserEntity updatedEntity = userJpaRepository.save(entity);
            return userMapper.toDomain(updatedEntity);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void deleteById(Long id) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
        userJpaRepository.delete(entity);
    }

    /**
     * Só a violação de unicidade (CPF ou e-mail repetido) vira "já cadastrado" (409).
     * Qualquer outro erro de integridade é um defeito e segue como erro inesperado (500).
     */
    private RuntimeException translate(DataIntegrityViolationException exception) {
        if (isUniqueViolation(exception)) {
            return new UserAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }
        return exception;
    }

    private boolean isUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }
}
