package com.example.user.application.port.out;

import com.example.user.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> save(User user);
    Mono<User> findById(String id);
    Mono<User> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
}
