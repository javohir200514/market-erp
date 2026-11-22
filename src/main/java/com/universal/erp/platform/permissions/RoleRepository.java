package com.universal.erp.platform.permissions;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);

    @Query("select r from Role r left join fetch r.permissions "
            + "where r.tenantId = :tenantId and r.code = :code")
    Optional<Role> findWithPermissionsByTenantIdAndCode(@Param("tenantId") UUID tenantId,
                                                        @Param("code") String code);
}
