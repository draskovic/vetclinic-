package com.softart.vetclinic.config.tenant;

import com.softart.vetclinic.config.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that populates ClinicContextHolder
 * from the authenticated JWT principal before controller execution,
 * and clears it after request completion.
 */
@Slf4j
@Component
public class TenantConnectionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
            ClinicContextHolder.set(principal.clinicId());
            log.trace("TenantInterceptor: set clinicId={}", principal.clinicId());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ClinicContextHolder.clear();
        log.trace("TenantInterceptor: cleared clinicId");
    }
}
