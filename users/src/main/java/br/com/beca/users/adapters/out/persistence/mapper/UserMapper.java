package br.com.beca.users.adapters.out.persistence.mapper;

import br.com.beca.users.adapters.out.persistence.entity.UserEntity;
import br.com.beca.users.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setCpf(user.getCpf());
        entity.setPassword(user.getPassword());
        return entity;
    }

    public User toDomain(UserEntity user) {
        return new User(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPassword(),
                user.getCreatedAt());
    }
    public List<User> toDomainList(List<UserEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}
