package com.legalassist.service;

import com.legalassist.dto.auth.LoginRequest;
import com.legalassist.dto.auth.RegisterRequest;
import com.legalassist.dto.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    UserResponse getCurrentUser();
}
