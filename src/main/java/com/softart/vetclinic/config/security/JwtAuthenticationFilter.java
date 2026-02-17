package com.softart.vetclinic.config.security;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parseToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UUID clinicId = UUID.fromString(claims.get("clinicId", String.class));
        String role = claims.get("role", String.class);

        // Set tenant context for RLS (before any DB queries in this request)
        ClinicContextHolder.set(clinicId);

        // Validate X-Clinic-Id header matches token if present
        String clinicIdHeader = request.getHeader("X-Clinic-Id");
        if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
            try {
                UUID headerClinicId = UUID.fromString(clinicIdHeader);
                if (!clinicId.equals(headerClinicId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"status\":403,\"message\":\"X-Clinic-Id header does not match authenticated user's clinic\"}");
                    return;
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID format in header — let the controller handle it
            }
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(userId, clinicId, claims.get("email", String.class), role),
                null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
