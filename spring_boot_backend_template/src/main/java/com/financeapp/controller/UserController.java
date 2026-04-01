package com.financeapp.controller;

import com.financeapp.dto.request.RoleUpdateDto;
import com.financeapp.dto.request.StatusUpdateDto;
import com.financeapp.dto.request.UserRequestDto;
import com.financeapp.dto.response.PagedResponseDto;
import com.financeapp.dto.response.UserResponseDto;
import com.financeapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(requestDto));
    }

    @GetMapping
    public ResponseEntity<PagedResponseDto<UserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto requestDto) {
        return ResponseEntity.ok(userService.updateUser(id, requestDto));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateDto roleUpdateDto) {
        return ResponseEntity.ok(userService.changeUserRole(id, roleUpdateDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto statusUpdateDto) {
        return ResponseEntity.ok(userService.changeUserStatus(id, statusUpdateDto));
    }
}
