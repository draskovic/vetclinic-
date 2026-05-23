package com.softart.vetclinic.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.softart.vetclinic.config.security.CustomUserDetails;
import com.softart.vetclinic.config.security.CustomUserDetailsService;
import com.softart.vetclinic.config.security.JwtService;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
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
    private final TransactionTemplate transactionTemplate;

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

    public AuthResponse refresh(RefreshTokenRequest request) {
        // 1. Load refresh token + basic validation (van clinic context-a — refresh_token nema RLS po klinici)
        RefreshToken storedToken = transactionTemplate.execute(status ->
                refreshTokenRepository.findByToken(request.refreshToken())
                        .orElseThrow(() -> new BadRequestException("Invalid refresh token")));

        if (storedToken.getRevoked()) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Revoke expired token in its own tx (still no clinic context — refresh_token table)
            transactionTemplate.executeWithoutResult(status -> {
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);
            });
            throw new BadRequestException("Refresh token has expired");
        }

        // 2. Resolve user's clinic via SECURITY DEFINER (bypasses RLS, no context needed)
        UUID userId = storedToken.getUserId();
        UUID userClinicId = transactionTemplate.execute(status ->
                (UUID) entityManager
                        .createNativeQuery("SELECT get_clinic_id_for_user(:uid)")
                        .setParameter("uid", userId)
                        .getSingleResult());

        // 3. Set context BEFORE the main tx — TenantAwareDataSource.getConnection() will
        //    automatically issue SET app.current_clinic_id on the next connection acquisition.
        ClinicContextHolder.set(userClinicId);
        try {
            return transactionTemplate.execute(status -> {
                // 4. Revoke old refresh token (token rotation)
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);

                // 5. Load user (with RLS active)
                User user = userRepository.findById(userId).orElseThrow();
                CustomUserDetails userDetails = new CustomUserDetails(user);

                // 6. Generate new tokens
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
            });
        } finally {
            ClinicContextHolder.clear();
        }
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
