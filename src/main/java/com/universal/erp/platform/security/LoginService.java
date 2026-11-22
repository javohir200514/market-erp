package com.universal.erp.platform.security;

import com.universal.erp.platform.audit.AuditAction;
import com.universal.erp.platform.audit.AuditLogService;
import com.universal.erp.platform.tenant.Tenant;
import com.universal.erp.platform.tenant.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/** Entry point used by the Vaadin LoginView (and later by anything else that logs users in). */
@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final TenantRepository tenantRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public LoginService(AuthenticationManager authenticationManager, AuditLogService auditLogService,
                        TenantRepository tenantRepository) {
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
        this.tenantRepository = tenantRepository;
    }

    public ErpUserDetails login(String tenantSlug, String email, String password,
                                HttpServletRequest request, HttpServletResponse response) {
        String ip = request.getRemoteAddr();

        Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(
                    new TenantUsernamePasswordAuthenticationToken(tenantSlug, email, password));
        } catch (AuthenticationException ex) {
            auditFailure(tenantSlug, email, ip);
            throw ex;
        }

        // Session fixation protection: new session id after successful authentication
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = holderStrategy.createEmptyContext();
        context.setAuthentication(authenticated);
        holderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        ErpUserDetails user = (ErpUserDetails) authenticated.getPrincipal();
        safeAudit(AuditAction.LOGIN, user.getTenantId(), user.getUserId(), user.getEmail(),
                user.getUserId().toString(), ip);
        return user;
    }

    private void auditFailure(String tenantSlug, String email, String ip) {
        UUID tenantId = null;
        if (tenantSlug != null && !tenantSlug.isBlank()) {
            tenantId = tenantRepository.findBySlug(tenantSlug.trim().toLowerCase(Locale.ROOT))
                    .map(Tenant::getId).orElse(null);
        }
        safeAudit(AuditAction.LOGIN_FAILURE, tenantId, null, email, null, ip);
    }

    private void safeAudit(AuditAction action, UUID tenantId, UUID userId, String label, String entityId, String ip) {
        try {
            auditLogService.record(action, tenantId, userId, label, "User", entityId, ip);
        } catch (RuntimeException e) {
            log.warn("Audit write failed for {}: {}", action, e.getMessage());
        }
    }
}