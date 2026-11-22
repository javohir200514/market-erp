package com.universal.erp.platform.security;

import com.universal.erp.platform.tenant.Tenant;
import com.universal.erp.platform.tenant.TenantContext;
import com.universal.erp.platform.tenant.TenantRepository;
import com.universal.erp.platform.user.enums.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * tenant slug + email + password authentication.
 * The tenant comes from the slug, the user is looked up only inside that tenant.
 * Error messages are generic to avoid revealing which part was wrong.
 */
@Component
public class TenantAuthenticationProvider implements AuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(TenantAuthenticationProvider.class);

    private final TenantRepository tenantRepository;
    private final AuthenticatedUserLoader loader;
    private final LoginAttemptService attempts;
    private final PasswordEncoder passwordEncoder;
    /** Used to spend the same time when the tenant/user does not exist. */
    private final String dummyHash;

    public TenantAuthenticationProvider(TenantRepository tenantRepository, AuthenticatedUserLoader loader,
                                        LoginAttemptService attempts, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.loader = loader;
        this.attempts = attempts;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-" + UUID.randomUUID());
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TenantUsernamePasswordAuthenticationToken request = (TenantUsernamePasswordAuthenticationToken) authentication;

        String slug = normalize(request.getTenantSlug());
        String email = normalize(request.getName());
        String rawPassword = request.getCredentials() == null ? "" : request.getCredentials().toString();

        if (slug.isEmpty() || email.isEmpty() || rawPassword.isEmpty()) {
            throw badCredentials(rawPassword);
        }

        Tenant tenant = tenantRepository.findBySlug(slug).orElse(null);
        if (tenant == null) {
            throw badCredentials(rawPassword);
        }

        UUID previous = TenantContext.getTenantId();
        TenantContext.setTenantId(tenant.getId());
        try {
            Authentication result = authenticateInTenant(tenant, email, rawPassword);
            if (result instanceof UsernamePasswordAuthenticationToken token) {
                token.setDetails(request.getDetails());
            }
            return result;
        } finally {
            if (previous != null) {
                TenantContext.setTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    private Authentication authenticateInTenant(Tenant tenant, String email, String rawPassword) {
        AuthenticatedUserLoader.Credentials creds = loader.findCredentials(email).orElse(null);
        if (creds == null || creds.passwordHash() == null) {
            throw badCredentials(rawPassword);
        }

        if (creds.lockedUntil() != null && creds.lockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account is temporarily locked");
        }

        if (!passwordEncoder.matches(rawPassword, creds.passwordHash())) {
            safely(() -> attempts.recordFailure(creds.userId()));
            throw new BadCredentialsException("Invalid credentials");
        }

        // Status is checked only after the password is proven correct.
        if (creds.status() != UserStatus.ACTIVE || !tenant.isActive()) {
            throw new DisabledException("Account or tenant is not active");
        }

        safely(() -> attempts.recordSuccess(creds.userId()));

        AuthenticatedUserLoader.Access access = loader.loadAccess(creds.userId());
        ErpUserDetails principal = new ErpUserDetails(
                creds.userId(), tenant.getId(), tenant.getSlug(), creds.email(), creds.fullName(),
                creds.passwordHash(), creds.locale(), creds.timeZone(),
                access.roleCodes(), access.permissionCodes(), access.scoped());

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private BadCredentialsException badCredentials(String rawPassword) {
        passwordEncoder.matches(rawPassword == null ? "" : rawPassword, dummyHash);
        return new BadCredentialsException("Invalid credentials");
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            // e.g. optimistic-lock clash between concurrent attempts: must not break login flow
            log.warn("Login attempt bookkeeping failed: {}", e.getMessage());
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TenantUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}