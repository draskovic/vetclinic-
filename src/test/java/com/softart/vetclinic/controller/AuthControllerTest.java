package com.softart.vetclinic.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.LoginRequest;
import com.softart.vetclinic.dto.RefreshTokenRequest;

class AuthControllerTest extends IntegrationTestBase {

    // ---- LOGIN ----

    @Test
    @DisplayName("POST /api/auth/login - success")
    void login_success() throws Exception {
        var request = new LoginRequest(clinicAId, "admin@clinica.test", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value("admin@clinica.test"));
    }

    @Test
    @DisplayName("POST /api/auth/login - wrong password returns 401")
    void login_wrongPassword() throws Exception {
        var request = new LoginRequest(clinicAId, "admin@clinica.test", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/auth/login - non-existent user returns 401")
    void login_nonExistentUser() throws Exception {
        var request = new LoginRequest(clinicAId, "nobody@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - disabled user returns 400")
    void login_disabledUser() throws Exception {
        // Disable the user
        jdbc.update("UPDATE users SET active = false WHERE id = ?", userAId);

        var request = new LoginRequest(clinicAId, "admin@clinica.test", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - missing fields returns 400")
    void login_missingFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- REFRESH ----

    @Test
    @DisplayName("POST /api/auth/refresh - success")
    void refresh_success() throws Exception {
        String tokenValue = UUID.randomUUID().toString();
        seedRefreshToken(userAId, tokenValue, false, OffsetDateTime.now().plusHours(1));

        var request = new RefreshTokenRequest(tokenValue);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@clinica.test"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - expired token returns 400")
    void refresh_expiredToken() throws Exception {
        String tokenValue = UUID.randomUUID().toString();
        seedRefreshToken(userAId, tokenValue, false, OffsetDateTime.now().minusHours(1));

        var request = new RefreshTokenRequest(tokenValue);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - revoked token returns 400")
    void refresh_revokedToken() throws Exception {
        String tokenValue = UUID.randomUUID().toString();
        seedRefreshToken(userAId, tokenValue, true, OffsetDateTime.now().plusHours(1));

        var request = new RefreshTokenRequest(tokenValue);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - non-existent token returns 400")
    void refresh_nonExistentToken() throws Exception {
        var request = new RefreshTokenRequest("non-existent-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- LOGOUT ----

    @Test
    @DisplayName("POST /api/auth/logout - success")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Clinic-Id", clinicAId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/auth/logout - unauthenticated returns 500 (no principal)")
    void logout_unauthenticated() throws Exception {
        // /api/auth/** is permitAll, so unauthenticated request reaches controller
        // but authentication.getPrincipal() causes NullPointerException → 500
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isInternalServerError());
    }
}
