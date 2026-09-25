package com.legalassist.service;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.entity.User;
import com.legalassist.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    @DisplayName("getOrCreateUser creates new user with public ID when user does not exist")
    void testGetOrCreateUser_NewUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByPublicUserId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.getOrCreateUser(userId);

        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertNotNull(user.getPublicUserId());
        assertTrue(user.getPublicUserId().startsWith("USR-"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("getOrCreateUser returns existing user when user already exists")
    void testGetOrCreateUser_ExistingUser() {
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "USR-TEST12", Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        User user = userService.getOrCreateUser(userId);

        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals("USR-TEST12", user.getPublicUserId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getUserProfile returns UserProfileResponse with internal UUID and public ID")
    void testGetUserProfile() {
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "USR-ABC123", Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        UserProfileResponse response = userService.getUserProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("USR-ABC123", response.publicUserId());
    }
}
