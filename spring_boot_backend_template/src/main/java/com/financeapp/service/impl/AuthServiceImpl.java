package com.financeapp.service.impl;

import com.financeapp.dao.UserDao;
import com.financeapp.dto.request.LoginRequestDto;
import com.financeapp.dto.request.RegisterRequestDto;
import com.financeapp.dto.response.AuthResponseDto;
import com.financeapp.dto.response.UserResponseDto;
import com.financeapp.entities.User;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import com.financeapp.exception.BadRequestException;
import com.financeapp.exception.InactiveUserException;
import com.financeapp.security.JwtUtil;
import com.financeapp.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto requestDto) {
        String email = requestDto.getEmail().trim().toLowerCase();
        if (userDao.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered.");
        }

        User user = User.builder()
                .fullName(requestDto.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userDao.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole().name())
                .build();
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .user(modelMapper.map(savedUser, UserResponseDto.class))
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto requestDto) {
        String email = requestDto.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, requestDto.getPassword()));
        } catch (DisabledException ex) {
            throw new InactiveUserException("Inactive user cannot log in.");
        }

        User user = userDao.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InactiveUserException("Inactive user cannot log in.");
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .user(modelMapper.map(user, UserResponseDto.class))
                .build();
    }
}
