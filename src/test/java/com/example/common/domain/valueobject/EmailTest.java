package com.example.common.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = Email.of("test@example.com");
        
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    void shouldConvertToLowercase() {
        Email email = Email.of("TEST@EXAMPLE.COM");
        
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Email cannot be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid",
        "@example.com",
        "test@",
        "test@.com",
        ""
    })
    void shouldThrowExceptionForInvalidEmail(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "user.name@example.co.kr",
        "user+tag@example.com",
        "first.last@example.museum",
        "123@example.com"
    })
    void shouldAcceptValidEmailFormats(String validEmail) {
        Email email = Email.of(validEmail);
        
        assertThat(email.getValue()).isEqualTo(validEmail.toLowerCase());
    }

    @Test
    void shouldTestEquality() {
        Email email1 = Email.of("test@example.com");
        Email email2 = Email.of("test@example.com");
        Email email3 = Email.of("other@example.com");
        
        assertThat(email1).isEqualTo(email2);
        assertThat(email1).isNotEqualTo(email3);
    }

    @Test
    void shouldHaveSameHashCodeForEqualEmails() {
        Email email1 = Email.of("test@example.com");
        Email email2 = Email.of("test@example.com");
        
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    void shouldReturnValueInToString() {
        Email email = Email.of("test@example.com");
        
        assertThat(email.toString()).isEqualTo("test@example.com");
    }
}
