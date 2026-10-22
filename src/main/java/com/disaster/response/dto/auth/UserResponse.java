package com.disaster.response.dto.auth;

import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String phone,
    UserRole role,
    boolean active,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()
        );
    }
}
