package com.universal.erp.platform.tenant;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;
import java.util.UUID;


@Service
@Validated
public class TenantRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(TenantRegistrationService.class);

    private final TenantRegistrationWorker worker;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;

    public TenantRegistrationService(TenantRegistrationWorker worker,
                                     PasswordEncoder passwordEncoder,
                                     TenantRepository tenantRepository) {
        this.worker = worker;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
    }

    public TenantRegistrationDto.Response register(@Valid TenantRegistrationDto.Request request) {
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        String email = request.adminEmail().trim().toLowerCase(Locale.ROOT);

        if (tenantRepository.existsBySlugIgnoreCase(slug)) {
            throw new TenantAlreadyExistsException("Tenant slug is already registered");
        }

        // BCrypt is slow: hash outside of any transaction.
        String passwordHash = passwordEncoder.encode(request.adminPassword());

        UUID tenantId = worker.createPendingTenant(request, slug, email);
        try {
            return TenantContext.callAs(tenantId,
                    () -> worker.provisionTenant(tenantId, request, email, passwordHash));
        } catch (RuntimeException ex) {
            discardQuietly(tenantId);
            throw ex;
        }
    }

    private void discardQuietly(UUID tenantId) {
        try {
            worker.discardPendingTenant(tenantId);
        } catch (RuntimeException cleanupFailure) {
            log.error("Could not remove half-registered tenant {}; remove it manually", tenantId, cleanupFailure);
        }
    }
}