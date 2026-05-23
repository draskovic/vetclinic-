package com.softart.vetclinic.config.security;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
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

        Claims claims;
        UUID userId;
        UUID clinicId;
        String role;
        try {
            claims = jwtService.parseToken(token);
            userId = UUID.fromString(claims.getSubject());
            clinicId = UUID.fromString(claims.get("clinicId", String.class));
            role = claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            // Invalid token (bad signature, expired, malformed/missing claims) → unauthenticated
            log.warn("Invalid JWT from IP {}: {} ({})",
                    getClientIp(request), e.getClass().getSimpleName(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Set tenant context for RLS (before any DB queries in this request)
        try {
            ClinicContextHolder.set(clinicId);

            // Validate X-Clinic-Id header matches token if present
            String clinicIdHeader = request.getHeader("X-Clinic-Id");
            if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
                try {
                    UUID headerClinicId = UUID.fromString(clinicIdHeader);
                    if (!clinicId.equals(headerClinicId)) {
                        log.warn("X-Clinic-Id mismatch from IP {}: userId={}, tokenClinic={}, headerClinic={}",
                                getClientIp(request), userId, clinicId, headerClinicId);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":403,\"message\":\"X-Clinic-Id header does not match authenticated user's clinic\"}");
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Malformed X-Clinic-Id header from IP {}: '{}'",
                            getClientIp(request), clinicIdHeader);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"status\":400,\"message\":\"Invalid X-Clinic-Id header format\"}");
                    return; // finally će očistiti nit
                }
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(
                    new JwtPrincipal(userId, clinicId, claims.get("email", String.class), role),
                    null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } finally {
            ClinicContextHolder.clear();
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
