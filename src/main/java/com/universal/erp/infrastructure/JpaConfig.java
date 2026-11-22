package com.universal.erp.infrastructure;

import com.universal.erp.platform.tenant.TenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * created_by / updated_by: the e-mail of the logged-in user, otherwise "system"
     * (login bookkeeping, registration, scheduled jobs). The Spring "anonymousUser" is NOT a real actor.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName)
                .or(() -> Optional.of("system"));
    }

    @Bean
    public HibernatePropertiesCustomizer tenantResolverCustomizer(TenantIdentifierResolver resolver) {
        return properties -> properties.put("hibernate.tenant_identifier_resolver", resolver);
    }
}