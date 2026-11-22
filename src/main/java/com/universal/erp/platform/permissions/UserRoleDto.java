package com.universal.erp.platform.permissions;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class UserRoleDto {
    private UserRoleDto() {}

    public record Grant(@NotNull UUID roleId, UUID companyId, UUID branchId) {}

    public record Response(UUID id, UUID userId, UUID roleId, String roleCode,
                           UUID companyId, UUID branchId) {
        public static Response from(UserRole ur) {
            return new Response(ur.getId(), ur.getUser().getId(), ur.getRole().getId(),
                    ur.getRole().getCode(),
                    ur.getCompany() == null ? null : ur.getCompany().getId(),
                    ur.getBranch() == null ? null : ur.getBranch().getId());
        }
    }
}