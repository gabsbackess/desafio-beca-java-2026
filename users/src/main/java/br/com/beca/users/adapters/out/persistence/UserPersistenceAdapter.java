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

import java.util.List;

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

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
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
            throw new UserAlreadyExistsException("CPF ou e-mail já cadastrado.");
        }
    }
    @Override

    public void deleteById(Long id) {

        UserEntity entity = userJpaRepository.findById(id)

                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        userJpaRepository.delete(entity);

    }


}

