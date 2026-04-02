package com.financeapp.service.impl;

import com.financeapp.dao.UserDao;
import com.financeapp.dto.request.RoleUpdateDto;
import com.financeapp.dto.request.StatusUpdateDto;
import com.financeapp.dto.request.UserRequestDto;
import com.financeapp.dto.response.PagedResponseDto;
import com.financeapp.dto.response.UserResponseDto;
import com.financeapp.entities.User;
import com.financeapp.exception.BadRequestException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto requestDto) {
        String email = requestDto.getEmail().trim().toLowerCase();
        log.info("Creating new user with email: {}", email);
        if (userDao.existsByEmailIgnoreCase(email)) {
            log.warn("User registration failed: Email already exists - {}", email);
            throw new BadRequestException("Email is already registered.");
        }

        User user = User.builder()
                .fullName(requestDto.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(requestDto.getRole())
                .status(requestDto.getStatus())
                .build();
        User savedUser = userDao.save(user);
        log.info("User created successfully with ID: {} and email: {}", savedUser.getId(), email);
        return modelMapper.map(savedUser, UserResponseDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<UserResponseDto> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<UserResponseDto> responsePage = userDao.findAll(pageable).map(user -> modelMapper.map(user, UserResponseDto.class));
        return PagedResponseDto.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = findUserById(id);
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        User user = findUserById(id);
        String email = requestDto.getEmail().trim().toLowerCase();
        if (userDao.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BadRequestException("Email is already used by another user.");
        }

        user.setFullName(requestDto.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setRole(requestDto.getRole());
        user.setStatus(requestDto.getStatus());
        User updatedUser = userDao.save(user);
        return modelMapper.map(updatedUser, UserResponseDto.class);
    }

    @Override
    @Transactional
    public UserResponseDto changeUserRole(Long id, RoleUpdateDto roleUpdateDto) {
        User user = findUserById(id);
        user.setRole(roleUpdateDto.getRole());
        User updatedUser = userDao.save(user);
        return modelMapper.map(updatedUser, UserResponseDto.class);
    }

    @Override
    @Transactional
    public UserResponseDto changeUserStatus(Long id, StatusUpdateDto statusUpdateDto) {
        User user = findUserById(id);
        user.setStatus(statusUpdateDto.getStatus());
        User updatedUser = userDao.save(user);
        return modelMapper.map(updatedUser, UserResponseDto.class);
    }

    private User findUserById(Long id) {
        return userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
