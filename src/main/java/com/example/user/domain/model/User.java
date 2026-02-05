package com.example.user.domain.model;

import com.example.common.domain.valueobject.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    @EqualsAndHashCode.Include
    private final String id;
    private final String name;
    private final Email email;
    private final String password; // Encrypted in a real app
    private final Instant createdAt;
    private final Instant updatedAt;

    public static User create(String name, Email email, String password) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new User(id, name, email, password, now, now);
    }

    public static User reconstitute(String id, String name, Email email, String password, Instant createdAt, Instant updatedAt) {
        return new User(id, name, email, password, createdAt, updatedAt);
    }
}
