package com.example.user.application.port.in;

import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.domain.model.User;
import reactor.core.publisher.Mono;

public interface RegisterUserUseCase {
    Mono<User> handle(RegisterUserCommand command);
}
