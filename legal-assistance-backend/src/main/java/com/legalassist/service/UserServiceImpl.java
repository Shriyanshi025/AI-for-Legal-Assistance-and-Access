package com.legalassist.service;

import com.legalassist.dto.UserProfileResponse;
import com.legalassist.entity.User;
import com.legalassist.repository.UserRepository;
import com.legalassist.util.PublicUserIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User getOrCreateUser(UUID internalId) {
        if (internalId == null) {
            internalId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        Optional<User> existingUser = userRepository.findById(internalId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getPublicUserId() != null && !user.getPublicUserId().isBlank()) {
                return user;
            }
            // Generate public ID for existing user if missing
            String publicId = generateUniquePublicUserId();
            user.setPublicUserId(publicId);
            return userRepository.save(user);
        }

        String publicId = generateUniquePublicUserId();
        User newUser = new User(internalId, publicId, Instant.now());
        User savedUser = userRepository.save(newUser);
        log.info("Created new user entity for internal UUID {} with public ID {}", internalId, publicId);
        return savedUser;
    }

    @Override
    @Transactional
    public UserProfileResponse getUserProfile(UUID internalId) {
        User user = getOrCreateUser(internalId);
        return new UserProfileResponse(user.getId(), user.getPublicUserId());
    }

    @Override
    public Optional<User> findByPublicUserId(String publicUserId) {
        if (publicUserId == null || publicUserId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByPublicUserId(publicUserId.trim().toUpperCase());
    }

    private String generateUniquePublicUserId() {
        int attempts = 0;
        while (attempts < 10) {
            String candidate = PublicUserIdGenerator.generatePublicUserId();
            if (!userRepository.existsByPublicUserId(candidate)) {
                return candidate;
            }
            attempts++;
        }
        throw new IllegalStateException("Failed to generate unique public user ID after multiple attempts");
    }
}
