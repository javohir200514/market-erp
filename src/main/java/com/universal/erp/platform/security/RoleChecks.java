package com.universal.erp.platform.security;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Joriy foydalanuvchining rollari (Spring Security authorities'dan: "ROLE_ADMIN" yoki "ADMIN").
 *
 * Eslatma: tanilgan rollardan birortasi ham topilmasa (masalan, eski hisob), foydalanuvchi
 * administrator hisoblanadi, shunda mavjud tizim buzilmaydi. Yangi xodimlar doim tanilgan rol bilan yaratiladi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/security/RoleChecks.java
 */
public final class RoleChecks {

    public static final String ADMIN = "ADMIN";
    public static final Set<String> KNOWN =
            Set.of("ADMIN", "ACCOUNTANT", "SALES", "WAREHOUSE", "HR_MANAGER", "VIEWER");

    private RoleChecks() {
    }

    /** Katta harfda, "ROLE_" prefiksisiz. */
    public static Set<String> currentRoles() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            return Set.of();
        }
        return a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(s -> s != null)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .collect(Collectors.toSet());
    }

    /** Tanilgan rollar (ADMIN, ACCOUNTANT ...) — boshqa ruxsat kodlarisiz. */
    public static Set<String> knownRoles() {
        return currentRoles().stream().filter(KNOWN::contains).collect(Collectors.toSet());
    }

    public static boolean isAdmin() {
        Set<String> known = knownRoles();
        return known.isEmpty() || known.contains(ADMIN);
    }
}