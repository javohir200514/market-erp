package com.universal.erp.platform.security;

import com.universal.erp.platform.permissions.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Read-only queries used to build the principal. Tenant filtering comes from @TenantId on UserRole. */
public interface AuthorizationRepository extends Repository<UserRole, UUID> {

    @Query("""
            select distinct r.code from UserRole ur join ur.role r
            where ur.user.id = :userId and ur.company is null and ur.branch is null
            """)
    List<String> findTenantWideRoleCodes(@Param("userId") UUID userId);

    @Query("""
            select distinct p.code from UserRole ur join ur.role r join r.permissions p
            where ur.user.id = :userId and ur.company is null and ur.branch is null
            """)
    List<String> findTenantWidePermissionCodes(@Param("userId") UUID userId);

    @Query("""
            select new com.universal.erp.platform.security.ScopedPermission(p.code, c.id, b.id)
            from UserRole ur join ur.role r join r.permissions p
            left join ur.company c left join ur.branch b
            where ur.user.id = :userId and (ur.company is not null or ur.branch is not null)
            """)
    List<ScopedPermission> findScopedPermissions(@Param("userId") UUID userId);
}