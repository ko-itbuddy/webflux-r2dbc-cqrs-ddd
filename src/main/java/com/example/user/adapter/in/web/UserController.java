package com.example.user.adapter.in.web;

import com.example.common.adapter.in.web.dto.ApiResponse;
import com.example.user.adapter.in.web.dto.UserResponse;
import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;

    public UserController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterUserCommand command) {
        return registerUserUseCase.handle(command)
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail().getValue()))
                .map(res -> ApiResponse.success(HttpStatus.CREATED.value(), res));
    }
}