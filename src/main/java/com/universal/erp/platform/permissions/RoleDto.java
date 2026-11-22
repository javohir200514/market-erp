package com.universal.erp.platform.permissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RoleDto {

    private RoleDto() {
    }

    public record Create(
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 300) String description,
            Set<String> permissionCodes) {
    }

    public record Update(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 300) String description,
            Set<String> permissionCodes,
            long version) {
    }

    public record PermissionResponse(UUID id, String code, String module, PermissionAction action, String description) {

        public static PermissionResponse from(Permission p) {
            return new PermissionResponse(p.getId(), p.getCode(), p.getModule(), p.getAction(), p.getDescription());
        }
    }

    public record Response(
            UUID id, String code, String name, String description, boolean systemRole,
            Set<String> permissionCodes, long version) {

        public static Response from(Role r) {
            return new Response(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSystemRole(),
                    r.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()),
                    r.getVersion());
        }
    }
}
