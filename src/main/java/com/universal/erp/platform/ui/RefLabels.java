package com.universal.erp.platform.ui;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * REF maydonidagi ID dan ko'rsatiladigan nomni oladi va RefResolver ga ulanadi.
 *
 * Eski (migratsiya qilinmagan) yozuvlarda maydonda hali nom turgan bo'lishi mumkin:
 * UUID bo'lmagan qiymat o'zi qaytariladi, shuning uchun eski ma'lumot buzilmay ko'rinadi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RefLabels.java
 */
@Component
public class RefLabels implements RefResolver.Lookup {

    private final RecordStore store;

    public RefLabels(RecordStore store) {
        this.store = store;
    }

    @PostConstruct
    void install() {
        RefResolver.install(this);
    }

    @Override
    public String label(String sectionKey, String id) {
        Optional<AppSection> section = AppSection.findByKey(sectionKey);
        if (section.isEmpty()) {
            return id;
        }
        Optional<RecordStore.Rec> rec = store.find(CurrentCompany.get(), sectionKey, id);
        if (rec.isPresent()) {
            return name(section.get(), rec.get());
        }
        return isUuid(id) ? "—" : id; // UUID bo'lsa — yozuv o'chirilgan; aks holda eski matn
    }

    /** Dropdown va ro'yxatlarda ko'rsatiladigan nom. */
    public static String name(AppSection section, RecordStore.Rec rec) {
        if ("finance/invoices".equals(section.key())) {
            return "№ " + rec.get("number") + " · " + FieldDef.format(FieldDef.parse(rec.get("amount")));
        }
        for (FieldDef f : section.fields()) {
            if (f.required() && f.type() == FieldDef.Type.TEXT && !rec.get(f.key()).isBlank()) {
                return rec.get(f.key());
            }
        }
        for (FieldDef f : section.fields()) {
            if (f.type() == FieldDef.Type.TEXT && !rec.get(f.key()).isBlank()) {
                return rec.get(f.key());
            }
        }
        return rec.id();
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}