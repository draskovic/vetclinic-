package com.softart.vetclinic.service;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.config.security.CustomUserDetails;
import com.softart.vetclinic.config.security.CustomUserDetailsService;
import com.softart.vetclinic.config.security.JwtService;
import com.softart.vetclinic.dto.AuthResponse;
import com.softart.vetclinic.dto.LoginRequest;
import com.softart.vetclinic.dto.RefreshTokenRequest;
import com.softart.vetclinic.entity.RefreshToken;
import com.softart.vetclinic.entity.User;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.mapper.UserMapper;
import com.softart.vetclinic.repository.RefreshTokenRepository;
import com.softart.vetclinic.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EntityManager entityManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Set tenant context for RLS before any DB queries
        ClinicContextHolder.set(request.clinicId());

        CustomUserDetails userDetails = userDetailsService.loadUserByClinicIdAndEmail(
                request.clinicId(), request.email());

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!userDetails.isEnabled()) {
            throw new BadRequestException("User account is disabled");
        }

        User user = userRepository.findById(userDetails.getUserId()).orElseThrow();

        String accessToken = jwtService.generateAccessToken(
                userDetails.getUserId(),
                userDetails.getClinicId(),
                userDetails.getEmail(),
                userDetails.getRoleName(),
                userDetails.getPermissions());

        String refreshToken = createRefreshToken(userDetails.getUserId());

        // Update lastLoginAt
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                userMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (storedToken.getRevoked()) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BadRequestException("Refresh token has expired");
        }

        // Revoke old token (token rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // For refresh flow: use SECURITY DEFINER function to get user's clinic_id
        // (bypasses RLS since we don't have clinic context yet)
        UUID userId = storedToken.getUserId();
        UUID userClinicId = (UUID) entityManager
                .createNativeQuery("SELECT get_clinic_id_for_user(:uid)")
                .setParameter("uid", userId)
                .getSingleResult();
        ClinicContextHolder.set(userClinicId);

        // SET directly on the CURRENT connection (already acquired by @Transactional)
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (var stmt = connection.createStatement()) {
                stmt.execute("SET app.current_clinic_id = '" + userClinicId + "'");
            }
        });

        User user = userRepository.findById(userId).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtService.generateAccessToken(
                userDetails.getUserId(),
                userDetails.getClinicId(),
                userDetails.getEmail(),
                userDetails.getRoleName(),
                userDetails.getPermissions());

        String newRefreshToken = createRefreshToken(user.getId());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                userMapper.toResponse(user));
    }

    @Transactional
    public void logout(UUID userId, UUID clinicId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String createRefreshToken(UUID userId) {
        String tokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

}
