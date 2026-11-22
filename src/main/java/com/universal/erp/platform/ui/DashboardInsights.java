package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BusinessType.Capability;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Dashboard tahlillari: tushum (hisob-faktura + kassa savdosi), qarzdorlik, toifalar,
 * eng ko'p sotilganlar, yaroqlilik muddati, boshlash qadamlari.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/DashboardInsights.java
 */
@Service
public class DashboardInsights {

    public record Day(LocalDate date, double value) {
    }

    public record Trend(boolean available, double total, Double deltaPct, List<Day> days) {
    }

    public record Step(String label, String route, boolean done) {
    }

    public record Expiring(String name, int daysLeft) {
    }

    public record Bucket(String label, long count) {
    }

    public record Ranked(String name, double qty) {
    }

    public record Debt(String title, String sub, double amount) {
    }

    public record Info(boolean tracksExpiry, int expired, int soon, List<Expiring> expiring,
                       boolean debtKnown, double debtTotal, int debtCount, List<Debt> debts,
                       int productCount, List<Bucket> categories, List<Bucket> sizes,
                       List<Ranked> top, List<Step> steps,
                       String invoiceRoute, String receiptRoute, String outRoute) {
    }

    private static final int SOON_DAYS = 30;

    private final RecordStore store;
    private final TenantModules tenantModules;

    public DashboardInsights(RecordStore store, TenantModules tenantModules) {
        this.store = store;
        this.tenantModules = tenantModules;
    }

    // ================================================================ tushum

