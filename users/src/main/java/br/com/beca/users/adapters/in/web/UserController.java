package br.com.beca.users.adapters.in.web;
import br.com.beca.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.users.adapters.in.web.dto.CreateUserResponse;
import br.com.beca.users.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.adapters.in.web.dto.UserWebMapper;

@RestController
@RequestMapping("/users")

public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final UserWebMapper userWebMapper;

    public UserController(CreateUserUseCase createUserUseCase, UserWebMapper userWebMapper) {
        this.createUserUseCase = createUserUseCase;
        this.userWebMapper = userWebMapper;
    }
    @PostMapping
    public CreateUserResponse create (@Valid @RequestBody CreateUserRequest request){
        User user = userWebMapper.toDomain(request);
        User savedUser = createUserUseCase.create(user);
        CreateUserResponse response = userWebMapper.toResponse(savedUser);
        return response;
    }
}
