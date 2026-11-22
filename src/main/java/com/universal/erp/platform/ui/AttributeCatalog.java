package com.universal.erp.platform.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Yo'nalish bo'yicha mahsulot atributlari shablonlari.
 * Joylashuvi: src/main/java/com/universal/erp/retail/business/AttributeCatalog.java
 */
public final class AttributeCatalog {

    /** spec: FieldDef.of formati (kalit|tur|uz|ru|en[|variantlar]). */
    private record Attr(String code, String spec) {
    }

    private static final Map<String, Attr> ALL = new LinkedHashMap<>();
    private static final Map<BusinessType, List<String>> BY_TYPE = new EnumMap<>(BusinessType.class);

    private AttributeCatalog() {
    }

    private static void def(String code, String spec) {
        ALL.put(code, new Attr(code, spec));
    }

    private static void use(BusinessType t, String... codes) {
        BY_TYPE.put(t, List.of(codes));
    }

    static {
        def("brand", "brand|text|Brend|Бренд|Brand");
        def("model", "model|text|Model|Модель|Model");

        // FOOD
        def("weight", "weight|number|Vazn, kg|Вес, кг|Weight, kg");
        def("production_date", "production_date|date|Ishlab chiqarilgan sana|Дата производства|Production date");
        def("expiry_date", "expiry_date|date|Yaroqlilik muddati|Срок годности|Expiry date");
        def("batch_no", "batch_no|text|Partiya raqami|Номер партии|Batch no.");

        // CLOTHING
        def("size", "size|select|O'lcham|Размер|Size|"
                + "xs:XS:XS:XS,s:S:S:S,m:M:M:M,l:L:L:L,xl:XL:XL:XL,xxl:XXL:XXL:XXL");
        def("color", "color|text|Rang|Цвет|Color");
        def("material", "material|text|Material|Материал|Material");

        // ELECTRONICS
        def("serial_no", "serial_no|text|Seriya raqami|Серийный номер|Serial no.");
        def("imei", "imei|text|IMEI|IMEI|IMEI");
        def("warranty_months", "warranty_months|number|Kafolat, oy|Гарантия, мес.|Warranty, months");
        def("manufacturer", "manufacturer|text|Ishlab chiqaruvchi|Производитель|Manufacturer");

        // COSMETICS
        def("volume_ml", "volume_ml|number|Hajm, ml|Объём, мл|Volume, ml");
        def("ingredients", "ingredients|long|Tarkib|Состав|Ingredients");

        use(BusinessType.GENERAL, "brand");
        use(BusinessType.FOOD, "brand", "weight", "production_date", "expiry_date", "batch_no");
        use(BusinessType.CLOTHING, "brand", "model", "size", "color", "material");
        use(BusinessType.ELECTRONICS, "brand", "model", "manufacturer", "serial_no", "imei", "warranty_months");
        use(BusinessType.COSMETICS, "brand", "volume_ml", "ingredients", "production_date", "expiry_date",
                "batch_no");
    }

    /** Tanlangan yo'nalishlar uchun takrorlanmaydigan atribut spec'lari. */
    public static List<String> specsFor(Set<BusinessType> types) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (BusinessType t : BusinessType.values()) {
            if (!types.contains(t)) {
                continue;
            }
            for (String code : BY_TYPE.getOrDefault(t, List.of())) {
                unique.putIfAbsent(code, ALL.get(code).spec());
            }
        }
        return new ArrayList<>(unique.values());
    }
}