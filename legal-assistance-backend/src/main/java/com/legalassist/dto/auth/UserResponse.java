package com.legalassist.dto.auth;

import com.legalassist.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String publicUserId,
        String name,
        String email,
        Instant createdAt
) {
    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getPublicUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
