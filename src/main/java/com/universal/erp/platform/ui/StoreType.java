package com.universal.erp.platform.ui;

import java.util.List;

/**
 * Do'kon turi: ro'yxatdan o'tishda tanlanadi va Ombor modulini moslashtiradi.
 * Bazada faqat {@link #code()} saqlanadi (masalan "grocery").
 * Tartib: uz, ru, en.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/StoreType.java
 */
public enum StoreType {

    GROCERY("grocery",
            "Oziq-ovqat / market", "Продукты / маркет", "Grocery / market",
            "Non, sut, ichimlik va boshqa tez sotiladigan mahsulotlar",
            "Хлеб, молоко, напитки и другие товары быстрого спроса",
            "Bread, milk, drinks and other fast-moving goods"),

    APPAREL("apparel",
            "Kiyim-kechak / poyabzal", "Одежда / обувь", "Clothing / footwear",
            "Rang va o'lcham bo'yicha variantlar",
            "Варианты по цвету и размеру",
            "Variants by color and size"),

    ELECTRONICS("electronics",
            "Elektronika / maishiy texnika", "Электроника / бытовая техника", "Electronics / appliances",
            "Seriya raqami va kafolat bilan",
            "С серийным номером и гарантией",
            "With serial numbers and warranty"),

    HARDWARE("hardware",
            "Qurilish va xo'jalik mollari", "Стройматериалы / хозтовары", "Hardware / building materials",
            "Metr, kg, qop kabi birliklar",
            "Единицы: метр, кг, мешок",
            "Units like meter, kg and bag"),

    GENERAL("general",
            "Boshqa / aralash", "Другое / смешанный", "Other / mixed",
            "Barcha imkoniyatlar yoqilgan",
            "Все возможности включены",
            "All features enabled");

    private static final List<String> CODES = List.of("uz", "ru", "en");

    private final String code;
    private final String[] titles;
    private final String[] hints;

    StoreType(String code, String tUz, String tRu, String tEn, String hUz, String hRu, String hEn) {
        this.code = code;
        this.titles = new String[] {tUz, tRu, tEn};
        this.hints = new String[] {hUz, hRu, hEn};
    }

    /** Bazada saqlanadigan kod. */
    public String code() {
        return code;
    }

    public String title(String lang) {
        return titles[index(lang)];
    }

    public String hint(String lang) {
        return hints[index(lang)];
    }

    /** Noma'lum yoki bo'sh kod uchun {@link #GENERAL} qaytariladi (eski kompaniyalar buzilmasin). */
    public static StoreType fromCode(String code) {
        if (code != null) {
            for (StoreType t : values()) {
                if (t.code.equalsIgnoreCase(code.trim())) {
                    return t;
                }
            }
        }
        return GENERAL;
    }

    private static int index(String lang) {
        int i = CODES.indexOf(lang);
        return i < 0 ? 0 : i;
    }
}