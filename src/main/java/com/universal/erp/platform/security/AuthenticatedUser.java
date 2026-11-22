package com.universal.erp.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Kirgan foydalanuvchi (SecurityContext ichidagi principal).
 * Tenant ID shu yerdan olinadi — hech qachon so'rov parametridan emas.
 *
 * Authorities: rollar "ROLE_<KOD>" ko'rinishida, ruxsatlar esa o'z kodi bilan
 * (masalan "platform.user.create").
 */
public record AuthenticatedUser(
        UUID userId,
        UUID tenantId,
        String tenantSlug,
        String email,
        String fullName,
        String locale,
        String timeZone,
        List<String> roles,
        List<String> permissions) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(roles.size() + permissions.size());
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // parol sessiyada saqlanmaydi
    }

    @Override
    public String getUsername() {
        return email;
    }
}