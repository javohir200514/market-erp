package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.AppTexts;
import java.util.EnumSet;
import java.util.Set;

/**
 * Do'kon yo'nalishi. Kompaniya bir nechtasini tanlashi mumkin.
 * Joylashuvi: src/main/java/com/universal/erp/retail/business/BusinessType.java
 */
public enum BusinessType {

    FOOD("cart",
            new String[] {"Oziq-ovqat", "Продукты питания", "Food"},
            new String[] {"Oziq-ovqat va ichimliklar do'koni", "Магазин продуктов и напитков", "Groceries and beverages"},
            new String[] {"Yaroqlilik muddati · Partiya · Vaznli tovar", "Срок годности · Партия · Весовой товар", "Expiry · Batch · Weighted items"},
            EnumSet.of(Capability.BATCH_TRACKING, Capability.EXPIRY_TRACKING, Capability.WEIGHT_PRODUCTS)),

    CLOTHING("layers",
            new String[] {"Kiyim-kechak", "Одежда", "Clothing"},
            new String[] {"Kiyim, poyabzal va aksessuarlar", "Одежда, обувь и аксессуары", "Clothes, shoes and accessories"},
            new String[] {"O'lcham · Rang · Material", "Размер · Цвет · Материал", "Size · Color · Material"},
            EnumSet.of(Capability.SIZE_VARIANTS, Capability.COLOR_VARIANTS, Capability.MATERIAL_ATTRIBUTES)),

    ELECTRONICS("plug",
            new String[] {"Elektronika", "Электроника", "Electronics"},
            new String[] {"Telefon, maishiy texnika va aksessuarlar", "Телефоны, бытовая техника и аксессуары", "Phones, appliances and accessories"},
            new String[] {"Seriya raqami · IMEI · Kafolat", "Серийный номер · IMEI · Гарантия", "Serial · IMEI · Warranty"},
            EnumSet.of(Capability.SERIAL_TRACKING, Capability.WARRANTY_TRACKING)),

    COSMETICS("check-circle",
            new String[] {"Kosmetika", "Косметика", "Cosmetics"},
            new String[] {"Parfyumeriya va parvarish vositalari", "Парфюмерия и средства ухода", "Perfume and care products"},
            new String[] {"Hajm · Tarkib · Yaroqlilik muddati", "Объём · Состав · Срок годности", "Volume · Ingredients · Expiry"},
            EnumSet.of(Capability.VOLUME_ATTRIBUTES, Capability.INGREDIENTS, Capability.EXPIRY_TRACKING,
                    Capability.BATCH_TRACKING)),

    GENERAL("inventory",
            new String[] {"Umumiy tovarlar", "Общие товары", "General goods"},
            new String[] {"Xo'jalik va boshqa aralash tovarlar", "Хозтовары и смешанный ассортимент", "Household and mixed goods"},
            new String[] {"Brend · Kategoriya · Birlik", "Бренд · Категория · Единица", "Brand · Category · Unit"},
            EnumSet.of(Capability.STANDARD_CATALOG));

    /** Yo'nalishga bog'liq qo'shimcha imkoniyatlar. Backend shularni tekshiradi, UI yashirishi yetarli emas. */
    public enum Capability {
        BATCH_TRACKING, EXPIRY_TRACKING, WEIGHT_PRODUCTS,
        SIZE_VARIANTS, COLOR_VARIANTS, MATERIAL_ATTRIBUTES,
        SERIAL_TRACKING, WARRANTY_TRACKING,
        VOLUME_ATTRIBUTES, INGREDIENTS,
        STANDARD_CATALOG
    }

    private final String icon;
    private final String[] title;
    private final String[] desc;
    private final String[] features;
    private final Set<Capability> capabilities;

    BusinessType(String icon, String[] title, String[] desc, String[] features, Set<Capability> capabilities) {
        this.icon = icon;
        this.title = title;
        this.desc = desc;
        this.features = features;
        this.capabilities = capabilities;
    }

    public String icon() {
        return icon;
    }

    public String title() {
        return title[AppTexts.index()];
    }

    public String description() {
        return desc[AppTexts.index()];
    }

    public String features() {
        return features[AppTexts.index()];
    }

    public Set<Capability> capabilities() {
        return capabilities;
    }

    /** Tanlangan yo'nalishlar birlashmasi (takrorlanmaydi). */
    public static Set<Capability> capabilitiesOf(Set<BusinessType> types) {
        Set<Capability> all = EnumSet.noneOf(Capability.class);
        for (BusinessType t : types) {
            all.addAll(t.capabilities);
        }
        return all;
    }

    public static boolean has(Set<BusinessType> types, Capability c) {
        return capabilitiesOf(types).contains(c);
    }
}