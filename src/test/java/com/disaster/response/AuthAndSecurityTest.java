package com.disaster.response;

import com.disaster.response.config.JwtTokenProvider;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.UserRole;
import com.disaster.response.dto.auth.LoginRequest;
import com.disaster.response.dto.auth.LoginResponse;
import com.disaster.response.repository.UserRepository;
import com.disaster.response.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthAndSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                3600000L
        );
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, 3600000L);
    }

    @Test
    @DisplayName("Authentication Success: Valid credentials return JWT Bearer token and user response")
    void testAuthenticationSuccess() {
        UUID userId = UUID.randomUUID();
        User user = new User("Test Citizen", "user@disaster.local", "hashedPassword", "+919999999999", UserRole.USER);
        user.setId(userId);

        when(userRepository.findByEmail("user@disaster.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("User@123", "hashedPassword")).thenReturn(true);

        LoginRequest request = new LoginRequest("user@disaster.local", "User@123");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("user@disaster.local", response.user().email());
        assertEquals(UserRole.USER, response.user().role());
        assertTrue(jwtTokenProvider.validateToken(response.token()));
        assertEquals(userId, jwtTokenProvider.getUserId(response.token()));
    }

    @Test
    @DisplayName("Authentication Failure: Invalid password throws BadCredentialsException")
    void testAuthenticationFailureWrongPassword() {
        User user = new User("Test Citizen", "user@disaster.local", "hashedPassword", null, UserRole.USER);
        when(userRepository.findByEmail("user@disaster.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest("user@disaster.local", "WrongPass");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Authentication Failure: Non-existent user throws BadCredentialsException")
    void testAuthenticationFailureUserNotFound() {
        when(userRepository.findByEmail("unknown@disaster.local")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown@disaster.local", "Password@123");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Authentication Failure: Deactivated account cannot login")
    void testDeactivatedAccountCannotLogin() {
        User user = new User("Deactivated", "inactive@disaster.local", "hashedPassword", null, UserRole.USER);
        user.setActive(false);
        when(userRepository.findByEmail("inactive@disaster.local")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("inactive@disaster.local", "User@123");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
