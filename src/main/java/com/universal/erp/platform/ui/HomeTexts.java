package com.universal.erp.platform.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bosh sahifa matnlari (navigatsiya, modullar, jarayon, platforma, footer).
 * Tartib: uz, ru, en.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/HomeTexts.java
 */
public final class HomeTexts {

    private static final List<String> CODES = List.of("uz", "ru", "en");
    private static final Map<String, String[]> ROWS = new HashMap<>();

    private HomeTexts() {
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
        // ---------------- navigatsiya
        row("nav.home", "Bosh sahifa", "Главная", "Home");
        row("nav.features", "Imkoniyatlar", "Возможности", "Features");
        row("nav.why", "Nega biz", "Почему мы", "Why us");
        row("nav.outcome", "Natija", "Результат", "Results");
        row("nav.process", "Jarayon", "Процесс", "Workflow");
        row("nav.platform", "Platforma", "Платформа", "Platform");
        row("nav.start", "Boshlash", "Начать", "Get started");
        row("nav.login", "Kirish", "Войти", "Sign in");
        row("nav.register", "Ro'yxatdan o'tish", "Регистрация", "Sign up");
        row("nav.menu", "Menyu", "Меню", "Menu");
        row("lang.label", "Til", "Язык", "Language");

        // ---------------- hero
        row("hero.sub",
                "Kassa, ombor, filiallar va narxlar bitta tizimda. Skanerlang, soting va qoldiqni real vaqtda ko'ring.",
                "Касса, склад, филиалы и цены в одной системе. Сканируйте, продавайте и видьте остатки в реальном времени.",
                "Register, stock, branches and prices in one system. Scan, sell and see inventory in real time.");
        row("hero.cta1", "Bepul boshlash", "Начать бесплатно", "Start free");
        row("hero.cta2", "Tizimga kirish", "Войти в систему", "Sign in");

        row("trust.1", "Ko'p filial, bitta hisob", "Много филиалов — один аккаунт", "Many branches, one account");
        row("trust.2", "Telefon skaneri kassa bilan ishlaydi", "Сканер на телефоне работает с кассой",
                "Phone scanner works with the register");
        row("trust.3", "Kompaniyalar ma'lumoti bir-biridan ajratilgan", "Данные компаний полностью изолированы",
                "Company data is fully isolated");

        // ---------------- namunaviy chek va jadval
        row("mock.store", "Chilonzor filiali · 2-kassa", "Филиал Чиланзар · касса 2", "Chilonzor branch · register 2");
        row("mock.p1", "Non", "Хлеб", "Bread");
        row("mock.p2", "Sut 1 l", "Молоко 1 л", "Milk 1 l");
        row("mock.p3", "Coca-Cola 1.5 l", "Coca-Cola 1.5 л", "Coca-Cola 1.5 l");
        row("mock.total", "Jami", "Итого", "Total");
        row("mock.cash", "Naqd", "Наличные", "Cash");
        row("mock.change", "Qaytim", "Сдача", "Change");
        row("mock.cur", "so'm", "сум", "UZS");
        row("mock.mln", "mln", "млн", "M");
        row("mock.branches", "Bugungi savdo, filiallar bo'yicha", "Продажи за сегодня по филиалам",
                "Today's sales by branch");
        row("mock.b1", "Chilonzor", "Чиланзар", "Chilonzor");
        row("mock.b2", "Yunusobod", "Юнусабад", "Yunusabad");
        row("mock.b3", "Sergeli", "Сергели", "Sergeli");

        // ---------------- imkoniyatlar
        row("mod.title", "Do'kon uchun kerakli hamma narsa", "Всё, что нужно магазину",
                "Everything a store needs");
        row("mod.sub", "Beshta yo'nalish bir-biri bilan bog'langan, ma'lumot ikki marta kiritilmaydi.",
                "Пять направлений связаны между собой, данные не нужно вводить дважды.",
                "Five areas work together, so data is never entered twice.");

        row("m1.t", "Kassa (POS)", "Касса (POS)", "Register (POS)");
        row("m1.d", "Skaner bilan tez savdo, naqd va karta, chek va qaytarish.",
                "Быстрая продажа со сканером, наличные и карта, чек и возврат.",
                "Fast scanner sales, cash and card, receipts and returns.");
        row("m1.b1", "Shtrix-kod va telefon-skaner", "Штрихкод и сканер на телефоне", "Barcode and phone scanner");
        row("m1.b2", "Naqd, karta va qaytim hisobi", "Наличные, карта и расчёт сдачи", "Cash, card and change");
        row("m1.b3", "Mahsulot nomini ovozda e'lon qilish", "Озвучивание названия товара", "Spoken product names");
        row("m1.b4", "Savdoni qaytarish va chek", "Возврат продажи и чек", "Sale returns and receipts");

        row("m2.t", "Mahsulotlar", "Товары", "Products");
        row("m2.d", "Kompaniyaning yagona katalogi: kategoriya, brend va variantlar.",
                "Единый каталог компании: категории, бренды и варианты.",
                "One company catalog: categories, brands and variants.");
        row("m2.b1", "Bir mahsulotga bir nechta shtrix-kod", "Несколько штрихкодов на товар",
                "Several barcodes per product");
        row("m2.b2", "Rang, o'lcham, hajm atributlari", "Атрибуты: цвет, размер, объём", "Color, size, volume attributes");
        row("m2.b3", "Yaroqlilik muddati va partiya", "Срок годности и партия", "Expiry date and batch");
        row("m2.b4", "Barcha filiallar uchun umumiy katalog", "Общий каталог для всех филиалов",
                "Shared catalog for all branches");

        row("m3.t", "Ombor", "Склад", "Inventory");
        row("m3.d", "Qoldiq harakatlardan hisoblanadi va qo'lda o'zgartirilmaydi.",
                "Остаток считается по движениям и не правится вручную.",
                "Stock is calculated from movements, never edited by hand.");
        row("m3.b1", "Kirim, chiqim va qaytarish", "Приход, расход и возврат", "Receipts, issues and returns");
        row("m3.b2", "Omborlar o'rtasida o'tkazma", "Перемещение между складами", "Transfers between warehouses");
        row("m3.b3", "Inventarizatsiya va hisobdan chiqarish", "Инвентаризация и списание",
                "Stocktake and write-offs");
        row("m3.b4", "Minimal qoldiq belgilash", "Минимальный остаток", "Minimum stock levels");

        row("m4.t", "Filiallar va narxlar", "Филиалы и цены", "Branches and prices");
        row("m4.d", "Har bir filialning o'z assortimenti va o'z narxi bor.",
                "У каждого филиала свой ассортимент и свои цены.",
                "Each branch has its own range and its own prices.");
        row("m4.b1", "Mahsulotni filialga biriktirish", "Привязка товара к филиалу", "Assign products to branches");
        row("m4.b2", "Filial bo'yicha alohida narx", "Отдельная цена по филиалу", "Per-branch pricing");
        row("m4.b3", "Bir nechta kassa va ombor", "Несколько касс и складов", "Several registers and warehouses");
        row("m4.b4", "Filiallar savdosini taqqoslash", "Сравнение продаж филиалов", "Compare branch sales");

        row("m5.t", "Hisobotlar va moliya", "Отчёты и финансы", "Reports and finance");
        row("m5.d", "Savdo, foyda va xarajat kompaniya va filial kesimida.",
                "Продажи, прибыль и расходы по компании и филиалам.",
                "Sales, profit and expenses by company and branch.");
        row("m5.b1", "Kunlik va oylik savdo", "Продажи по дням и месяцам", "Daily and monthly sales");
        row("m5.b2", "Foyda va rentabellik", "Прибыль и рентабельность", "Profit and margin");
        row("m5.b3", "Xarajatlar va to'lovlar", "Расходы и платежи", "Expenses and payments");
        row("m5.b4", "Kassirlar bo'yicha hisobot", "Отчёт по кассирам", "Cashier reports");

        // ---------------- savdo jarayoni
        row("flow.title", "Tovar kelganidan sotilgunicha", "От поступления товара до продажи",
                "From delivery to sale");
        row("flow.sub", "Har bir qadam tizimda qayd etiladi.", "Каждый шаг фиксируется в системе.",
                "Every step is recorded in the system.");
        row("f1.t", "Xarid", "Закупка", "Purchasing");
        row("f1.d", "Yetkazib beruvchidan buyurtma va hisob-faktura.", "Заказ и счёт от поставщика.",
                "Supplier order and invoice.");
        row("f2.t", "Kirim", "Приход", "Receiving");
        row("f2.d", "Tovar omborga qabul qilinadi, qoldiq oshadi.", "Товар принимается на склад, остаток растёт.",
                "Goods are received and stock goes up.");
        row("f3.t", "Narx", "Цена", "Pricing");
        row("f3.d", "Har bir filial uchun sotuv narxi belgilanadi.", "Для каждого филиала задаётся цена продажи.",
                "A selling price is set for each branch.");
        row("f4.t", "Kassa", "Касса", "Register");
        row("f4.d", "Kassir skanerlaydi, mijoz to'laydi.", "Кассир сканирует, покупатель платит.",
                "The cashier scans, the customer pays.");
        row("f5.t", "Chek", "Чек", "Receipt");
        row("f5.d", "Savdo narx va miqdori bilan saqlanadi.", "Продажа сохраняется с ценой и количеством.",
                "The sale is saved with price and quantity.");
        row("f6.t", "Hisobot", "Отчёт", "Report");
        row("f6.d", "Savdo va foyda filial kesimida ko'rinadi.", "Продажи и прибыль видны по филиалам.",
                "Sales and profit are visible per branch.");

        // ---------------- platforma
        row("plat.title", "Tarmoq o'sishi uchun qurilgan", "Создано для роста сети", "Built for a growing chain");
        row("plat.sub", "Bitta kompaniya, ko'p filial, ko'p foydalanuvchi: xavfsiz va tartibli.",
                "Одна компания, много филиалов и пользователей: безопасно и упорядоченно.",
                "One company, many branches, many users: safe and orderly.");
        row("p1.t", "Rollar va ruxsatlar", "Роли и права", "Roles and permissions");
        row("p1.d", "Kassir, omborchi va menejer faqat o'z ishini ko'radi.",
                "Кассир, кладовщик и менеджер видят только своё.",
                "Cashiers, storekeepers and managers only see their own work.");
        row("p2.t", "Ko'p filial", "Много филиалов", "Many branches");
        row("p2.d", "Har bir filialning o'z kassasi, ombori va narxi bor.",
                "У каждого филиала своя касса, склад и цены.",
                "Every branch has its own register, warehouse and prices.");
        row("p3.t", "O'zgarishlar tarixi", "История изменений", "Change history");
        row("p3.d", "Kim nimani va qachon o'zgartirgani saqlanadi.", "Сохраняется, кто, что и когда изменил.",
                "Who changed what, and when, is kept.");
        row("p4.t", "Ma'lumot xavfsizligi", "Безопасность данных", "Data security");
        row("p4.d", "Kompaniyalar ma'lumoti bir-biridan to'liq ajratilgan.",
                "Данные компаний полностью изолированы друг от друга.",
                "Company data is fully separated.");
        row("p5.t", "Telefon skaneri", "Сканер на телефоне", "Phone scanner");
        row("p5.d", "Android ilova shtrix-kodni kassaga yuboradi.", "Android-приложение отправляет штрихкод на кассу.",
                "The Android app sends barcodes to the register.");
        row("p6.t", "Uch tilda", "На трёх языках", "In three languages");
        row("p6.d", "O'zbek, rus va ingliz tillarida ishlaydi.", "Работает на узбекском, русском и английском.",
                "Works in Uzbek, Russian and English.");

        // ---------------- boshlash
        row("start.title", "Uch qadamda ishga tushiring", "Запуск за три шага", "Up and running in three steps");
        row("start.sub", "Murakkab sozlash va o'qitish talab qilinmaydi.", "Сложная настройка и обучение не нужны.",
                "No complex setup or training required.");
        row("s1.t", "Ro'yxatdan o'ting", "Зарегистрируйтесь", "Sign up");
        row("s1.d", "Kompaniya, administrator va birinchi filial ma'lumotlarini kiriting.",
                "Укажите данные компании, администратора и первого филиала.",
                "Enter your company, administrator and first branch.");
        row("s2.t", "Mahsulotlarni qo'shing", "Добавьте товары", "Add products");
        row("s2.d", "Katalogni yarating va filiallar uchun narxlarni belgilang.",
                "Создайте каталог и задайте цены для филиалов.",
                "Build the catalog and set prices for each branch.");
        row("s3.t", "Savdoni boshlang", "Начните продавать", "Start selling");
        row("s3.d", "Kassani oching va birinchi shtrix-kodni skanerlang.",
                "Откройте кассу и отсканируйте первый штрихкод.",
                "Open the register and scan your first barcode.");

        // ---------------- footer
        row("foot.product", "Imkoniyatlar", "Возможности", "Features");
        row("foot.platform", "Platforma", "Платформа", "Platform");
        row("foot.account", "Hisob", "Аккаунт", "Account");
        row("foot.rights", "Barcha huquqlar himoyalangan.", "Все права защищены.", "All rights reserved.");
    }
}