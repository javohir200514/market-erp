package com.universal.erp.platform.permissions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("""
            select ur from UserRole ur
            join fetch ur.role r
            left join fetch ur.company
            left join fetch ur.branch
            where ur.user.id = :userId
            """)
    List<UserRole> findAllByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    boolean existsByRoleId(UUID roleId);
}
