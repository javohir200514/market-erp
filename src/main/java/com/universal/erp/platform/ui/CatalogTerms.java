package com.universal.erp.platform.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Do'kon yo'nalishiga qarab katalog nomlari va toifalar: "Mahsulotlar" o'rniga "Kiyimlar", "Qurilmalar" va hokazo.
 * Bitta yo'nalish tanlangan bo'lsa shuning nomi, bir nechta (yoki hech biri) bo'lsa umumiy "Tovarlar" ishlatiladi.
 * Tartib: uz, ru, en.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/CatalogTerms.java
 */
public final class CatalogTerms {

    /** Sahifa sarlavhasi, tugma va dialog nomlari. */
    public record Terms(String title, String add, String newTitle, String editTitle, String empty,
                        String example, String defaultCategory, List<Category> categories) {
        /** "Kiyim qo'shish" tugmasi matni (add bilan bir xil). */
        public String addLabel() {
            return add;
        }

        /** Yangi yozuv dialogi sarlavhasi (newTitle bilan bir xil). */
        public String createTitle() {
            return newTitle;
        }
    }

    /** Toifa: bazada saqlanadigan kalit va ko'rinadigan nom. */
    public record Category(String key, String label) {
    }

    private record CatDef(String key, String uz, String ru, String en) {
    }

    private static final Map<BusinessType, String[][]> TERMS = new EnumMap<>(BusinessType.class);
    private static final Map<BusinessType, List<CatDef>> CATS = new EnumMap<>(BusinessType.class);
    private static final CatDef OTHER = new CatDef("other", "Boshqa", "Другое", "Other");

    private CatalogTerms() {
    }

    /** Mahsulot formasidagi qo'shimcha maydonlar tartibi (toifa alohida chiplar bilan tanlanadi). */
    public static final List<WarehouseProfile.Field> FIELD_ORDER = List.of(
            WarehouseProfile.Field.BRAND, WarehouseProfile.Field.SIZE, WarehouseProfile.Field.COLOR,
            WarehouseProfile.Field.MATERIAL, WarehouseProfile.Field.WEIGHT, WarehouseProfile.Field.VOLUME,
            WarehouseProfile.Field.BATCH_NO, WarehouseProfile.Field.EXPIRY_DATE,
            WarehouseProfile.Field.SERIAL_NO, WarehouseProfile.Field.IMEI,
            WarehouseProfile.Field.WARRANTY_MONTHS, WarehouseProfile.Field.INGREDIENTS);

    /** Kiritish turi: "text", "number" yoki "date". */
    public static String attrKind(WarehouseProfile.Field f) {
        return switch (f) {
            case WEIGHT, VOLUME, WARRANTY_MONTHS -> "number";
            case EXPIRY_DATE -> "date";
            default -> "text";
        };
    }

    /** Forma setkasida butun qatorni egallaydimi. */
    public static boolean attrWide(WarehouseProfile.Field f) {
        return f == WarehouseProfile.Field.INGREDIENTS || f == WarehouseProfile.Field.MATERIAL;
    }

    /** Toifa kalitiga ko'ra joriy tildagi nom; topilmasa kalitning o'zi. */
    public static String categoryLabel(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        int i = index(AppTexts.lang());
        for (List<CatDef> list : CATS.values()) {
            for (CatDef c : list) {
                if (c.key().equalsIgnoreCase(key.trim())) {
                    return i == 1 ? c.ru() : i == 2 ? c.en() : c.uz();
                }
            }
        }
        return key.trim();
    }

    /** Qo'shimcha maydonning yozuvdagi (RecordStore) kaliti. */
    public static String attrKey(WarehouseProfile.Field f) {
        return switch (f) {
            case BRAND -> "brand";
            case CATEGORY -> "category";
            case EXPIRY_DATE -> "expiry_date";
            case BATCH_NO -> "batch_no";
            case WEIGHT -> "weight";
            case SIZE -> "size";
            case COLOR -> "color";
            case MATERIAL -> "material";
            case SERIAL_NO -> "serial_no";
            case IMEI -> "imei";
            case WARRANTY_MONTHS -> "warranty_months";
            case VOLUME -> "volume";
            case INGREDIENTS -> "ingredients";
        };
    }

