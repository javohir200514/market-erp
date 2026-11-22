package com.universal.erp.platform.ui;

import java.util.Arrays;
import java.util.Optional;

/**
 * ERP modullari. Ro'yxatdan o'tishning 3-qadamida tanlanadi,
 * ilova qobig'ida menyu sifatida chiqadi.
 *
 * key — ikon nomi (HomeIcons), matn kaliti (AppTexts: mod.<key>.t/.d/.f)
 * va marshrut qismi.
 */
public enum AppModule {

    FINANCE("finance"),
    CRM("crm"),
    INVENTORY("inventory"),
    POS("pos"),
    HR("hr"),
    BI("bi");

    private final String key;

    AppModule(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String icon() {
        return key;
    }

    public String route() {
        return "app/" + key;
    }

    public String title() {
        return AppTexts.t("mod." + key + ".t");
    }

    public String description() {
        return AppTexts.t("mod." + key + ".d");
    }

    /**
     * "A · B · C" ko'rinishidagi qisqa imkoniyatlar ro'yxati.
     */
    public String features() {
        return AppTexts.t("mod." + key + ".f");
    }

    public static Optional<AppModule> byKey(String key) {
        return Arrays.stream(values())
                .filter(m -> m.key.equals(key))
                .findFirst();
    }
}