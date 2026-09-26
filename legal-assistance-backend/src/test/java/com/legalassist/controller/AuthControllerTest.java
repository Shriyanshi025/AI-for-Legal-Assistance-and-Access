package com.legalassist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalassist.dto.auth.LoginRequest;
import com.legalassist.dto.auth.RegisterRequest;
import com.legalassist.dto.auth.UserResponse;
import com.legalassist.exception.GlobalExceptionHandler;
import com.legalassist.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register should create user and return 201 Created")
    void register_ShouldCreateUser_AndReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "usr_123456", "Jane Doe", "jane@example.com", Instant.now());

        when(authService.register(any(), any(), any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when email already exists")
    void register_ShouldReturn400_WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");

        when(authService.register(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK on valid credentials")
    void login_ShouldReturn200_WhenCredentialsValid() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "password123");
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "usr_123456", "Jane Doe", "jane@example.com", Instant.now());

        when(authService.login(any(), any(), any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 Unauthorized on invalid credentials")
    void login_ShouldReturn401_WhenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "wrongpassword");

        when(authService.login(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/logout should return 200 OK")
    void logout_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/auth/me should return current user when authenticated")
    void me_ShouldReturnUser_WhenAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "usr_123456", "Jane Doe", "jane@example.com", Instant.now());

        when(authService.getCurrentUser()).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    @DisplayName("GET /api/auth/me should return 401 Unauthorized when unauthenticated")
    void me_ShouldReturn401_WhenUnauthenticated() throws Exception {
        when(authService.getCurrentUser())
                .thenThrow(new BadCredentialsException("Unauthenticated user"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated user"));
    }
}
