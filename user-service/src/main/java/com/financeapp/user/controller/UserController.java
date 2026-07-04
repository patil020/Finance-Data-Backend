package com.financeapp.user.controller;

import com.financeapp.common.ApiResponse;
import com.financeapp.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * DESIGN PATTERN: REST Controller Pattern
 * HTTP endpoints for user management
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createUser(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request.getEmail(), request.getName(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success(user, "User created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getUser(
            @PathVariable @NotBlank(message = "id is required") String id) {
        var user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<?>> changeRole(
            @PathVariable @NotBlank(message = "id is required") String id,
            @Valid @RequestBody RoleUpdateRequest request) {
        var user = userService.changeRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(user, "User role updated successfully"));
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        private String email;

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must not exceed 120 characters")
        private String name;

        @NotBlank(message = "role is required")
        @Pattern(regexp = "ADMIN|ANALYST|VIEWER", flags = Pattern.Flag.CASE_INSENSITIVE, message = "role must be ADMIN, ANALYST, or VIEWER")
        private String role;
    }

    @Data
    public static class RoleUpdateRequest {
        @NotBlank(message = "role is required")
        @Pattern(regexp = "ADMIN|ANALYST|VIEWER", flags = Pattern.Flag.CASE_INSENSITIVE, message = "role must be ADMIN, ANALYST, or VIEWER")
        private String role;
    }
}
