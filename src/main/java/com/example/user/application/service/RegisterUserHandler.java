package com.example.user.application.service;

import com.example.user.application.port.in.RegisterUserUseCase;
import com.example.user.application.dto.RegisterUserCommand;
import com.example.user.domain.model.User;
import com.example.user.application.port.out.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RegisterUserHandler implements RegisterUserUseCase {
    private final UserRepository userRepository;

    public RegisterUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<User> handle(RegisterUserCommand command) {
        return userRepository.existsByEmail(command.email().getValue())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalArgumentException("User with email already exists"));
                }
                User user = User.create(command.name(), command.email(), command.password());
                return userRepository.save(user);
            });
    }
}