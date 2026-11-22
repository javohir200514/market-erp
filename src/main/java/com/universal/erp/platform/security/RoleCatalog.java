package com.universal.erp.platform.security;

import com.universal.erp.platform.ui.AppTexts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tizim rollari (ro'yxatdan o'tgan ADMIN + xodimlar uchun tayyor rollar) — nomi va tavsifi uch tilda.
 * Qaysi modulga qanday kirishi {@link RolePolicy}'da.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RoleCatalog.java
 */
public final class RoleCatalog {

    public record Role(String code, String[] names, String[] descriptions) {
        public String name() {
            return names[AppTexts.index()];
        }

        public String description() {
            return descriptions[AppTexts.index()];
        }
    }

    private static final Map<String, Role> ROLES = new LinkedHashMap<>();

    private RoleCatalog() {
    }

    private static void add(String code, String nUz, String nRu, String nEn, String dUz, String dRu, String dEn) {
        ROLES.put(code, new Role(code, new String[] {nUz, nRu, nEn}, new String[] {dUz, dRu, dEn}));
    }

    static {
        add("ADMIN", "Administrator", "Администратор", "Administrator",
                "Hamma narsa: modullar, xodimlar va rollarni boshqarish",
                "Всё: управление модулями, сотрудниками и ролями",
                "Everything: manage modules, staff and roles");
        add("ACCOUNTANT", "Buxgalter", "Бухгалтер", "Accountant",
                "Moliya — to'liq; BI — faqat ko'rish",
                "Финансы — полный доступ; BI — только просмотр",
                "Finance — full; BI — view only");
        add("SALES", "Savdo menejeri", "Менеджер по продажам", "Sales manager",
                "CRM — to'liq; Moliya va Ombor — faqat ko'rish",
                "CRM — полный доступ; Финансы и Склад — только просмотр",
                "CRM — full; Finance and Inventory — view only");
        add("WAREHOUSE", "Omborchi", "Кладовщик", "Warehouse keeper",
                "Ombor — to'liq",
                "Склад — полный доступ",
                "Inventory — full");
        add("HR_MANAGER", "Kadrlar xodimi", "Сотрудник отдела кадров", "HR manager",
                "Kadrlar — to'liq",
                "Кадры — полный доступ",
                "HR — full");
        add("VIEWER", "Kuzatuvchi", "Наблюдатель", "Viewer",
                "Barcha modullarni faqat ko'rish",
                "Просмотр всех модулей без изменений",
                "View all modules, no changes");
    }

    public static List<Role> all() {
        return List.copyOf(ROLES.values());
    }

    public static Role get(String code) {
        return ROLES.get(code);
    }

    /** Joriy tildagi nom; noma'lum kod bo'lsa — kodning o'zi. */
    public static String label(String code) {
        Role r = ROLES.get(code);
        return r == null ? code : r.name();
    }

    /** Bazaga yoziladigan nom (o'zbekcha). */
    public static String storedName(String code) {
        Role r = ROLES.get(code);
        return r == null ? code : r.names()[0];
    }

    public static String storedDescription(String code) {
        Role r = ROLES.get(code);
        return r == null ? "" : r.descriptions()[0];
    }
}