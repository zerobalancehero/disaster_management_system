package com.disaster.response.service;

import com.disaster.response.config.JwtTokenProvider;
import com.disaster.response.domain.entity.User;
import com.disaster.response.dto.auth.LoginRequest;
import com.disaster.response.dto.auth.LoginResponse;
import com.disaster.response.dto.auth.UserResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long expirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.expirationMs = expirationMs;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return LoginResponse.of(token, expirationMs, UserResponse.from(user));
    }
}
