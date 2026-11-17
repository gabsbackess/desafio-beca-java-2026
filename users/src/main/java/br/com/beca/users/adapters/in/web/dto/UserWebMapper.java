package br.com.beca.users.adapters.in.web.dto;

import org.springframework.stereotype.Component;

import java.util.UUID;
import br.com.beca.users.domain.model.User;

@Component
public class UserWebMapper {
    public User toDomain(CreateUserRequest request){
    return new User(request.getName(), request.getEmail(), request.getCpf(), request.getPassword());
    }
    public CreateUserResponse toResponse(User user){
    return new CreateUserResponse(user.getId(), user.getName(), user.getEmail(), user.getCpf(), user.getCreatedAt());
    }

    /** Visão resumida, para quem não é o dono: só id e nome (sem e-mail, CPF nem data de cadastro). */
    public CreateUserResponse toSummary(User user){
    return new CreateUserResponse(user.getId(), user.getName(), null, null, null);
    }

    /** Dados completos para o dono do cadastro; resumo (id e nome) para qualquer outro. */
    public CreateUserResponse toResponseFor(User user, UUID viewerId){
    return user.getId() != null && user.getId().equals(viewerId) ? toResponse(user) : toSummary(user);
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
