package com.universal.erp.platform.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal stored in the HTTP session.
 * Authorities = tenant-wide permission codes + ROLE_{code}.
 * Company/branch-scoped grants are kept separately and are NOT tenant-wide authorities.
 */
public final class ErpUserDetails implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String email;
    private final String fullName;
    private final String locale;
    private final String timeZone;
    private String passwordHash;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;
    private final List<ScopedPermission> scopedPermissions;
    private final Set<GrantedAuthority> authorities;

    public ErpUserDetails(UUID userId, UUID tenantId, String tenantSlug, String email, String fullName,
                          String passwordHash, String locale, String timeZone,
                          Set<String> roleCodes, Set<String> permissionCodes,
                          List<ScopedPermission> scopedPermissions) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.locale = locale;
        this.timeZone = timeZone;
        this.roleCodes = Set.copyOf(roleCodes);
        this.permissionCodes = Set.copyOf(permissionCodes);
        this.scopedPermissions = List.copyOf(scopedPermissions);

        Set<GrantedAuthority> granted = new LinkedHashSet<>();
        permissionCodes.forEach(code -> granted.add(new SimpleGrantedAuthority(code)));
        roleCodes.forEach(code -> granted.add(new SimpleGrantedAuthority("ROLE_" + code)));
        this.authorities = Collections.unmodifiableSet(granted);
    }

    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getTenantSlug() { return tenantSlug; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getLocale() { return locale; }
    public String getTimeZone() { return timeZone; }
    public Set<String> getRoleCodes() { return roleCodes; }
    public Set<String> getPermissionCodes() { return permissionCodes; }
    public List<ScopedPermission> getScopedPermissions() { return scopedPermissions; }

    public boolean hasTenantWidePermission(String code) {
        return permissionCodes.contains(code);
    }

    /** Tenant-wide grant, or a grant scoped to this company/branch. Used by module data filters later. */
    public boolean hasPermission(String code, UUID companyId, UUID branchId) {
        if (permissionCodes.contains(code)) return true;
        for (ScopedPermission sp : scopedPermissions) {
            if (!sp.permissionCode().equals(code)) continue;
            if (sp.branchId() != null) {
                if (sp.branchId().equals(branchId)) return true;
            } else if (sp.companyId() != null && sp.companyId().equals(companyId)) {
                return true;
            }
        }
        return false;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public void eraseCredentials() { this.passwordHash = null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErpUserDetails other)) return false;
        return userId.equals(other.userId) && tenantId.equals(other.tenantId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, tenantId); }

    @Override
    public String toString() { return "ErpUserDetails[" + email + "@" + tenantSlug + "]"; }
}