package com.financeapp.auth.controller;

import com.financeapp.common.*;
import com.financeapp.dto.AuthTokenResponse;
import com.financeapp.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * DESIGN PATTERN: Controller/API Gateway Pattern
 * Handles HTTP requests and delegates to service layer
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = Constants.CORRELATION_ID_HEADER, required = false) String correlationId) {
        AuthTokenResponse response = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthTokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(Constants.AUTHORIZATION_HEADER) String authHeader) {
        if (!authHeader.startsWith(Constants.BEARER_PREFIX)) {
            throw new ServiceException("Authorization header must use Bearer token", Constants.ERR_UNAUTHORIZED, 401);
        }
        String token = authHeader.replace(Constants.BEARER_PREFIX, "");
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate(
            @RequestHeader(Constants.AUTHORIZATION_HEADER) String authHeader) {
        if (!authHeader.startsWith(Constants.BEARER_PREFIX)) {
            throw new ServiceException("Authorization header must use Bearer token", Constants.ERR_UNAUTHORIZED, 401);
        }
        String token = authHeader.replace(Constants.BEARER_PREFIX, "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(valid, "Token validation result"));
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        private String email;

        @NotBlank(message = "password is required")
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "refreshToken is required")
        private String refreshToken;
    }
}
