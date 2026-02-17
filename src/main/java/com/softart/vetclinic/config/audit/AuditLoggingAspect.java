package com.softart.vetclinic.config.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.softart.vetclinic.config.security.JwtPrincipal;
import com.softart.vetclinic.entity.BaseEntity;
import com.softart.vetclinic.enums.AuditAction;
import com.softart.vetclinic.service.AbstractCrudService;
import com.softart.vetclinic.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;
    private ObjectMapper auditMapper;

    @PostConstruct
    void initMapper() {
        auditMapper = new ObjectMapper();
        auditMapper.registerModule(new JavaTimeModule());
        auditMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        auditMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        auditMapper.addMixIn(Object.class, AuditSerializationMixin.class);
    }

    /**
     * Mixin koji ignorise sve lazy-loaded relacije i osetljiva polja.
     */
    @JsonIgnoreProperties(value = {
            // Lazy ManyToOne relacije (svi entiteti)
            "clinic", "user", "role", "owner", "pet", "species", "breed",
            "appointment", "medicalRecord", "invoice", "inventoryItem",
            "location", "vet", "service", "followUpAppointment",
            "changedByUser", "uploadedByUser", "performedByUser",
            // Osetljiva polja
            "passwordHash",
            // Hibernate proxy polja
            "hibernateLazyInitializer", "handler"
    }, ignoreUnknown = true)
    abstract static class AuditSerializationMixin {}

    // ========================================================================
    // Pointcuts
    // ========================================================================

    @Pointcut("execution(* com.softart.vetclinic.service.AbstractCrudService+.create(..))")
    public void createOperation() {}

    @Pointcut("execution(* com.softart.vetclinic.service.AbstractCrudService+.update(..))")
    public void updateOperation() {}

    @Pointcut("execution(* com.softart.vetclinic.service.AbstractCrudService+.softDelete(..))")
    public void deleteOperation() {}

    @Pointcut("execution(* com.softart.vetclinic.service.AuthService.login(..))")
    public void loginOperation() {}

    @Pointcut("execution(* com.softart.vetclinic.service.AuthService.logout(..))")
    public void logoutOperation() {}

    // ========================================================================
    // Advice
    // ========================================================================

    /**
     * Posle uspesnog create-a: loguje new values.
     */
    @AfterReturning(pointcut = "createOperation()", returning = "result")
    public void afterCreate(JoinPoint joinPoint, Object result) {
        try {
            if (!(result instanceof BaseEntity entity)) return;

            String entityType = resolveEntityType(joinPoint);
            String newValues = serialize(entity);
            RequestInfo req = extractRequestInfo();
            UUID userId = extractUserId();

            auditLogService.logAsync(entity.getClinicId(), userId, AuditAction.CREATE,
                    entityType, entity.getId(), null, newValues,
                    req.ipAddress(), req.userAgent());
        } catch (Exception e) {
            log.error("Audit logging failed for CREATE", e);
        }
    }

    /**
     * Oko update-a: hvata old state pre mutacije, new state posle.
     * AbstractCrudService.update() radi findById → updater.accept(existing) → save(existing)
     * na ISTOM objektu. Moramo serijalizovati BEFORE proceed().
     */
    @Around("updateOperation()")
    public Object aroundUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        String oldValues = null;
        UUID entityId = null;
        UUID clinicId = null;

        try {
            Object[] args = joinPoint.getArgs();
            entityId = (UUID) args[0];
            clinicId = (UUID) args[1];

            // Ucitaj entitet BEFORE mutacije i serijalizuj
            @SuppressWarnings("unchecked")
            AbstractCrudService<BaseEntity, ?> targetService =
                    (AbstractCrudService<BaseEntity, ?>) joinPoint.getTarget();
            BaseEntity beforeEntity = targetService.findById(entityId, clinicId);
            oldValues = serialize(beforeEntity);
        } catch (Exception e) {
            log.warn("Failed to capture pre-update state for audit", e);
        }

        Object result = joinPoint.proceed();

        try {
            if (result instanceof BaseEntity entity) {
                String entityType = resolveEntityType(joinPoint);
                String newValues = serialize(entity);
                RequestInfo req = extractRequestInfo();
                UUID userId = extractUserId();

                auditLogService.logAsync(
                        clinicId != null ? clinicId : entity.getClinicId(),
                        userId, AuditAction.UPDATE,
                        entityType, entityId, oldValues, newValues,
                        req.ipAddress(), req.userAgent());
            }
        } catch (Exception e) {
            log.error("Audit logging failed for UPDATE", e);
        }

        return result;
    }

    /**
     * Oko softDelete: hvata entity state pre brisanja.
     */
    @Around("deleteOperation()")
    public Object aroundDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        String oldValues = null;
        UUID entityId = null;
        UUID clinicId = null;
        String entityType = null;

        try {
            Object[] args = joinPoint.getArgs();
            entityId = (UUID) args[0];
            clinicId = (UUID) args[1];
            entityType = resolveEntityType(joinPoint);

            @SuppressWarnings("unchecked")
            AbstractCrudService<BaseEntity, ?> targetService =
                    (AbstractCrudService<BaseEntity, ?>) joinPoint.getTarget();
            BaseEntity entity = targetService.findById(entityId, clinicId);
            oldValues = serialize(entity);
        } catch (Exception e) {
            log.warn("Failed to capture pre-delete state for audit", e);
        }

        Object result = joinPoint.proceed();

        try {
            RequestInfo req = extractRequestInfo();
            UUID userId = extractUserId();

            auditLogService.logAsync(clinicId, userId, AuditAction.DELETE,
                    entityType, entityId, oldValues, null,
                    req.ipAddress(), req.userAgent());
        } catch (Exception e) {
            log.error("Audit logging failed for DELETE", e);
        }

        return result;
    }

    /**
     * Posle uspesnog login-a: loguje LOGIN akciju sa email-om korisnika.
     * AuthService.login(LoginRequest request) vraca AuthResponse.
     */
    @AfterReturning(pointcut = "loginOperation()", returning = "result")
    public void afterLogin(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            // login(LoginRequest request) — LoginRequest ima clinicId() i email()
            var loginRequest = (com.softart.vetclinic.dto.LoginRequest) args[0];
            UUID clinicId = loginRequest.clinicId();
            String email = loginRequest.email();

            // AuthResponse sadrzi user info — izvuci userId
            UUID userId = null;
            if (result instanceof com.softart.vetclinic.dto.AuthResponse authResponse
                    && authResponse.user() != null) {
                userId = authResponse.user().id();
            }

            RequestInfo req = extractRequestInfo();
            String details = "{\"email\":\"" + email + "\"}";

            auditLogService.logAsync(clinicId, userId, AuditAction.LOGIN,
                    "Auth", userId, null, details,
                    req.ipAddress(), req.userAgent());
        } catch (Exception e) {
            log.error("Audit logging failed for LOGIN", e);
        }
    }

    /**
     * Posle uspesnog logout-a: loguje LOGOUT akciju.
     * AuthService.logout(UUID userId, UUID clinicId)
     */
    @AfterReturning("logoutOperation()")
    public void afterLogout(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            UUID userId = (UUID) args[0];
            UUID clinicId = (UUID) args[1];

            RequestInfo req = extractRequestInfo();

            auditLogService.logAsync(clinicId, userId, AuditAction.LOGOUT,
                    "Auth", userId, null, null,
                    req.ipAddress(), req.userAgent());
        } catch (Exception e) {
            log.error("Audit logging failed for LOGOUT", e);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private String resolveEntityType(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        className = className.replaceAll("\\$\\$.*", "");
        return className.replace("Service", "");
    }

    private String serialize(Object entity) {
        try {
            return auditMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.warn("Failed to serialize entity for audit: {}", e.getMessage());
            return null;
        }
    }

    private UUID extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                return principal.userId();
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from SecurityContext", e);
        }
        return null;
    }

    private RequestInfo extractRequestInfo() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                }
                String userAgent = request.getHeader("User-Agent");
                return new RequestInfo(ip, userAgent);
            }
        } catch (Exception e) {
            log.warn("Could not extract request info for audit", e);
        }
        return new RequestInfo(null, null);
    }

    private record RequestInfo(String ipAddress, String userAgent) {}
}
