package com.legalassist.controller;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.security.UserPrincipal;
import com.legalassist.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BadCredentialsException("Unauthenticated user");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        UUID targetId = resolveUserId();
        UserProfileResponse response = userService.getUserProfile(targetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfileByPath(
            @PathVariable("userId") UUID userId
    ) {
        UUID targetId = resolveUserId();
        UserProfileResponse response = userService.getUserProfile(targetId);
        return ResponseEntity.ok(response);
    }
}