    /** Mahsulot/kirim formalarida ko'rinadigan qo'shimcha maydon nomi (joriy til bo'yicha). */
    public static String attrLabel(WarehouseProfile.Field f) {
        return switch (f) {
            case BRAND -> L.t("Brend", "Бренд", "Brand");
            case CATEGORY -> L.t("Toifa", "Категория", "Category");
            case EXPIRY_DATE -> L.t("Yaroqlilik muddati", "Срок годности", "Expiry date");
            case BATCH_NO -> L.t("Partiya raqami", "Номер партии", "Batch no.");
            case WEIGHT -> L.t("Og'irligi", "Вес", "Weight");
            case SIZE -> L.t("O'lcham", "Размер", "Size");
            case COLOR -> L.t("Rangi", "Цвет", "Color");
            case MATERIAL -> L.t("Material", "Материал", "Material");
            case SERIAL_NO -> L.t("Seriya raqami", "Серийный номер", "Serial no.");
            case IMEI -> "IMEI";
            case WARRANTY_MONTHS -> L.t("Kafolat (oy)", "Гарантия (мес.)", "Warranty (months)");
            case VOLUME -> L.t("Hajmi", "Объём", "Volume");
            case INGREDIENTS -> L.t("Tarkibi", "Состав", "Ingredients");
        };
    }

    public static Terms of(Set<BusinessType> types, String lang) {
        BusinessType t = types != null && types.size() == 1 ? types.iterator().next() : BusinessType.GENERAL;
        String[][] r = TERMS.get(t);
        int i = index(lang);
        return new Terms(r[0][i], r[1][i], r[2][i], r[3][i], r[4][i], r[5][i], "", categories(types, lang));
    }

    /** Tanlangan yo'nalishlar toifalarining birlashmasi; oxirida doim "Boshqa". Yo'nalish bo'sh bo'lsa hammasi. */
    public static List<Category> categories(Set<BusinessType> types, String lang) {
        Map<String, CatDef> all = new LinkedHashMap<>();
        for (BusinessType t : BusinessType.values()) {
            if (types == null || types.isEmpty() || types.contains(t)) {
                for (CatDef c : CATS.get(t)) {
                    all.putIfAbsent(c.key(), c);
                }
            }
        }
        all.remove(OTHER.key());
        all.put(OTHER.key(), OTHER);
        int i = index(lang);
        List<Category> out = new ArrayList<>();
        for (CatDef c : all.values()) {
            out.add(new Category(c.key(), i == 1 ? c.ru() : i == 2 ? c.en() : c.uz()));
        }
        return out;
    }

    private static int index(String lang) {
        return "ru".equals(lang) ? 1 : "en".equals(lang) ? 2 : 0;
    }

    private static String[] t(String uz, String ru, String en) {
        return new String[] {uz, ru, en};
    }

    private static CatDef c(String key, String uz, String ru, String en) {
        return new CatDef(key, uz, ru, en);
    }

