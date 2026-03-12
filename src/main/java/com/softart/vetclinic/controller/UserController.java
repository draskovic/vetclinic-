package com.softart.vetclinic.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.softart.vetclinic.config.security.JwtPrincipal;
import com.softart.vetclinic.dto.ChangePasswordRequest;
import com.softart.vetclinic.dto.CreateUserRequest;
import com.softart.vetclinic.dto.UpdateProfileRequest;
import com.softart.vetclinic.dto.UpdateUserRequest;
import com.softart.vetclinic.dto.UserResponse;
import com.softart.vetclinic.mapper.UserMapper;
import com.softart.vetclinic.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Page<UserResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return userService.findAll(clinicId, pageable).map(userMapper::toResponse);
    }

    @GetMapping("/{id}")
    public UserResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return userMapper.toResponse(userService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateUserRequest request) {
        var entity = userMapper.toEntity(request);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userMapper.toResponse(
                userService.update(id, clinicId, existing -> userMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        userService.softDelete(id, clinicId);
    }
    
    @GetMapping("/me")
    public UserResponse getMe(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return userMapper.toResponse(userService.findById(principal.userId(), principal.clinicId()));
    }
    
    @PutMapping("/me")
    public UserResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        userService.updateProfile(principal.userId(), principal.clinicId(), request);
        return userMapper.toResponse(
                userService.findById(principal.userId(), principal.clinicId()));
    }


    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        userService.changePassword(principal.userId(), principal.clinicId(),
                request.currentPassword(), request.newPassword(), passwordEncoder);
    }
}
