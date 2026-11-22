package com.universal.erp.platform.security;

import com.universal.erp.platform.ui.AppModule;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Rol -> modul ruxsati: ko'rish va o'zgartirish. ADMIN (yoki tanilgan roli yo'q eski hisob) hamma narsaga kiradi.
 * Bir nechta rol bo'lsa, ruxsatlar qo'shiladi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RolePolicy.java
 */
public final class RolePolicy {

    private enum Level { VIEW, EDIT }

    private static final Map<String, Map<AppModule, Level>> ACCESS = Map.of(
            "ACCOUNTANT", grants(AppModule.FINANCE, Level.EDIT, AppModule.BI, Level.VIEW),
            "SALES", grants(AppModule.CRM, Level.EDIT, AppModule.FINANCE, Level.VIEW, AppModule.INVENTORY, Level.VIEW),
            "WAREHOUSE", grants(AppModule.INVENTORY, Level.EDIT),
            "HR_MANAGER", grants(AppModule.HR, Level.EDIT),
            "VIEWER", grants(AppModule.FINANCE, Level.VIEW, AppModule.CRM, Level.VIEW,
                    AppModule.INVENTORY, Level.VIEW, AppModule.HR, Level.VIEW, AppModule.BI, Level.VIEW));

    private RolePolicy() {
    }

    private static Map<AppModule, Level> grants(Object... pairs) {
        Map<AppModule, Level> m = new EnumMap<>(AppModule.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((AppModule) pairs[i], (Level) pairs[i + 1]);
        }
        return m;
    }

    public static boolean isAdmin() {
        return RoleChecks.isAdmin();
    }

    public static boolean canView(AppModule m) {
        return level(m) != null;
    }

    public static boolean canWrite(AppModule m) {
        return level(m) == Level.EDIT;
    }

    private static Level level(AppModule m) {
        if (RoleChecks.isAdmin()) {
            return Level.EDIT;
        }
        Level best = null;
        Set<String> roles = RoleChecks.knownRoles();
        for (String role : roles) {
            Map<AppModule, Level> g = ACCESS.get(role);
            Level l = g == null ? null : g.get(m);
            if (l == Level.EDIT) {
                return Level.EDIT;
            }
            if (l == Level.VIEW) {
                best = Level.VIEW;
            }
        }
        return best;
    }
}