package br.com.beca.users.adapters.out.persistence;

import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import br.com.beca.users.adapters.out.persistence.mapper.UserMapper;
import br.com.beca.users.adapters.out.persistence.repository.UserJpaRepository;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        try {
            UserEntity entity = userMapper.toEntity(user);
            UserEntity savedEntity = userJpaRepository.save(entity);
            return userMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("CPF ou e-mail já cadastrado.");
        }
    }

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

}

