package com.universal.erp.platform.ui;

import com.vaadin.flow.server.VaadinSession;

/**
 * Tizimga kirgan foydalanuvchining kompaniyasi (server sessiyasida). Login va ro'yxatdan
 * o'tishdan keyin {@link #set(String)} chaqiriladi.
 */
public final class CurrentCompany {

    private static final String KEY = "erp.company.current";

    private CurrentCompany() {
    }

    public static void set(String company) {
        VaadinSession s = VaadinSession.getCurrent();
        if (s != null) {
            s.setAttribute(KEY, key(company));
        }
    }

    /** "Nur Savdo" va "nur-savdo" bir xil kalit beradi. */
    public static String key(String company) {
        if (company == null) {
            return "";
        }
        String k = company.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-");
        return k.replaceAll("^-+|-+$", "");
    }

    /** Kompaniya login nomi yoki bo'sh satr. */
    public static String get() {
        VaadinSession s = VaadinSession.getCurrent();
        Object v = s == null ? null : s.getAttribute(KEY);
        return v == null ? "" : v.toString();
    }
}