package com.universal.erp.platform.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bosh sahifaning marketing matnlari: sarlavhalar, oldin/keyin, natija, CTA.
 * Tartib: uz, ru, en.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/HomeCopy.java
 */
public final class HomeCopy {

    private static final List<String> CODES = List.of("uz", "ru", "en");
    private static final Map<String, String[]> ROWS = new HashMap<>();

    private HomeCopy() {
    }

    public static String get(String key, String lang) {
        String[] r = ROWS.get(key);
        if (r == null) {
            return key;
        }
        int i = CODES.indexOf(lang);
        return r[i < 0 ? 0 : i];
    }

    private static void row(String key, String uz, String ru, String en) {
        ROWS.put(key, new String[] {uz, ru, en});
    }

    static {
        // ---------------- hero
        row("hero.headline", "Tartib bor joyda savdo o'sadi", "Где порядок — там растут продажи",
                "Where there's order, sales grow");
        row("hero.note", "Karta talab qilinmaydi. Birinchi filial bir necha daqiqada tayyor.",
                "Карта не нужна. Первый филиал готов за несколько минут.",
                "No card required. Your first branch is ready in minutes.");
        row("chip.1.t", "Skaner → kassa", "Сканер → касса", "Scanner → register");
        row("chip.1.d", "Telefon kamerasi mahsulotni kassaga yuboradi", "Камера телефона отправляет товар на кассу",
                "The phone camera sends the product to the register");
        row("chip.2.t", "Qoldiq kam", "Остаток мал", "Low stock");
        row("chip.2.d", "Sut 1 l: 12 dona qoldi", "Молоко 1 л: осталось 12 шт.", "Milk 1 l: 12 left");

        // ---------------- bo'lim yorliqlari
        row("eb.why", "Nega bu tizim", "Зачем это нужно", "Why this");
        row("eb.features", "Imkoniyatlar", "Возможности", "Features");
        row("eb.outcome", "Natija", "Результат", "Results");
        row("eb.process", "Savdo jarayoni", "Процесс продаж", "Sales flow");
        row("eb.platform", "Platforma", "Платформа", "Platform");
        row("eb.start", "Boshlash", "Начало работы", "Getting started");

        // ---------------- oldin / keyin
        row("shift.title", "Daftar va Excel o'rniga bitta tizim",
                "Одна система вместо тетрадей и Excel",
                "One system instead of notebooks and spreadsheets");
        row("shift.sub", "Tarmoq kengayganda qo'lda yuritilgan hisob birinchi bo'lib buziladi. Mana nima o'zgaradi.",
                "Когда сеть растёт, ручной учёт ломается первым. Вот что меняется.",
                "When a chain grows, manual bookkeeping breaks first. Here is what changes.");
        row("shift.before", "Hozir", "Сейчас", "Now");
        row("shift.after", "DOKONIX bilan", "С DOKONIX", "With DOKONIX");

        row("b1", "Narxlar har bir filialda alohida daftarda", "Цены в каждом филиале в своей тетради",
                "Prices live in a separate notebook at each branch");
        row("b2", "Qoldiq sanab chiqilmaguncha noma'lum", "Остаток неизвестен, пока не пересчитаешь",
                "Stock is unknown until someone counts it");
        row("b3", "Kassada mahsulotni qo'lda qidirish", "Поиск товара на кассе вручную",
                "Searching for products by hand at the register");
        row("b4", "Qaysi filial foyda qilayotgani oy oxirida ma'lum bo'ladi",
                "Какой филиал прибыльный, понятно только в конце месяца",
                "You learn which branch earns money at month end");
        row("b5", "Yaroqlilik muddati va partiyalar yodda saqlanadi", "Сроки годности и партии держат в голове",
                "Expiry dates and batches are kept in someone's head");

        row("a1", "Har bir filialning narxi bitta joyda boshqariladi", "Цены всех филиалов управляются в одном месте",
                "Every branch's prices are managed in one place");
        row("a2", "Qoldiq har bir kirim va sotuvdan o'zi hisoblanadi", "Остаток считается сам из прихода и продаж",
                "Stock is calculated from every receipt and sale");
        row("a3", "Skaner yoki telefon kamerasi: mahsulot kassada", "Сканер или камера телефона: товар уже на кассе",
                "Scanner or phone camera: the product is on the register");
        row("a4", "Filiallar savdosi har kuni bitta jadvalda", "Продажи филиалов каждый день в одной таблице",
                "Branch sales in one table, every day");
        row("a5", "Yaroqlilik muddati mahsulot kartasida saqlanadi", "Срок годности хранится в карточке товара",
                "Expiry dates are stored on the product card");

        // ---------------- natija
        row("out.title", "Egasi uchun aniq ko'rinish", "Ясная картина для владельца", "A clear picture for owners");
        row("out.sub", "Tizim uchta narsani hal qiladi: tartib, nazorat va tezlik.",
                "Система решает три задачи: порядок, контроль и скорость.",
                "The system solves three things: order, control and speed.");
        row("out.1.t", "Tartibli hisob", "Порядок в учёте", "Orderly records");
        row("out.1.d", "Har bir kirim, sotuv va qaytarish tarixda saqlanadi. Hech narsa jimgina o'zgarmaydi.",
                "Каждый приход, продажа и возврат хранятся в истории. Ничего не меняется незаметно.",
                "Every receipt, sale and return stays in the history. Nothing changes silently.");
        row("out.2.t", "Qoldiq ustidan nazorat", "Контроль остатков", "Control over stock");
        row("out.2.d", "Qoldiq harakatlardan hisoblanadi, shuning uchun kamomad sababini topish mumkin.",
                "Остаток считается по движениям, поэтому причину недостачи можно найти.",
                "Stock comes from movements, so the cause of a shortage can be traced.");
        row("out.3.t", "Tez kassa", "Быстрая касса", "A fast register");
        row("out.3.d", "Kassir mahsulotni skanerlaydi, tizim nomi va narxini o'zi topadi.",
                "Кассир сканирует товар, система сама находит название и цену.",
                "The cashier scans, the system finds the name and price on its own.");

        // ---------------- CTA va footer
        row("cta2.title", "Birinchi filialingizni bugun oching", "Откройте первый филиал уже сегодня",
                "Open your first branch today");
        row("cta2.sub", "Ro'yxatdan o'ting, mahsulotlarni qo'shing va kassani ishga tushiring.",
                "Зарегистрируйтесь, добавьте товары и запустите кассу.",
                "Sign up, add your products and start the register.");
        row("cta2.note", "Sozlash uchun dasturchi kerak emas.", "Для настройки программист не нужен.",
                "No developer needed to set it up.");
        row("foot.tag", "Chakana savdo tarmoqlari uchun kassa, ombor va hisobot tizimi.",
                "Касса, склад и отчёты для розничных сетей.",
                "Register, stock and reporting for retail chains.");
    }
}