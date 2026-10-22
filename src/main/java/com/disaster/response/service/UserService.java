package com.disaster.response.service;

import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.UserRole;
import com.disaster.response.dto.auth.UserResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(UserRole role, Pageable pageable) {
        if (role != null) {
            return userRepository.findByRole(role, pageable).map(UserResponse::from);
        }
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, String name, String phone, Boolean active, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String oldValues = "name=" + user.getName() + ", active=" + user.isActive();

        if (name != null && !name.isBlank()) user.setName(name);
        if (phone != null) user.setPhone(phone);
        if (active != null) user.setActive(active);

        User saved = userRepository.save(user);
        String newValues = "name=" + saved.getName() + ", active=" + saved.isActive();
        auditService.record(admin, "UPDATE_USER", "User", saved.getId().toString(), oldValues, newValues);

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse updateUserRole(UUID userId, UserRole newRole, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);

        User saved = userRepository.save(user);
        auditService.record(admin, "UPDATE_USER_ROLE", "User", saved.getId().toString(),
                "role=" + oldRole, "role=" + newRole);

        return UserResponse.from(saved);
    }
}
