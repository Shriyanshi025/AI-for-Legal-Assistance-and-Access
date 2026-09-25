package com.legalassist.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String publicUserId
) {
}
