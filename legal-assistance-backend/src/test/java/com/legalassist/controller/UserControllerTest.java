package com.legalassist.controller;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.entity.User;
import com.legalassist.security.UserPrincipal;
import com.legalassist.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UUID setupMockAuth() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setPublicUserId("USR-K7M4Q9");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_pass");
        user.setEnabled(true);

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        return userId;
    }

    @Test
    @DisplayName("GET /api/users/profile returns authenticated user profile")
    void testGetUserProfile() throws Exception {
        UUID authenticatedUserId = setupMockAuth();
        UserProfileResponse profile = new UserProfileResponse(authenticatedUserId, "USR-K7M4Q9");
        when(userService.getUserProfile(authenticatedUserId)).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(authenticatedUserId.toString()))
                .andExpect(jsonPath("$.publicUserId").value("USR-K7M4Q9"));
    }

    @Test
    @DisplayName("GET /api/users/profile with unauthenticated principal throws BadCredentialsException")
    void testGetUserProfileUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/profile ignores query userId parameter and uses authenticated identity")
    void testGetUserProfileIgnoresQueryUserId() throws Exception {
        UUID authenticatedUserId = setupMockAuth();
        UUID attackerUserId = UUID.randomUUID();

        UserProfileResponse profile = new UserProfileResponse(authenticatedUserId, "USR-K7M4Q9");
        when(userService.getUserProfile(authenticatedUserId)).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile").param("userId", attackerUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authenticatedUserId.toString()));
    }
}
