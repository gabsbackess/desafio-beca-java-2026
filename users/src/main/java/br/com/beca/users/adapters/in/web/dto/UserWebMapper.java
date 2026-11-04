package br.com.beca.users.adapters.in.web.dto;

import org.springframework.stereotype.Component;
import br.com.beca.users.domain.model.User;

@Component
public class UserWebMapper {
    public User toDomain(CreateUserRequest request){
    return new User(request.getName(), request.getEmail(), request.getCpf(), request.getPassword());
    }
    public CreateUserResponse toResponse(User user){
    return new CreateUserResponse(user.getId(), user.getName(), user.getEmail(), user.getCpf(), user.getCreatedAt());
    }
    public User toDomain(UpdateUserRequest request) {
        return new User(
                request.getName(),
                request.getEmail(),
                request.getCpf(),
                request.getPassword()
        );
    }

}
