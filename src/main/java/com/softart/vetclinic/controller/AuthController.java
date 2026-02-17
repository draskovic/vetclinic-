package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.security.JwtPrincipal;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.dto.AuthResponse;
import com.softart.vetclinic.dto.LoginRequest;
import com.softart.vetclinic.dto.RefreshTokenRequest;
import com.softart.vetclinic.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Set RLS tenant context BEFORE @Transactional opens a connection
        ClinicContextHolder.set(request.clinicId());
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        authService.logout(principal.userId(), principal.clinicId());
    }
}
