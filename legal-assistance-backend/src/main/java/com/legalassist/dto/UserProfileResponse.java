package com.legalassist.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String publicUserId,
        String name,
        String email
) {
    public UserProfileResponse(UUID id, String publicUserId) {
        this(id, publicUserId, null, null);
    }
}
