package com.example.user.application.service;

import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.application.port.in.RegisterUserUseCase;
import com.example.user.application.port.out.UserRepository;
import com.example.user.domain.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RegisterUserHandler implements RegisterUserUseCase {
    private final UserRepository userRepository;

    public RegisterUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> handle(RegisterUserCommand command) {
        return userRepository.existsByEmail(command.email().getValue())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalArgumentException("Email already exists: " + command.email().getValue()));
                }
                User user = User.create(command.name(), command.email(), command.password());
                return userRepository.save(user);
            });
    }
}
