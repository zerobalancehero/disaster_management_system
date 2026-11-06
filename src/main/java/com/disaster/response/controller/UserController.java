package com.disaster.response.controller;

import com.disaster.response.config.UserPrincipal;
import com.disaster.response.dto.auth.UserResponse;
import com.disaster.response.dto.common.PagedResponse;
import com.disaster.response.dto.notification.NotificationResponse;
import com.disaster.response.service.NotificationService;
import com.disaster.response.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current authenticated user profile and notifications")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;

    public UserController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getUserById(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Retrieve in-app notifications for the authenticated user")
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(notificationService.getNotificationsForUser(principal.getId(), pageable)));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Mark an in-app notification as read")
    public ResponseEntity<NotificationResponse> markNotificationRead(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal.getId()));
    }
}
