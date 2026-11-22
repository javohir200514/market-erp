package com.universal.erp.platform.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Login request: tenant slug + email + password. */
public class TenantUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private static final long serialVersionUID = 1L;

    private final String tenantSlug;

    public TenantUsernamePasswordAuthenticationToken(String tenantSlug, String email, String password) {
        super(email, password);
        this.tenantSlug = tenantSlug;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }
}