package com.disaster.response.dto.auth;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInMs,
    UserResponse user
) {
    public static LoginResponse of(String token, long expiresInMs, UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresInMs, user);
    }
}