    /** Tushum = hisob-fakturalar (qoralamasiz) + kassa savdolari. */
    public Trend trend(String company, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        LocalDate prevStart = start.minusDays(days);

        Map<LocalDate, Double> byDay = new HashMap<>();
        double prevTotal = 0;

        List<RecordStore.Rec> rows = new ArrayList<>();
        rows.addAll(safeList(company, "finance/invoices"));
        rows.addAll(safeList(company, "pos/sales"));

        for (RecordStore.Rec r : rows) {
            if ("draft".equals(r.get("status"))) {
                continue;
            }
            LocalDate d = date(r.get("date"));
            if (d == null) {
                continue;
            }
            double amount = FieldDef.parse(r.get("amount"));
            if (!d.isBefore(start) && !d.isAfter(end)) {
                byDay.merge(d, amount, Double::sum);
            } else if (!d.isBefore(prevStart) && d.isBefore(start)) {
                prevTotal += amount;
            }
        }

        List<Day> list = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            double v = byDay.getOrDefault(d, 0.0);
            total += v;
            list.add(new Day(d, v));
        }
        Double delta = prevTotal > 0 ? (total - prevTotal) / prevTotal * 100.0 : null;
        return new Trend(true, total, delta, list);
    }

    // ================================================================ umumiy ma'lumot

    public Info load(String company, Set<BusinessType> types) {
        Set<AppModule> modules = tenantModules.enabled(company);
        boolean finance = modules.contains(AppModule.FINANCE);
        boolean inventory = modules.contains(AppModule.INVENTORY);
        boolean pos = modules.contains(AppModule.POS);

        boolean tracksExpiry = types != null && !types.isEmpty()
                && BusinessType.capabilitiesOf(types).contains(Capability.EXPIRY_TRACKING);

        List<RecordStore.Rec> products = safeList(company, "inventory/products");
        Map<String, Double> stock = new HashMap<>();
        for (RecordStore.Rec r : safeList(company, "inventory/stock")) {
            stock.put(norm(r.get("product")), FieldDef.parse(r.get("quantity")));
        }

        // ---- yaroqlilik muddati
        int expired = 0;
        int soon = 0;
        List<Expiring> expiring = new ArrayList<>();
        if (tracksExpiry) {
            LocalDate today = LocalDate.now();
            for (RecordStore.Rec r : products) {
                LocalDate exp = date(r.get("expiry_date"));
                if (exp == null) {
                    continue;
                }
                Double q = stock.get(norm(r.get("name")));
                if (q != null && q <= 0) {
                    continue; // qoldiq yo'q, ogohlantirish kerak emas
                }
                int left = (int) ChronoUnit.DAYS.between(today, exp);
                if (left < 0) {
                    expired++;
                } else if (left <= SOON_DAYS) {
                    soon++;
                } else {
                    continue;
                }
                expiring.add(new Expiring(r.get("name"), left));
            }
            expiring.sort(Comparator.comparingInt(Expiring::daysLeft));
            if (expiring.size() > 5) {
                expiring = new ArrayList<>(expiring.subList(0, 5));
            }
        }

        // ---- qarzdorlik
        boolean debtKnown = finance;
        double debtTotal = 0;
        int debtCount = 0;
        List<Debt> debts = new ArrayList<>();
        if (finance) {
            Map<String, String> customers = new HashMap<>();
            for (RecordStore.Rec c : safeList(company, "crm/customers")) {
                customers.put(c.id(), c.get("name"));
            }
            for (RecordStore.Rec r : safeList(company, "finance/invoices")) {
                String st = r.get("status");
                if (!"sent".equals(st) && !"overdue".equals(st)) {
                    continue;
                }
                double amount = FieldDef.parse(r.get("amount"));
                debtTotal += amount;
                debtCount++;
                String who = r.get("customer");
                who = customers.getOrDefault(who, who);
                String sub = r.get("number")
                        + (r.get("due").isBlank() ? "" : " · " + r.get("due"));
                debts.add(new Debt(who.isBlank() ? "—" : who, sub, amount));
            }
            debts.sort(Comparator.comparingDouble(Debt::amount).reversed());
            if (debts.size() > 5) {
                debts = new ArrayList<>(debts.subList(0, 5));
            }
        }

        // ---- toifalar va o'lchamlar
        Map<String, Long> cats = new LinkedHashMap<>();
        Map<String, Long> sizeCount = new LinkedHashMap<>();
        for (RecordStore.Rec r : products) {
            String c = r.get("category");
            if (!c.isBlank()) {
                cats.merge(c, 1L, Long::sum);
            }
            String s = r.get("size");
            if (!s.isBlank()) {
                sizeCount.merge(s.toUpperCase(Locale.ROOT), 1L, Long::sum);
            }
        }
        List<Bucket> categories = new ArrayList<>();
        cats.forEach((k, v) -> categories.add(new Bucket(categoryLabel(k), v)));
        categories.sort(Comparator.comparingLong(Bucket::count).reversed());
        List<Bucket> sizes = new ArrayList<>();
        sizeCount.forEach((k, v) -> sizes.add(new Bucket(k, v)));
        sizes.sort(Comparator.comparingLong(Bucket::count).reversed());

        return new Info(
                tracksExpiry, expired, soon, expiring,
                debtKnown, debtTotal, debtCount, debts,
                products.size(),
                limit(categories, 6), limit(sizes, 8),
                topSold(company), steps(company, modules, products.isEmpty()),
                finance ? "app/finance/invoices" : null,
                inventory ? "app/inventory/receipts" : null,
                inventory ? "app/inventory/issues" : null);
    }

    // ================================================================ eng ko'p sotilganlar

    /** Oxirgi 30 kunda sotilgan mahsulotlar (chiqimdagi sabab "sale"), miqdor bo'yicha. */
    private List<Ranked> topSold(String company) {
        LocalDate from = LocalDate.now().minusDays(29);
        Map<String, Double> sum = new LinkedHashMap<>();
        for (RecordStore.Rec r : safeList(company, "inventory/issues")) {
            if (!"sale".equals(r.get("reason"))) {
                continue;
            }
            LocalDate d = date(r.get("date"));
            if (d == null || d.isBefore(from)) {
                continue;
            }
            String name = r.get("product").trim();
            if (name.isEmpty()) {
                continue;
            }
            sum.merge(name, FieldDef.parse(r.get("quantity")), Double::sum);
        }
        List<Ranked> out = new ArrayList<>();
        sum.forEach((k, v) -> out.add(new Ranked(k, v)));
        out.sort(Comparator.comparingDouble(Ranked::qty).reversed());
        return limit(out, 5);
    }

    // ================================================================ boshlash qadamlari

    private List<Step> steps(String company, Set<AppModule> modules, boolean noProducts) {
        List<Step> s = new ArrayList<>();
        if (modules.contains(AppModule.INVENTORY)) {
            s.add(new Step(L.t("Birinchi mahsulotni qo'shing", "Добавьте первый товар", "Add your first product"),
                    "app/inventory/products", !noProducts));
            s.add(new Step(L.t("Kirim qiling", "Оформите приход", "Record a receipt"),
                    "app/inventory/receipts", !safeList(company, "inventory/receipts").isEmpty()));
        }
        if (modules.contains(AppModule.POS)) {
            s.add(new Step(L.t("Birinchi savdoni qiling", "Проведите первую продажу", "Make your first sale"),
                    "app/pos/terminal", !safeList(company, "pos/sales").isEmpty()));
        }
        return s;
    }

    // ================================================================ yordamchilar

    private List<RecordStore.Rec> safeList(String company, String key) {
        try {
            return store.list(company, key);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> List<T> limit(List<T> list, int n) {
        return list.size() > n ? new ArrayList<>(list.subList(0, n)) : list;
    }

    /** Toifa kalitini foydalanuvchi tilidagi nomga aylantiradi (noma'lum kalit o'zi qaytadi). */
    static String categoryLabel(String key) {
        return switch (key) {
            case "food" -> L.t("Oziq-ovqat", "Продукты", "Food");
            case "drinks" -> L.t("Ichimliklar", "Напитки", "Drinks");
            case "dairy" -> L.t("Sut mahsulotlari", "Молочные продукты", "Dairy");
            case "bakery" -> L.t("Non va qandolat", "Хлеб и выпечка", "Bakery");
            case "meat" -> L.t("Go'sht va baliq", "Мясо и рыба", "Meat and fish");
            case "produce" -> L.t("Meva va sabzavot", "Фрукты и овощи", "Fruit and vegetables");
            case "clothes" -> L.t("Kiyim", "Одежда", "Clothes");
            case "outerwear" -> L.t("Ustki kiyim", "Верхняя одежда", "Outerwear");
            case "underwear" -> L.t("Ichki kiyim", "Бельё", "Underwear");
            case "kids" -> L.t("Bolalar kiyimi", "Детская одежда", "Kids");
            case "shoes" -> L.t("Poyabzal", "Обувь", "Shoes");
            case "accessories" -> L.t("Aksessuarlar", "Аксессуары", "Accessories");
            case "electronics" -> L.t("Elektronika", "Электроника", "Electronics");
            case "phones" -> L.t("Telefonlar", "Телефоны", "Phones");
            case "appliances" -> L.t("Maishiy texnika", "Бытовая техника", "Appliances");
            case "computers" -> L.t("Kompyuterlar", "Компьютеры", "Computers");
            case "gadgets" -> L.t("Aksessuarlar", "Аксессуары", "Accessories");
            case "care" -> L.t("Parvarish", "Уход", "Skin care");
            case "makeup" -> L.t("Dekorativ kosmetika", "Декоративная косметика", "Makeup");
            case "perfume" -> L.t("Parfyumeriya", "Парфюмерия", "Perfume");
            case "hair" -> L.t("Soch parvarishi", "Уход за волосами", "Hair care");
            case "hygiene" -> L.t("Gigiyena", "Гигиена", "Hygiene");
            case "household" -> L.t("Xo'jalik mollari", "Хозтовары", "Household");
            case "stationery" -> L.t("Kantselyariya", "Канцтовары", "Stationery");
            case "tools" -> L.t("Asboblar", "Инструменты", "Tools");
            case "other" -> L.t("Boshqa", "Другое", "Other");
            default -> key;
        };
    }
}