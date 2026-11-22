package com.universal.erp.platform.security;

import com.universal.erp.platform.user.repository.UserRepository;
import com.universal.erp.platform.user.enums.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Loads login data. TenantContext MUST already be bound to the tenant before calling
 * (Hibernate resolves the tenant when the Session opens, i.e. when the transaction starts).
 */
@Service
public class AuthenticatedUserLoader {

    public record Credentials(UUID userId, String email, String passwordHash, String fullName,
                              UserStatus status, Instant lockedUntil, String locale, String timeZone) {
    }

    public record Access(Set<String> roleCodes, Set<String> permissionCodes, List<ScopedPermission> scoped) {
    }

    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    public AuthenticatedUserLoader(UserRepository userRepository, AuthorizationRepository authorizationRepository) {
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Credentials> findCredentials(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(u -> new Credentials(
                u.getId(), u.getEmail(), u.getPasswordHash(), u.getFullName(),
                u.getStatus(), u.getLockedUntil(), u.getLocale(), u.getTimeZone()));
    }

    @Transactional(readOnly = true)
    public Access loadAccess(UUID userId) {
        return new Access(
                new HashSet<>(authorizationRepository.findTenantWideRoleCodes(userId)),
                new HashSet<>(authorizationRepository.findTenantWidePermissionCodes(userId)),
                authorizationRepository.findScopedPermissions(userId));
    }
}