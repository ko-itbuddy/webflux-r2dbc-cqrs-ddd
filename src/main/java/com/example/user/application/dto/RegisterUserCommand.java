package com.example.user.application.dto;

import com.example.common.domain.valueobject.Email;

public record RegisterUserCommand(
    String name,
    Email email,
    String password
) {
}
