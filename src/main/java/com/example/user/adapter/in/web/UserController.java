package com.example.user.adapter.web;

import com.example.common.domain.valueobject.Email;
import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.application.service.RegisterUserHandler;
import com.example.user.domain.model.User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final RegisterUserHandler registerUserHandler;

    public UserController(RegisterUserHandler registerUserHandler) {
        this.registerUserHandler = registerUserHandler;
    }

    @PostMapping("/register")
    public Mono<UserResponse> register(@RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
            request.name(),
            Email.of(request.email()),
            request.password()
        );
        return registerUserHandler.handle(command)
            .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail().getValue()));
    }

    public record RegisterRequest(String name, String email, String password) {}
    public record UserResponse(String id, String name, String email) {}
}
