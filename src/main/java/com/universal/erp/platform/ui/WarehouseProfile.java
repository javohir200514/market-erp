package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BusinessType.Capability;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kompaniya tanlagan yo'nalishlarga ({@link BusinessType}) qarab Ombor moduli nimani ko'rsatishini belgilaydi:
 * mahsulot maydonlari, bo'limlar, o'lchov birliklari va yorliqlar. Bir nechta yo'nalish tanlansa, imkoniyatlar
 * birlashtiriladi (masalan Oziq-ovqat + Elektronika: muddat ham, seriya raqami ham chiqadi).
 *
 * Yo'nalishlar bo'sh bo'lsa (eski kompaniyalar), hamma imkoniyat yoqilgan holat qaytadi.
 *
 * Ishlatish:
 *   WarehouseProfile p = warehouseProfiles.current();
 *   if (p.has(Field.EXPIRY_DATE)) form.add(expiryField);
 *   unitSelect.setItems(p.units());
 *   p.require(Field.SERIAL_NO, "Seriya raqami");   // backendda (UI yashirishi yetarli emas)
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/WarehouseProfile.java
 */
public final class WarehouseProfile {

    /** Mahsulot kartasidagi yo'nalishga xos maydonlar. */
    public enum Field {
        BRAND, CATEGORY,
        EXPIRY_DATE, BATCH_NO, WEIGHT,
        SIZE, COLOR, MATERIAL,
        SERIAL_NO, IMEI, WARRANTY_MONTHS,
        VOLUME, INGREDIENTS
    }

    /** Ombor bo'limlari. */
    public enum Section {
        STOCK, RECEIPTS, ISSUES, TRANSFERS, STOCKTAKE, WRITE_OFFS,
        BATCHES, EXPIRY, SERIALS, VARIANTS
    }

    /** O'lchov birliklari. Tartib: uz, ru, en. */
    public enum Unit {
        PIECE("dona", "шт.", "pcs", false),
        PAIR("juft", "пара", "pair", false),
        PACK("pachka", "пачка", "pack", false),
        BOX("quti", "коробка", "box", false),
        BAG("qop", "мешок", "bag", false),
        KG("kg", "кг", "kg", true),
        GRAM("g", "г", "g", true),
        LITER("litr", "л", "l", true),
        ML("ml", "мл", "ml", true),
        METER("metr", "м", "m", true);

        private final String[] names;
        private final boolean fractional;

        Unit(String uz, String ru, String en, boolean fractional) {
            this.names = new String[] {uz, ru, en};
            this.fractional = fractional;
        }

        public String label(String lang) {
            return pick(lang, names[0], names[1], names[2]);
        }

        /** Kasr miqdor mumkinmi (masalan 1,5 kg). */
        public boolean fractional() {
            return fractional;
        }
    }

    private final Set<Capability> capabilities;
    private final Set<Field> fields = EnumSet.of(Field.BRAND, Field.CATEGORY);
    private final Set<Section> sections =
            EnumSet.of(Section.STOCK, Section.RECEIPTS, Section.ISSUES, Section.TRANSFERS,
                    Section.STOCKTAKE, Section.WRITE_OFFS);
    private final Set<Unit> units = EnumSet.of(Unit.PIECE, Unit.PACK, Unit.BOX);

    private WarehouseProfile(Set<Capability> capabilities) {
        this.capabilities = capabilities;
        for (Capability c : capabilities) {
            switch (c) {
                case BATCH_TRACKING -> {
                    fields.add(Field.BATCH_NO);
                    sections.add(Section.BATCHES);
                }
                case EXPIRY_TRACKING -> {
                    fields.add(Field.EXPIRY_DATE);
                    sections.add(Section.EXPIRY);
                }
                case WEIGHT_PRODUCTS -> {
                    fields.add(Field.WEIGHT);
                    units.addAll(List.of(Unit.KG, Unit.GRAM));
                }
                case SIZE_VARIANTS -> {
                    fields.add(Field.SIZE);
                    sections.add(Section.VARIANTS);
                    units.add(Unit.PAIR);
                }
                case COLOR_VARIANTS -> {
                    fields.add(Field.COLOR);
                    sections.add(Section.VARIANTS);
                }
                case MATERIAL_ATTRIBUTES -> fields.add(Field.MATERIAL);
                case SERIAL_TRACKING -> {
                    fields.addAll(List.of(Field.SERIAL_NO, Field.IMEI));
                    sections.add(Section.SERIALS);
                }
                case WARRANTY_TRACKING -> fields.add(Field.WARRANTY_MONTHS);
                case VOLUME_ATTRIBUTES -> {
                    fields.add(Field.VOLUME);
                    units.addAll(List.of(Unit.LITER, Unit.ML));
                }
                case INGREDIENTS -> fields.add(Field.INGREDIENTS);
                case STANDARD_CATALOG -> units.addAll(List.of(Unit.KG, Unit.LITER, Unit.METER, Unit.BAG));
            }
        }
    }

