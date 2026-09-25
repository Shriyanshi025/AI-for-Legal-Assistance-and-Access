package com.legalassist.service;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User getOrCreateUser(UUID internalId);
    UserProfileResponse getUserProfile(UUID internalId);
    Optional<User> findByPublicUserId(String publicUserId);
}
