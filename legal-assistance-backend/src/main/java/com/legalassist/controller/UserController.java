package com.legalassist.controller;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final UUID DEFAULT_DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        UUID targetId = userId != null ? userId : DEFAULT_DEMO_USER_ID;
        UserProfileResponse response = userService.getUserProfile(targetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfileByPath(
            @PathVariable("userId") UUID userId
    ) {
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
}