    /** Tanlangan yo'nalishlar bo'yicha profil; bo'sh bo'lsa hamma imkoniyat yoqilgan. */
    public static WarehouseProfile of(Set<BusinessType> types) {
        if (types == null || types.isEmpty()) {
            return new WarehouseProfile(EnumSet.allOf(Capability.class));
        }
        return new WarehouseProfile(BusinessType.capabilitiesOf(types));
    }

    // ================================================================ so'rovlar

    public boolean can(Capability c) {
        return capabilities.contains(c);
    }

    /** Maydon mahsulot kartasida ko'rinsinmi. */
    public boolean has(Field f) {
        return fields.contains(f);
    }

    /** Bo'lim menyuda ko'rinsinmi. */
    public boolean shows(Section s) {
        return sections.contains(s);
    }

    public Set<Unit> units() {
        return units;
    }

    /** Har bir dona seriya raqami bilan yuritiladimi (miqdor faqat butun bo'ladi). */
    public boolean tracksSerials() {
        return can(Capability.SERIAL_TRACKING);
    }

    public boolean tracksExpiry() {
        return can(Capability.EXPIRY_TRACKING);
    }

    /** Birlik uchun kasr miqdor mumkinmi (masalan kg, litr, metr: ha; dona, juft, quti: yo'q). */
    public boolean allowsFraction(Unit unit) {
        return unit == null || unit.fractional();
    }

    /**
     * Backend tekshiruvi: yo'nalishga mos kelmaydigan maydon yuborilsa xato beradi.
     * UI da yashirish yetarli emas, shuning uchun saqlashdan oldin chaqiring.
     */
    public void require(Field f, String name) {
        if (!has(f)) {
            throw new IllegalStateException("Bu yo'nalish uchun '" + name + "' maydoni yoqilmagan");
        }
    }

    /**
     * Menyudagi ichki bo'lim ko'rinsinmi (marshrut nomi bo'yicha). Nomi tanilmagan bo'limlar doim ko'rinadi.
     * Masalan "app/inventory/serials" faqat seriya raqami yoqilgan bo'lsa chiqadi.
     */
    public boolean isRouteVisible(String route) {
        if (route == null) {
            return true;
        }
        String r = route.toLowerCase(Locale.ROOT);
        if (containsAny(r, "serial", "imei")) {
            return shows(Section.SERIALS);
        }
        if (containsAny(r, "warrant", "kafolat")) {
            return can(Capability.WARRANTY_TRACKING);
        }
        if (containsAny(r, "expir", "muddat")) {
            return shows(Section.EXPIRY);
        }
        if (containsAny(r, "batch", "partiya", "lot")) {
            return shows(Section.BATCHES);
        }
        if (containsAny(r, "variant")) {
            return shows(Section.VARIANTS);
        }
        return true;
    }

    // ================================================================ yorliqlar

    /** "Rang va o'lcham", "O'lcham", "Rang" yoki oddiy "Variant". */
    public String variantLabel(String lang) {
        boolean size = can(Capability.SIZE_VARIANTS);
        boolean color = can(Capability.COLOR_VARIANTS);
        if (size && color) {
            return pick(lang, "Rang va o'lcham", "Цвет и размер", "Color and size");
        }
        if (size) {
            return pick(lang, "O'lcham", "Размер", "Size");
        }
        if (color) {
            return pick(lang, "Rang", "Цвет", "Color");
        }
        return pick(lang, "Variant", "Вариант", "Variant");
    }

    /** "Partiya va muddat", "Partiya", "Yaroqlilik muddati" yoki bo'sh (kerak emas). */
    public String batchLabel(String lang) {
        boolean batch = can(Capability.BATCH_TRACKING);
        boolean expiry = can(Capability.EXPIRY_TRACKING);
        if (batch && expiry) {
            return pick(lang, "Partiya va muddat", "Партия и срок", "Batch and expiry");
        }
        if (batch) {
            return pick(lang, "Partiya", "Партия", "Batch");
        }
        if (expiry) {
            return pick(lang, "Yaroqlilik muddati", "Срок годности", "Expiry date");
        }
        return "";
    }

    private static boolean containsAny(String s, String... parts) {
        for (String p : parts) {
            if (s.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private static String pick(String lang, String uz, String ru, String en) {
        return "ru".equals(lang) ? ru : "en".equals(lang) ? en : uz;
    }
}