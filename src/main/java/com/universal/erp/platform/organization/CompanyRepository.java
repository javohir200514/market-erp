package com.universal.erp.platform.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** All queries are automatically restricted to the current tenant by Hibernate's @TenantId. */
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<Company> findByParentCompanyId(UUID parentCompanyId);

    @Query("""
            select c from Company c
            where lower(c.code) like lower(concat('%', :q, '%'))
               or lower(c.displayName) like lower(concat('%', :q, '%'))
            """)
    Page<Company> search(@Param("q") String q, Pageable pageable);
}
