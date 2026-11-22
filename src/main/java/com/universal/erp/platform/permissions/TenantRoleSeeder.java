package com.universal.erp.platform.permissions;

import com.universal.erp.platform.tenant.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Creates or synchronizes the default system roles of a tenant.
 *
 * Idempotent: safe to call for a brand-new tenant (registration) and for existing tenants
 * (SystemRoleSynchronizer on start-up). A missing role is created; an existing role only
 * receives permissions it does not have yet (nothing is ever revoked here).
 *
 * MUST be called while TenantContext is bound to the target tenant. Runs in one transaction,
 * so a tenant is either fully synchronized or left untouched.
 */
@Component
public class TenantRoleSeeder {
    public static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public TenantRoleSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /** @return the TENANT_ADMIN role */
    @Transactional
    public Role seedDefaultRoles() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Permission> catalog = permissionRepository.findAll();

        Role admin = ensure(tenantId, TENANT_ADMIN, "Tenant Administrator",
                "Full access to every tenant feature", catalog, p -> true);

        ensure(tenantId, "ORG_MANAGER", "Organization Manager",
                "Manage companies, branches and departments", catalog,
                p -> p.getCode().matches("platform\\.(company|branch|department):(read|create|update)"));

        ensure(tenantId, "USER_MANAGER", "User Manager",
                "Manage users and view roles", catalog,
                p -> p.getCode().matches("platform\\.user:(read|create|update|delete)")
                        || p.getCode().equals("platform.role:read"));

        ensure(tenantId, "VIEWER", "Viewer",
                "Read-only access (no audit logs)", catalog,
                p -> p.getAction() == PermissionAction.READ && !p.getCode().equals("platform.audit:read"));

        return admin;
    }

    private Role ensure(UUID tenantId, String code, String name, String description,
                        List<Permission> catalog, Predicate<Permission> filter) {
        Set<Permission> wanted = catalog.stream().filter(filter).collect(Collectors.toSet());

        return roleRepository.findWithPermissionsByTenantIdAndCode(tenantId, code)
                .map(existing -> {
                    // managed entity: dirty checking persists the added permissions on commit
                    existing.getPermissions().addAll(wanted);
                    return existing;
                })
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(code);
                    role.setName(name);
                    role.setDescription(description);
                    role.setSystemRole(true);
                    role.setPermissions(new HashSet<>(wanted));
                    return roleRepository.save(role);
                });
    }
}