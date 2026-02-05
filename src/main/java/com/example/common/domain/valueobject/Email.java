package com.example.common.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires protected constructor
@Embeddable
public final class Email {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    @Column(name = "email")
    private String value;
    
    public static Email of(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return new Email(email.toLowerCase());
    }
    
    @Override
    public String toString() {
        return value;
    }
}
