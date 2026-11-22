package com.universal.erp.platform.ui;


import com.vaadin.flow.component.Html;

import java.util.Map;

/**
 * Bosh sahifa uchun inline SVG ikonlar (24x24, chiziqli uslub, rangi currentColor).
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/HomeIcons.java
 * Yangi ikon qo'shish: PATHS ga nom va SVG path ma'lumotini kiriting.
 */
final class HomeIcons {

    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("finance", "M3 21h18 M5 21V10 M9 21V10 M15 21V10 M19 21V10 M12 3 3 8h18z"),
            Map.entry("crm", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"),
            Map.entry("inventory", "M16.5 9.4 7.5 4.21 M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96 12 12.01l8.73-5.05 M12 22.08V12"),
            // Kassa apparati: ekran, bo'yin, korpus va tugmalar
            Map.entry("pos", "M5 3h14a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M6 9v3 M18 9v3 M4 12h16a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z M7 16h2 M11 16h2 M15 16h2"),
            Map.entry("hr", "M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16 M20 6H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2z"),
            Map.entry("bi", "M3 3v18h18 M7 15l4-4 3 3 5-6"),
            Map.entry("bars", "M18 20V10 M12 20V4 M6 20v-6"),
            Map.entry("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4"),
            Map.entry("building", "M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18z M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2 M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2 M10 6h4 M10 10h4 M10 14h4 M10 18h4"),
            Map.entry("key", "M21 2l-2 2 M11.4 11.6a5.5 5.5 0 1 1-7.8 7.8 5.5 5.5 0 0 1 7.8-7.8z M11.4 11.6 15.5 7.5 M15.5 7.5l3 3L22 7l-3-3"),
            Map.entry("history", "M12 8v4l3 2 M3 12a9 9 0 1 0 3-6.7L3 8 M3 3v5h5"),
            Map.entry("globe", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M2 12h20 M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"),
            Map.entry("plug", "M12 22v-5 M9 8V2 M15 8V2 M18 8v5a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V8z"),
            Map.entry("check", "M20 6 9 17l-5-5"),
            Map.entry("arrow", "M5 12h14 M12 5l7 7-7 7"),
            Map.entry("user", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"),
            Map.entry("user-plus", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M19 8v6 M22 11h-6"),
            Map.entry("layers", "M12 2 2 7l10 5 10-5z M2 17l10 5 10-5 M2 12l10 5 10-5"),
            Map.entry("send", "M22 2 11 13 M22 2l-7 20-4-9-9-4z"),
            Map.entry("file", "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8"),
            Map.entry("cart", "M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0"),
            Map.entry("receipt", "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1z M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8 M12 17.5v-11"),
            Map.entry("card", "M21 4H3a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h18a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M1 10h22"),
            Map.entry("home", "M3 9.5 12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"),
            Map.entry("check-circle", "M22 11.08V12a10 10 0 1 1-5.93-9.14 M9 11l3 3L22 4"),
            Map.entry("menu", "M4 6h16 M4 12h16 M4 18h16"),
            Map.entry("search", "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.3-4.3"),
            Map.entry("bell", "M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9 M13.7 21a2 2 0 0 1-3.4 0"));

    static Html svg(String name) {
        String d = PATHS.getOrDefault(name, "");
        return new Html("<svg class=\"home-svg\" style=\"width:var(--ico,24px);height:var(--ico,24px)\" xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" "
                + "fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" "
                + "stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"" + d + "\"></path></svg>");
    }

    private HomeIcons() {
    }
}