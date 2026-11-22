package com.universal.erp.platform.tenant;

import com.universal.erp.platform.user.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public input for creating a tenant and its initial administrator. */
public final class TenantRegistrationDto {
    private TenantRegistrationDto() {}

    public record Request(
            @NotBlank @Size(max = 100) String tenantName,
            @NotBlank @Size(max = 63)
            @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message = "Slug must contain lowercase letters, digits or hyphens")
            String slug,
            @NotBlank @Email @Size(max = 254) String adminEmail,
            @NotBlank @Size(max = 100) String adminFirstName,
            @NotBlank @Size(max = 100) String adminLastName,
            @NotBlank @Size(min = 12, max = 128) String adminPassword,
            @Size(max = 10) String locale,
            @Size(max = 64) String timeZone) {}

    public record Response(
            java.util.UUID tenantId,
            String tenantName,
            String slug,
            java.util.UUID adminUserId,
            String adminEmail,
            TenantStatus tenantStatus,
            UserStatus adminStatus) {}
}