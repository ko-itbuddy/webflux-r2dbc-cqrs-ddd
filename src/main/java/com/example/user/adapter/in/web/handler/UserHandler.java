package com.example.user.adapter.in.web.handler;

import com.example.common.domain.valueobject.Email;
import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.application.port.in.RegisterUserUseCase;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {
    private final RegisterUserUseCase registerUserUseCase;

    public UserHandler(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
            .map(req -> new RegisterUserCommand(req.name(), Email.of(req.email()), req.password()))
            .flatMap(registerUserUseCase::handle)
            .flatMap(user -> ServerResponse.ok().bodyValue(new UserResponse(user.getId(), user.getName(), user.getEmail().getValue())));
    }

    public record RegisterRequest(String name, String email, String password) {}
    public record UserResponse(String id, String name, String email) {}
}
