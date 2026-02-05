package com.example.user.domain.model;

import com.example.common.domain.valueobject.Email;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class User {
    @Id
    @EqualsAndHashCode.Include
    private String id;
    
    private String name;
    
    @Embedded
    private Email email;
    
    private String password; // Encrypted in a real app
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;

    public static User create(String name, Email email, String password) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new User(id, name, email, password, now, now);
    }

    public static User reconstitute(String id, String name, Email email, String password, Instant createdAt, Instant updatedAt) {
        return new User(id, name, email, password, createdAt, updatedAt);
    }
}