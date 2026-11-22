package com.universal.erp.platform.ui;

import java.util.Map;

public final class PosTexts {

    private static final Map<String, String[]> M = Map.ofEntries(
            Map.entry("title", new String[] {"POS skaner", "POS сканер", "POS scanner"}),
            Map.entry("scan.placeholder", new String[] {"Shtrix-kodni skanerlang yoki kiriting", "Отсканируйте или введите штрихкод", "Scan or type a barcode"}),
            Map.entry("scan.hint", new String[] {"Skanerga tayyor", "Готов к сканированию", "Ready to scan"}),
            Map.entry("ready", new String[] {"Mahsulotni skanerlang", "Отсканируйте товар", "Scan a product"}),
            Map.entry("cart", new String[] {"Savat", "Корзина", "Cart"}),
            Map.entry("empty", new String[] {"Savat bo'sh. Mahsulotni skanerlang.", "Корзина пуста. Отсканируйте товар.", "Cart is empty. Scan a product."}),
            Map.entry("added", new String[] {"Qo'shildi: {0}", "Добавлено: {0}", "Added: {0}"}),
            Map.entry("inc", new String[] {"{0} — soni: {1}", "{0} — количество: {1}", "{0} — quantity: {1}"}),
            Map.entry("notfound", new String[] {"Mahsulot topilmadi: {0}", "Товар не найден: {0}", "Product not found: {0}"}),
            Map.entry("total", new String[] {"Jami summa", "Итого", "Total"}),
            Map.entry("positions", new String[] {"Pozitsiyalar", "Позиции", "Items"}),
            Map.entry("units", new String[] {"Jami miqdor", "Общее количество", "Total quantity"}),
            Map.entry("clear", new String[] {"Savatni tozalash", "Очистить корзину", "Clear cart"}),
            Map.entry("remove", new String[] {"O'chirish", "Удалить", "Remove"}),
            Map.entry("sale", new String[] {"Savdo №", "Продажа №", "Sale #"}),
            Map.entry("method", new String[] {"To'lov usuli", "Способ оплаты", "Payment method"}),
            Map.entry("cash", new String[] {"Naqd", "Наличные", "Cash"}),
            Map.entry("card", new String[] {"Karta", "Карта", "Card"}),
            Map.entry("received", new String[] {"Qabul qilindi", "Принято", "Received"}),
            Map.entry("change", new String[] {"Qaytim", "Сдача", "Change"}),
            Map.entry("exact", new String[] {"Aniq summa", "Без сдачи", "Exact"}),
            Map.entry("close", new String[] {"Savdoni yopish", "Закрыть продажу", "Close sale"}),
            Map.entry("closed", new String[] {"✓ Savdo № {0} yopildi · {1} UZS · {2}", "✓ Продажа № {0} закрыта · {1} UZS · {2}", "✓ Sale #{0} closed · {1} UZS · {2}"}),
            Map.entry("nocart", new String[] {"Savat bo'sh", "Корзина пуста", "Cart is empty"}),
            Map.entry("short", new String[] {"Qabul qilingan summa yetarli emas", "Принятой суммы недостаточно", "Received amount is not enough"}),
            Map.entry("denied", new String[] {"Savdoni yopishga ruxsat yo'q", "Нет прав на закрытие продажи", "No permission to close sales"}),
            Map.entry("phone", new String[] {"Telefonni ulash", "Подключить телефон", "Connect phone"}),
            Map.entry("error", new String[] {"Savdoni saqlab bo'lmadi", "Не удалось сохранить продажу", "Could not save the sale"}));

    private PosTexts() {
    }

    public static String t(String key) {
        String[] v = M.get(key);
        return v == null ? key : v[AppTexts.index()];
    }

    public static String t(String key, Object... args) {
        String s = t(key);
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return s;
    }
}