package com.socialpublish.auth.dto;

import com.socialpublish.auth.entity.Role;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        Role role
) {
}
