package com.legalassist.controller;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users/profile returns public user profile")
    void testGetUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profile = new UserProfileResponse(userId, "USR-K7M4Q9");
        when(userService.getUserProfile(any(UUID.class))).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.publicUserId").value("USR-K7M4Q9"));
    }
}
