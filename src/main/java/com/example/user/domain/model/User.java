package com.example.user.domain.model;

import com.example.common.domain.model.BaseEntity;
import com.example.common.domain.valueobject.Email;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity<User> {
    @Id
    @EqualsAndHashCode.Include
    private String id;
    
    private String name;
    
    @Embedded
    private Email email;
    
    private String password;

    public static User create(String name, Email email, String password) {
        String id = UUID.randomUUID().toString();
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .password(password)
                .build();
    }

    public static User reconstitute(String id, String name, Email email, String password, Instant createdAt, Instant updatedAt) {
        User user = User.builder()
                .id(id)
                .name(name)
                .email(email)
                .password(password)
                .build();
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }
}