    static {
        // sarlavha, qo'shish tugmasi, yangi, tahrirlash, bo'sh holat, misol
        TERMS.put(BusinessType.FOOD, new String[][] {
                t("Oziq-ovqat", "Продукты", "Food"),
                t("Oziq-ovqat qo'shish", "Добавить продукт", "Add food item"),
                t("Yangi oziq-ovqat", "Новый продукт", "New food item"),
                t("Oziq-ovqatni tahrirlash", "Редактировать продукт", "Edit food item"),
                t("Hali oziq-ovqat qo'shilmagan.", "Продукты ещё не добавлены.", "No food items added yet."),
                t("Masalan: Sut 1 litr", "Например: Молоко 1 л", "e.g. Milk 1 l")});
        TERMS.put(BusinessType.CLOTHING, new String[][] {
                t("Kiyimlar", "Одежда", "Clothing"),
                t("Kiyim qo'shish", "Добавить одежду", "Add clothing item"),
                t("Yangi kiyim", "Новая одежда", "New clothing item"),
                t("Kiyimni tahrirlash", "Редактировать одежду", "Edit clothing item"),
                t("Hali kiyim qo'shilmagan.", "Одежда ещё не добавлена.", "No clothing added yet."),
                t("Masalan: Erkaklar ko'ylagi", "Например: Мужская рубашка", "e.g. Men's shirt")});
        TERMS.put(BusinessType.ELECTRONICS, new String[][] {
                t("Qurilmalar", "Техника", "Devices"),
                t("Qurilma qo'shish", "Добавить устройство", "Add device"),
                t("Yangi qurilma", "Новое устройство", "New device"),
                t("Qurilmani tahrirlash", "Редактировать устройство", "Edit device"),
                t("Hali qurilma qo'shilmagan.", "Устройства ещё не добавлены.", "No devices added yet."),
                t("Masalan: Samsung Galaxy A15", "Например: Samsung Galaxy A15", "e.g. Samsung Galaxy A15")});
        TERMS.put(BusinessType.COSMETICS, new String[][] {
                t("Kosmetika", "Косметика", "Cosmetics"),
                t("Kosmetika qo'shish", "Добавить косметику", "Add cosmetic product"),
                t("Yangi kosmetika", "Новая косметика", "New cosmetic product"),
                t("Kosmetikani tahrirlash", "Редактировать косметику", "Edit cosmetic product"),
                t("Hali kosmetika qo'shilmagan.", "Косметика ещё не добавлена.", "No cosmetics added yet."),
                t("Masalan: Namlovchi krem", "Например: Увлажняющий крем", "e.g. Moisturizing cream")});
        TERMS.put(BusinessType.GENERAL, new String[][] {
                t("Tovarlar", "Товары", "Products"),
                t("Tovar qo'shish", "Добавить товар", "Add product"),
                t("Yangi tovar", "Новый товар", "New product"),
                t("Tovarni tahrirlash", "Редактировать товар", "Edit product"),
                t("Hali tovar qo'shilmagan.", "Товары ещё не добавлены.", "No products added yet."),
                t("Masalan: Tovar nomi", "Например: Название товара", "e.g. Product name")});

        CATS.put(BusinessType.FOOD, List.of(
                c("bakery", "Non va qandolat", "Хлеб и выпечка", "Bakery"),
                c("dairy", "Sut mahsulotlari", "Молочные продукты", "Dairy"),
                c("meat", "Go'sht va parranda", "Мясо и птица", "Meat and poultry"),
                c("produce", "Meva-sabzavot", "Овощи и фрукты", "Fruit and vegetables"),
                c("drinks", "Ichimliklar", "Напитки", "Drinks"),
                c("grocery", "Bakaleya", "Бакалея", "Grocery"),
                c("sweets", "Shirinliklar", "Сладости", "Sweets")));
        CATS.put(BusinessType.CLOTHING, List.of(
                c("men", "Erkaklar", "Мужское", "Men"),
                c("women", "Ayollar", "Женское", "Women"),
                c("kids", "Bolalar", "Детское", "Kids"),
                c("shoes", "Poyabzal", "Обувь", "Footwear"),
                c("outerwear", "Ustki kiyim", "Верхняя одежда", "Outerwear"),
                c("accessories", "Aksessuarlar", "Аксессуары", "Accessories")));
        CATS.put(BusinessType.ELECTRONICS, List.of(
                c("phones", "Telefonlar", "Телефоны", "Phones"),
                c("computers", "Kompyuter va noutbuklar", "Компьютеры и ноутбуки", "Computers and laptops"),
                c("tv_audio", "Televizor va audio", "ТВ и аудио", "TV and audio"),
                c("appliances", "Maishiy texnika", "Бытовая техника", "Appliances"),
                c("el_accessories", "Aksessuarlar", "Аксессуары", "Accessories")));
        CATS.put(BusinessType.COSMETICS, List.of(
                c("perfume", "Parfyumeriya", "Парфюмерия", "Perfume"),
                c("skincare", "Teri parvarishi", "Уход за кожей", "Skincare"),
                c("makeup", "Dekorativ kosmetika", "Декоративная косметика", "Makeup"),
                c("haircare", "Soch parvarishi", "Уход за волосами", "Hair care"),
                c("hygiene", "Gigiyena", "Гигиена", "Hygiene")));
        CATS.put(BusinessType.GENERAL, List.of(
                c("household", "Xo'jalik mollari", "Хозтовары", "Household"),
                c("stationery", "Kanselyariya", "Канцелярия", "Stationery"),
                c("toys", "O'yinchoqlar", "Игрушки", "Toys"),
                c("tools", "Asboblar", "Инструменты", "Tools"),
                OTHER));
    }
}