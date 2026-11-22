package com.universal.erp.platform.user.dto;

import com.universal.erp.platform.user.enums.UserStatus;
import com.universal.erp.platform.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class UserDto {

    private UserDto() {
    }

    public record Invite(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @NotEmpty Set<@Valid RoleAssignment> roles) {
    }

    public record RoleAssignment(@NotNull UUID roleId, UUID companyId, UUID branchId) {
    }

    public record UpdateProfile(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @NotBlank @Size(max = 10) String locale,
            @NotBlank @Size(max = 50) String timeZone,
            long version) {
    }

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 128) String newPassword) {
    }

    /** passwordHash is deliberately absent. */
    public record Response(
            UUID id, String email, String firstName, String lastName, String phone,
            String locale, String timeZone, UserStatus status, Instant lastLoginAt, long version) {

        public static Response from(User u) {
            return new Response(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                    u.getLocale(), u.getTimeZone(), u.getStatus(), u.getLastLoginAt(), u.getVersion());
        }
    }
}