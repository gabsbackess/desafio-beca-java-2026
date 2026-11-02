package br.com.beca.users.adapters.out.persistence;
import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import br.com.beca.users.adapters.out.persistence.mapper.UserMapper;
import br.com.beca.users.adapters.out.persistence.repository.UserJpaRepository;
import br.com.beca.users.application.port.out.UserRepositoryPort;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }
    public UserPersistenceAdapter(
            UserJpaRepository userJpaRepository,
            UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }
}
