package com.financeapp.user.service;

import com.financeapp.common.Constants;
import com.financeapp.common.ServiceException;
import com.financeapp.user.entity.UserAccount;
import com.financeapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

/**
 * DESIGN PATTERN: Domain Service Pattern
 * Handles user business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepository userRepository;

    @Transactional
    public UserDTO createUser(String email, String name, String role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ServiceException("User already exists", "USER_EXISTS", 409);
        }

        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setName(name.trim());
        user.setRole(normalizeRole(role));
        user.setActive(true);

        UserDTO response = toDto(userRepository.save(user));
        
        kafkaTemplate.send(Constants.KAFKA_USER_CREATED_TOPIC, response.getId(), response);
        log.info("User created: {}", email);

        return response;
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(String id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ServiceException("User not found", "NOT_FOUND", 404));
    }

    @Transactional
    public UserDTO changeRole(String userId, String newRole) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found", "NOT_FOUND", 404));
        user.setRole(normalizeRole(newRole));
        UserDTO response = toDto(userRepository.save(user));
        kafkaTemplate.send(Constants.KAFKA_USER_ROLE_CHANGED_TOPIC, userId, response);
        log.info("User role changed: {} -> {}", userId, newRole);
        return response;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private UserDTO toDto(UserAccount user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
