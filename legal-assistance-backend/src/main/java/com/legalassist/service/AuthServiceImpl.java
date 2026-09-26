package com.legalassist.service;

import com.legalassist.dto.auth.LoginRequest;
import com.legalassist.dto.auth.RegisterRequest;
import com.legalassist.dto.auth.UserResponse;
import com.legalassist.entity.User;
import com.legalassist.repository.UserRepository;
import com.legalassist.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UUID userId = UUID.randomUUID();
        String publicUserId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setId(userId);
        user.setPublicUserId(publicUserId);
        user.setFullName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        // Auto-authenticate after registration
        setAuthenticatedSession(savedUser, httpRequest, httpResponse);

        return UserResponse.fromUser(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BadCredentialsException("User account is disabled");
        }

        setAuthenticatedSession(user, httpRequest, httpResponse);

        return UserResponse.fromUser(user);
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            throw new BadCredentialsException("Unauthenticated user");
        }

        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            return UserResponse.fromUser(user);
        }

        throw new BadCredentialsException("Unauthenticated user");
    }

    private void setAuthenticatedSession(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken authResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
