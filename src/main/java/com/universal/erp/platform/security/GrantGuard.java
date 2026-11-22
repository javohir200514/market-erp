package com.universal.erp.platform.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** Prevents privilege escalation: you can only hand out permissions you hold tenant-wide yourself. */
@Component
public class GrantGuard {
    private final CurrentUser currentUser;

    public GrantGuard(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public void assertCanGrant(Collection<String> permissionCodes) {
        ErpUserDetails me = currentUser.require();
        boolean allowed = permissionCodes.stream().allMatch(me::hasTenantWidePermission);
        if (!allowed) {
            throw new AccessDeniedException("You cannot grant permissions you do not hold");
        }
    }
}