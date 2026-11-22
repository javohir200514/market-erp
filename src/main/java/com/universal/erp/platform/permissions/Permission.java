package com.universal.erp.platform.permissions;

import com.universal.erp.shared.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Global catalog of permissions, seeded by migrations/modules (e.g. code "inventory.product:create").
 * Not tenant-owned: tenants only decide which roles hold which permissions.
 * Distinct from subscription entitlements, which decide whether a module is available at all.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    /** Format: {module}.{resource}:{action}. */
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionAction action;

    @Column(length = 300)
    private String description;
}
