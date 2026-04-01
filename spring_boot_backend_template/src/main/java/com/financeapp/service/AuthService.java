package com.financeapp.service;

import com.financeapp.dto.request.LoginRequestDto;
import com.financeapp.dto.request.RegisterRequestDto;
import com.financeapp.dto.response.AuthResponseDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto requestDto);

    AuthResponseDto login(LoginRequestDto requestDto);
}
