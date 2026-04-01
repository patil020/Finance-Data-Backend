package com.financeapp.service;

import com.financeapp.dto.request.RoleUpdateDto;
import com.financeapp.dto.request.StatusUpdateDto;
import com.financeapp.dto.request.UserRequestDto;
import com.financeapp.dto.response.PagedResponseDto;
import com.financeapp.dto.response.UserResponseDto;

public interface UserService {

    UserResponseDto createUser(UserRequestDto requestDto);

    PagedResponseDto<UserResponseDto> getAllUsers(int page, int size, String sortBy, String sortDir);

    UserResponseDto getUserById(Long id);

    UserResponseDto updateUser(Long id, UserRequestDto requestDto);

    UserResponseDto changeUserRole(Long id, RoleUpdateDto roleUpdateDto);

    UserResponseDto changeUserStatus(Long id, StatusUpdateDto statusUpdateDto);
}
