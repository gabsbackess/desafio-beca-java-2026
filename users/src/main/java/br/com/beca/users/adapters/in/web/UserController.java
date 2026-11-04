package br.com.beca.users.adapters.in.web;

import br.com.beca.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.users.adapters.in.web.dto.CreateUserResponse;
import br.com.beca.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.users.adapters.in.web.dto.UserWebMapper;
import br.com.beca.users.application.port.in.*;
import br.com.beca.users.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")

public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UserWebMapper userWebMapper;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;


    public UserController(CreateUserUseCase createUserUseCase,
                          ListUsersUseCase listUsersUseCase, UserWebMapper userWebMapper,
                          FindUserByIdUseCase findUserByIdUseCase, UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.userWebMapper = userWebMapper;
        this.findUserByIdUseCase = findUserByIdUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse create (@Valid @RequestBody CreateUserRequest request){
        User user = userWebMapper.toDomain(request);
        User savedUser = createUserUseCase.create(user);
        CreateUserResponse response = userWebMapper.toResponse(savedUser);
        return response;
    }
    @GetMapping
    public List<CreateUserResponse> list() {
        List<User> users = listUsersUseCase.list();

        return users.stream()
                .map(userWebMapper::toResponse)
                .toList();
    }
    @GetMapping("/{id}")
    public CreateUserResponse findById(@PathVariable Long id) {
        User user = findUserByIdUseCase.findById(id);
        return userWebMapper.toResponse(user);
    }
    @PutMapping("/{id}")
    public CreateUserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userWebMapper.toDomain(request);
        User updatedUser = updateUserUseCase.update(id, user);
        return userWebMapper.toResponse(updatedUser);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUserUseCase.delete(id);

    }

}
