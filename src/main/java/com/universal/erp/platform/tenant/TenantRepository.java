package com.universal.erp.platform.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCodeIgnoreCase(String code);

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findBySlugIgnoreCase(String slug);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsBySlugIgnoreCase(String slug);
}