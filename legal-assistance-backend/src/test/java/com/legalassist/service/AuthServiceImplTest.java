package com.legalassist.service;

import com.legalassist.dto.auth.LoginRequest;
import com.legalassist.dto.auth.RegisterRequest;
import com.legalassist.dto.auth.UserResponse;
import com.legalassist.entity.User;
import com.legalassist.repository.UserRepository;
import com.legalassist.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("register should hash password, save user, and return UserResponse")
    void register_ShouldHashPassword_AndSaveUser() {
        RegisterRequest registerReq = new RegisterRequest("Counsel Jane", "jane@law.com", "securePass123");

        when(userRepository.existsByEmail("jane@law.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass123")).thenReturn("$2a$10$hashedPasswordSecret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse userResponse = authService.register(registerReq, request, response);

        assertNotNull(userResponse);
        assertNotNull(userResponse.id());
        assertEquals("Counsel Jane", userResponse.name());
        assertEquals("jane@law.com", userResponse.email());

        verify(passwordEncoder).encode("securePass123");
        verify(userRepository).save(any(User.class));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("register should throw IllegalArgumentException when email exists")
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest registerReq = new RegisterRequest("Counsel Jane", "jane@law.com", "securePass123");
        when(userRepository.existsByEmail("jane@law.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerReq, request, response));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login should succeed when email and password match")
    void login_ShouldSucceed_WhenCredentialsMatch() {
        LoginRequest loginReq = new LoginRequest("jane@law.com", "securePass123");
        User user = new User(UUID.randomUUID(), "usr_123", "jane@law.com", "$2a$10$hashedPasswordSecret", "Counsel Jane", true, Instant.now());

        when(userRepository.findByEmail("jane@law.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("securePass123", "$2a$10$hashedPasswordSecret")).thenReturn(true);

        UserResponse responseUser = authService.login(loginReq, request, response);

        assertNotNull(responseUser);
        assertEquals("Counsel Jane", responseUser.name());
        assertEquals("jane@law.com", responseUser.email());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("login should throw BadCredentialsException when password does not match")
    void login_ShouldThrowBadCredentials_WhenPasswordMismatches() {
        LoginRequest loginReq = new LoginRequest("jane@law.com", "wrongPassword");
        User user = new User(UUID.randomUUID(), "usr_123", "jane@law.com", "$2a$10$hashedPasswordSecret", "Counsel Jane", true, Instant.now());

        when(userRepository.findByEmail("jane@law.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPasswordSecret")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq, request, response));
    }

    @Test
    @DisplayName("login should throw BadCredentialsException when user not found")
    void login_ShouldThrowBadCredentials_WhenUserNotFound() {
        LoginRequest loginReq = new LoginRequest("unknown@law.com", "password");
        when(userRepository.findByEmail("unknown@law.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq, request, response));
    }

    @Test
    @DisplayName("logout should clear security context and invalidate session")
    void logout_ShouldClearSecurityContext() {
        request.getSession(true);
        authService.logout(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession(false));
    }

    @Test
    @DisplayName("getCurrentUser should return user response when security context holds valid principal")
    void getCurrentUser_ShouldReturnUser_WhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "usr_123", "jane@law.com", "hashed", "Counsel Jane", true, Instant.now());
        UserPrincipal principal = UserPrincipal.create(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = authService.getCurrentUser();

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Counsel Jane", result.name());
    }
}
