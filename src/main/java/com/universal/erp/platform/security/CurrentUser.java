package com.universal.erp.platform.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Access to the logged-in user. Bean name "currentUser" is used in @PreAuthorize expressions. */
@Component("currentUser")
public class CurrentUser {

    public Optional<ErpUserDetails> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof ErpUserDetails user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public ErpUserDetails require() {
        return get().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Not authenticated"));
    }

    public UUID userId() {
        return require().getUserId();
    }

    public UUID tenantId() {
        return require().getTenantId();
    }

    public boolean isSelf(UUID userId) {
        return userId != null && get().map(u -> u.getUserId().equals(userId)).orElse(false);
    }
}