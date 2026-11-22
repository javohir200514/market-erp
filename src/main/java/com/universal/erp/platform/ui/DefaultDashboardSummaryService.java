package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.WarehouseProfile.Field;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Dashboard ma'lumotlari (kam qolgan, yangi qo'shilgan/muddati yaqin, oxirgi harakatlar, modul kartalari).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/DefaultDashboardSummaryService.java
 */
@Service
public class DefaultDashboardSummaryService implements DashboardSummaryService {

    private static final double DEFAULT_LOW_STOCK_THRESHOLD = 5.0;
    private static final int EXPIRY_WARNING_DAYS = 14;
    private static final int LIST_LIMIT = 6;

    private record ExpiryCandidate(long daysLeft, ProfileHighlightItem item) {
    }

    private record Activity(LocalDate date, int order, RecentActivityItem item) {
    }

    private final RecordStore recordStore;
    private final ObjectProvider<WarehouseProfiles> profilesProvider;

    public DefaultDashboardSummaryService(
            RecordStore recordStore,
            ObjectProvider<WarehouseProfiles> profilesProvider
    ) {
        this.recordStore = recordStore;
        this.profilesProvider = profilesProvider;
    }

    // ================================================================ yordamchilar

    private static boolean isMeaningful(String val) {
        if (val == null) {
            return false;
        }
        String t = val.trim();
        return !t.isEmpty() && !t.equals("—") && !t.equals("-") && !t.equalsIgnoreCase("null");
    }

    private static String safeGet(RecordStore.Rec rec, String key) {
        try {
            String v = rec.get(key);
            if (isMeaningful(v)) {
                return v.trim();
            }
        } catch (RuntimeException ignored) {
            // maydon yo'q bo'lsa bo'sh qaytadi
        }
        return "";
    }

    private List<RecordStore.Rec> rows(String company, AppSection section) {
        if (section == null) {
            return List.of();
        }
        try {
            return recordStore.list(company, section.key());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private WarehouseProfile resolveProfile(String company) {
        WarehouseProfiles profiles = profilesProvider.getIfAvailable();
        if (profiles != null && company != null && !company.isBlank()) {
            try {
                return profiles.forCompany(company);
            } catch (RuntimeException ignored) {
                // profil yo'q
            }
        }
        return null;
    }

    private CatalogTerms.Terms resolveTerms(String company) {
        WarehouseProfiles profiles = profilesProvider.getIfAvailable();
        if (profiles != null && company != null && !company.isBlank()) {
            try {
                return profiles.termsFor(company);
            } catch (RuntimeException ignored) {
                // nomlar yo'q
            }
        }
        return null;
    }

    private static String route(AppModule module, AppSection section) {
        return "/app/" + module.key() + "/" + section.slug();
    }

    // ================================================================ kam qolgan

    @Override
    public List<LowStockItem> loadLowStockItems(String company, List<AppModule> modules) {
        String defaultUnit = resolveDefaultUnit(company);

        // mahsulot kartasidan rasm va o'lchov birligi
        Map<String, String> productImages = new HashMap<>();
        Map<String, String> productUnits = new HashMap<>();
        AppSection prodSec = AppSection.get(AppModule.INVENTORY, "products");
        for (RecordStore.Rec pr : rows(company, prodSec)) {
            String pName = safeGet(pr, "name").toLowerCase(Locale.ROOT);
            if (pName.isEmpty()) {
                continue;
            }
            String img = safeGet(pr, "image");
            if (!img.isEmpty()) {
                productImages.put(pName, img);
            }
            String unit = safeGet(pr, "unit");
            if (!unit.isEmpty()) {
                productUnits.put(pName, unit);
            }
        }

        // bir tovar bir marta chiqadi (eng kam qoldig'i bilan)
        Map<String, LowStockItem> byName = new LinkedHashMap<>();
        for (AppModule module : modules) {
            for (AppSection section : AppSection.of(module)) {
                if (section.isReport() || !isStockSection(section)) {
                    continue;
                }
                for (RecordStore.Rec r : rows(company, section)) {
                    Double qty = extractQuantity(r, section);
                    if (qty == null) {
                        continue;
                    }
                    Double threshold = extractThreshold(r, section);
                    double limit = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
                    if (qty > limit) {
                        continue;
                    }
                    String name = extractPrimaryName(r, section);
                    String key = name.toLowerCase(Locale.ROOT);
                    LowStockItem old = byName.get(key);
                    if (old != null && old.quantity() <= qty) {
                        continue;
                    }

                    String extra = extractSecondaryInfo(r, section);
                    String detail = module.title() + " • " + (extra.isBlank() ? section.title() : extra);
                    String unit = unitLabel(extractUnitRaw(r, section, productUnits.get(key)), defaultUnit);
                    String imageUrl = extractImageUrl(r, section);
                    if (imageUrl.isBlank()) {
                        imageUrl = productImages.getOrDefault(key, "");
                    }
                    byName.put(key, new LowStockItem(name, detail, imageUrl,
                            resolveItemIconKey(name, extra, section), qty, unit, route(module, section)));
                }
            }
        }

        List<LowStockItem> items = new ArrayList<>(byName.values());
        items.sort(Comparator.comparingDouble(LowStockItem::quantity));
        return items.size() > LIST_LIMIT ? new ArrayList<>(items.subList(0, LIST_LIMIT)) : items;
    }

    // ================================================================ do'kon turiga xos panel

    @Override
    public ProfileHighlightGroup loadProfileHighlights(String company, List<AppModule> modules) {
        WarehouseProfile profile = resolveProfile(company);
        CatalogTerms.Terms terms = resolveTerms(company);
        boolean tracksExpiry = profile != null && profile.has(Field.EXPIRY_DATE);

        AppSection prodSec = AppSection.get(AppModule.INVENTORY, "products");
        List<RecordStore.Rec> products = rows(company, prodSec);

        // 1) yaroqlilik muddati kuzatiladigan do'konlar: muddati yaqinlar
        if (tracksExpiry) {
            LocalDate today = LocalDate.now();
            List<ExpiryCandidate> scored = new ArrayList<>();

            for (RecordStore.Rec r : products) {
                String rawExp = safeGet(r, "expiry_date");
                if (rawExp.isEmpty()) {
                    continue;
                }
                LocalDate expDate;
                try {
                    expDate = LocalDate.parse(rawExp.substring(0, Math.min(10, rawExp.length())));
                } catch (RuntimeException ex) {
                    continue;
                }

                long daysLeft = ChronoUnit.DAYS.between(today, expDate);
                if (daysLeft > EXPIRY_WARNING_DAYS) {
                    continue;
                }
                String name = extractPrimaryName(r, prodSec);
                String cat = safeGet(r, "category");
                String catLabel = cat.isEmpty() ? "" : CatalogTerms.categoryLabel(cat);
                String detail = (catLabel.isEmpty() ? "" : catLabel + " • ")
                        + L.t("Muddati: ", "Срок: ", "Expires: ") + expDate;

                String badge;
                String tone;
                if (daysLeft < 0) {
                    badge = L.t("Muddati o'tgan", "Просрочено", "Expired");
                    tone = "bad";
                } else if (daysLeft == 0) {
                    badge = L.t("Bugun tugaydi", "Истекает сегодня", "Expires today");
                    tone = "bad";
                } else {
                    badge = daysLeft + " " + L.t("kun qoldi", "дн. осталось", "days left");
                    tone = daysLeft <= 3 ? "bad" : "warn";
                }
                scored.add(new ExpiryCandidate(daysLeft, new ProfileHighlightItem(
                        name, detail, safeGet(r, "image"), resolveItemIconKey(name, cat, prodSec),
                        badge, tone, "/app/inventory/products")));
            }

            scored.sort(Comparator.comparingLong(ExpiryCandidate::daysLeft));
            List<ProfileHighlightItem> items = new ArrayList<>();
            for (int i = 0; i < scored.size() && i < LIST_LIMIT; i++) {
                items.add(scored.get(i).item());
            }
            return new ProfileHighlightGroup(
                    L.t("Yaroqlilik muddati yaqin", "Истекает срок годности", "Expiring soon"),
                    items.size() + " " + L.t("ta", "шт", "items"),
                    items.isEmpty() ? "ok" : "warn",
                    L.t("Yaqin " + EXPIRY_WARNING_DAYS + " kun ichida muddati tugaydigan mahsulot yo'q.",
                            "Нет товаров с истекающим сроком в ближайшие " + EXPIRY_WARNING_DAYS + " дней.",
                            "No items expiring within " + EXPIRY_WARNING_DAYS + " days."),
                    items);
        }

        // 2) boshqa do'konlar: eng so'nggi qo'shilgan tovarlar
        List<ProfileHighlightItem> recentCatalog = new ArrayList<>();
        for (int i = products.size() - 1; i >= 0 && recentCatalog.size() < LIST_LIMIT; i--) {
            RecordStore.Rec r = products.get(i);
            String name = extractPrimaryName(r, prodSec);

            List<String> meta = new ArrayList<>();
            String cat = safeGet(r, "category");
            if (!cat.isEmpty()) {
                meta.add(CatalogTerms.categoryLabel(cat));
            }
            if (profile != null) {
                for (Field f : new Field[] {Field.SIZE, Field.COLOR}) {
                    if (profile.has(f)) {
                        String val = safeGet(r, CatalogTerms.attrKey(f));
                        if (!val.isEmpty()) {
                            meta.add(val);
                        }
                    }
                }
            }
            String detail = meta.isEmpty()
                    ? (terms != null ? terms.title() : L.t("Katalog", "Каталог", "Catalog"))
                    : String.join(" • ", meta);

            double price = FieldDef.parse(safeGet(r, "price"));
            String badge = price > 0
                    ? FieldDef.format(price) + " " + resolveCurrencyLabel()
                    : L.t("Yangi", "Новый", "New");

            recentCatalog.add(new ProfileHighlightItem(name, detail, safeGet(r, "image"),
                    resolveItemIconKey(name, cat, prodSec), badge, "info", "/app/inventory/products"));
        }

        String sectionTitle = terms != null
                ? L.t("Yangi qo'shilgan: ", "Новые позиции: ", "Latest: ") + terms.title()
                : L.t("Yangi qo'shilgan mahsulotlar", "Новые товары", "Latest products");
        String emptyMsg = terms != null
                ? terms.empty()
                : L.t("Katalogga hali mahsulot qo'shilmagan.", "В каталог ещё не добавлены товары.",
                "No products added yet.");

        return new ProfileHighlightGroup(sectionTitle,
                recentCatalog.size() + " " + L.t("ta", "шт", "items"), "info", emptyMsg, recentCatalog);
    }

    // ================================================================ oxirgi harakatlar

    @Override
    public List<RecentActivityItem> loadRecentActivities(String company, List<AppModule> modules) {
        List<Activity> all = new ArrayList<>();
        String defaultUnit = resolveDefaultUnit(company);
        int order = 0;

        for (AppModule module : modules) {
            for (AppSection section : AppSection.of(module)) {
                if (section.isReport()) {
                    continue;
                }
                for (RecordStore.Rec r : rows(company, section)) {
                    String dateStr = extractDateString(r, section);
                    String subtitle = module.title() + " • " + section.title()
                            + (dateStr.isBlank() ? "" : " (" + dateStr + ")");
                    all.add(new Activity(parseRecordDate(r, section), order++, new RecentActivityItem(
                            extractPrimaryName(r, section),
                            subtitle,
                            extractBadgeOrAmount(r, section, defaultUnit),
                            dateStr,
                            route(module, section))));
                }
            }
        }

        // yangisi birinchi: sana bo'yicha (sanasizlari oxirida), teng bo'lsa keyin qo'shilgani oldin
        all.sort(Comparator
                .comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Activity::order, Comparator.reverseOrder()));

        List<RecentActivityItem> out = new ArrayList<>();
        for (int i = 0; i < all.size() && i < LIST_LIMIT; i++) {
            out.add(all.get(i).item());
        }
        return out;
    }

    // ================================================================ modul kartalari

    @Override
    public List<ModuleCardSummary> loadModuleSummaries(String company, List<AppModule> modules) {
        LocalDate today = LocalDate.now();
        String defaultUnit = resolveDefaultUnit(company);
        String cur = resolveCurrencyLabel();
        List<ModuleCardSummary> result = new ArrayList<>();

        for (AppModule module : modules) {
            String mKey = module.key().toLowerCase(Locale.ROOT);
            boolean inventoryModule = mKey.contains("ombor") || mKey.contains("inventory");

            int totalRecords = 0;
            double totalAmount = 0.0;
            String sumKey = null;

            int todayRecords = 0;
            double todaySum = 0.0;
            double todayIn = 0.0;
            double todayOut = 0.0;
            int[] dailyCounts = new int[7];

            for (AppSection section : AppSection.of(module)) {
                if (sumKey == null && section.sumKey() != null) {
                    sumKey = section.sumKey();
                }
                if (section.isReport()) {
                    continue;
                }

                String sProbe = (section.key() + " " + section.slug()).toLowerCase(Locale.ROOT);
                boolean isIn = sProbe.contains("kirim") || sProbe.contains("income") || sProbe.contains("receipt");
                boolean isOut = sProbe.contains("chiqim") || sProbe.contains("outgo") || sProbe.contains("issue")
                        || sProbe.contains("expense");

                List<RecordStore.Rec> rows = rows(company, section);
                totalRecords += rows.size();

                for (RecordStore.Rec r : rows) {
                    double amount = section.sumKey() != null ? FieldDef.parse(safeGet(r, section.sumKey())) : 0.0;
                    totalAmount += amount;

                    // sanasiz yozuvlar "bugun" ga qo'shilmaydi
                    LocalDate recDate = parseRecordDate(r, section);
                    if (recDate == null) {
                        continue;
                    }
                    long diff = ChronoUnit.DAYS.between(recDate, today);
                    if (diff >= 0 && diff < 7) {
                        dailyCounts[6 - (int) diff]++;
                    }
                    if (diff != 0) {
                        continue;
                    }

                    todayRecords++;
                    todaySum += amount;
                    // omborda miqdor, boshqa modullarda summa hisoblanadi
                    Double qty = extractQuantity(r, section);
                    double metric = inventoryModule ? (qty != null ? qty : 1.0) : amount;
                    if (isIn) {
                        todayIn += metric;
                    } else if (isOut) {
                        todayOut += metric;
                    }
                }
            }

            List<Integer> bars = new ArrayList<>(7);
            for (int c : dailyCounts) {
                bars.add(c);
            }

            String highlightLabel;
            String highlightValue;
            String secondaryLabel;
            String secondaryValue;

            if (mKey.contains("moliya") || mKey.contains("finance")) {
                highlightLabel = L.t("Bugungi tushum", "Сегодняшний приход", "Today's inflow");
                highlightValue = FieldDef.format(todayIn > 0 ? todayIn : todaySum) + " " + cur;
                secondaryLabel = L.t("Bugungi chiqim", "Сегодняшний расход", "Today's outflow");
                secondaryValue = FieldDef.format(todayOut) + " " + cur;
            } else if (mKey.contains("crm")) {
                highlightLabel = L.t("Bugungi bitimlar", "Сделок сегодня", "Deals today");
                highlightValue = todayRecords + " " + L.t("ta", "шт", "items");
                secondaryLabel = L.t("Bugungi summa", "Сумма сегодня", "Amount today");
                secondaryValue = FieldDef.format(todaySum) + " " + cur;
            } else if (inventoryModule) {
                highlightLabel = L.t("Bugungi kirim", "Приход сегодня", "Inbound today");
                highlightValue = FieldDef.format(todayIn) + " " + defaultUnit;
                secondaryLabel = L.t("Bugungi chiqim", "Расход сегодня", "Outbound today");
                secondaryValue = FieldDef.format(todayOut) + " " + defaultUnit;
            } else if (mKey.contains("kadr") || mKey.contains("hr") || mKey.contains("staff")) {
                highlightLabel = L.t("Jami xodimlar", "Всего записей", "Total records");
                highlightValue = totalRecords + " " + L.t("ta", "шт", "items");
                secondaryLabel = L.t("Hisob-kitob", "Начисления", "Payroll / total");
                secondaryValue = FieldDef.format(totalAmount) + " " + cur;
            } else {
                highlightLabel = sumKey != null ? L.t("Jami", "Итого", "Total")
                        : L.t("Jami yozuvlar", "Всего записей", "Total records");
                highlightValue = sumKey != null
                        ? FieldDef.format(totalAmount) + " " + cur
                        : totalRecords + " " + L.t("ta", "шт", "items");
                secondaryLabel = L.t("Kunlik aylanma", "Оборот за день", "Daily turnover");
                secondaryValue = FieldDef.format(todaySum) + " " + cur;
            }

            result.add(new ModuleCardSummary(module, totalRecords, highlightLabel, highlightValue,
                    todayRecords, secondaryLabel, secondaryValue, bars));
        }
        return result;
    }

    @Override
    public int countSectionRecords(String company, AppSection section) {
        return section.isReport() ? 0 : rows(company, section).size();
    }

    // ================================================================ birlik va valyuta

    private String resolveDefaultUnit(String company) {
        WarehouseProfile wp = resolveProfile(company);
        if (wp != null && wp.units() != null) {
            for (WarehouseProfile.Unit u : wp.units()) {
                String lbl = u.label(AppTexts.lang());
                if (isMeaningful(lbl)) {
                    return lbl;
                }
            }
        }
        return L.t("dona", "шт", "pcs");
    }

    /** Bazadagi birlik kaliti ("pcs", "kg") ni joriy tildagi nomga aylantiradi; tanilmasa o'zini qaytaradi. */
    private static String unitLabel(String raw, String fallback) {
        if (!isMeaningful(raw)) {
            return fallback;
        }
        String r = raw.trim();
        for (WarehouseProfile.Unit u : WarehouseProfile.Unit.values()) {
            if (r.equalsIgnoreCase(u.name()) || r.equalsIgnoreCase(u.label("en"))
                    || r.equalsIgnoreCase(u.label("uz")) || r.equalsIgnoreCase(u.label("ru"))) {
                return u.label(AppTexts.lang());
            }
        }
        return r;
    }

    private static String resolveCurrencyLabel() {
        String cur = AppTexts.t("rec.cur");
        return isMeaningful(cur) ? cur : "UZS";
    }

    // ================================================================ maydonlarni o'qish

    /** Faqat joriy qoldiq bo'limi (kirim, chiqim, inventarizatsiya emas). */
    private static boolean isStockSection(AppSection section) {
        String probe = (section.key() + " " + section.slug()).toLowerCase(Locale.ROOT);
        if (probe.contains("take") || probe.contains("count") || probe.contains("inventariz")) {
            return false;
        }
        return probe.contains("stock") || probe.contains("qoldiq");
    }

    private static String resolveItemIconKey(String name, String extra, AppSection section) {
        String secKey = section != null ? section.key() : "";
        String probe = (name + " " + extra + " " + secKey).toLowerCase(Locale.ROOT);
        if (probe.contains("ko'ylak") || probe.contains("kiyim") || probe.contains("cloth")
                || probe.contains("shirt") || probe.contains("men") || probe.contains("women")) {
            return "shirt";
        }
        return "box";
    }

    private static Double extractQuantity(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                String k = fd.key().toLowerCase(Locale.ROOT);
                if (k.contains("qty") || k.contains("quantity") || k.contains("qoldiq")
                        || k.contains("stock") || k.contains("count") || k.contains("soni")
                        || k.contains("miqdor")) {
                    String raw = safeGet(rec, fd.key());
                    if (isMeaningful(raw)) {
                        return FieldDef.parse(raw);
                    }
                }
            }
        }
        for (String fallbackKey : List.of("quantity", "qty", "qoldiq", "stock", "count")) {
            String raw = safeGet(rec, fallbackKey);
            if (isMeaningful(raw)) {
                return FieldDef.parse(raw);
            }
        }
        return null;
    }

    private static Double extractThreshold(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                String k = fd.key().toLowerCase(Locale.ROOT);
                if (k.startsWith("min") || k.contains("_min") || k.contains("limit")
                        || k.contains("threshold") || k.contains("kam_")) {
                    String raw = safeGet(rec, fd.key());
                    if (isMeaningful(raw)) {
                        return FieldDef.parse(raw);
                    }
                }
            }
        }
        return null;
    }

    /** Yozuv yoki mahsulot kartasidagi xom birlik (kalit yoki nom). */
    private static String extractUnitRaw(RecordStore.Rec rec, AppSection section, String productUnit) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                String k = fd.key().toLowerCase(Locale.ROOT);
                if (k.contains("unit") || k.contains("birlik") || k.contains("olchov")) {
                    String raw = safeGet(rec, fd.key());
                    if (isMeaningful(raw)) {
                        return raw;
                    }
                }
            }
        }
        String rawUnit = safeGet(rec, "unit");
        return isMeaningful(rawUnit) ? rawUnit : productUnit;
    }

    private static String extractImageUrl(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                String k = fd.key().toLowerCase(Locale.ROOT);
                if (k.contains("img") || k.contains("image") || k.contains("photo")
                        || k.contains("rasm") || k.contains("pic")) {
                    String raw = safeGet(rec, fd.key());
                    if (isMeaningful(raw)) {
                        return raw;
                    }
                }
            }
        }
        return safeGet(rec, "image");
    }

    private static String extractPrimaryName(RecordStore.Rec rec, AppSection section) {
        String nameVal = safeGet(rec, "name");
        if (isMeaningful(nameVal)) {
            return nameVal;
        }
        String productVal = safeGet(rec, "product");
        if (isMeaningful(productVal)) {
            return productVal;
        }
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                if (fd.type() != FieldDef.Type.DATE && fd.type() != FieldDef.Type.LONG && !fd.numeric()) {
                    String shown = fd.display(safeGet(rec, fd.key()));
                    if (isMeaningful(shown)) {
                        return shown;
                    }
                }
            }
        }
        String secTitle = section != null ? section.title() : L.t("Yozuv", "Запись", "Record");
        return secTitle + " #" + rec.id();
    }

    private static String extractSecondaryInfo(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                String k = fd.key().toLowerCase(Locale.ROOT);
                if (k.contains("warehouse") || k.contains("ombor") || k.contains("sku")
                        || k.contains("code") || k.contains("category")) {
                    String raw = safeGet(rec, fd.key());
                    String shown = k.contains("category") ? CatalogTerms.categoryLabel(raw) : fd.display(raw);
                    if (isMeaningful(shown)) {
                        return shown;
                    }
                }
            }
        }
        return "";
    }

    private static String extractDateString(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                if (fd.type() == FieldDef.Type.DATE) {
                    String shown = fd.display(safeGet(rec, fd.key()));
                    if (isMeaningful(shown)) {
                        return shown;
                    }
                }
            }
        }
        return safeGet(rec, "date");
    }

    private static LocalDate parseRecordDate(RecordStore.Rec rec, AppSection section) {
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                if (fd.type() == FieldDef.Type.DATE) {
                    LocalDate d = parseDate(safeGet(rec, fd.key()));
                    if (d != null) {
                        return d;
                    }
                }
            }
        }
        return parseDate(safeGet(rec, "date"));
    }

    private static LocalDate parseDate(String raw) {
        if (!isMeaningful(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extractBadgeOrAmount(RecordStore.Rec rec, AppSection section, String defaultUnit) {
        if (section != null && section.sumKey() != null) {
            double val = FieldDef.parse(safeGet(rec, section.sumKey()));
            if (val > 0) {
                return FieldDef.format(val) + " " + resolveCurrencyLabel();
            }
        }
        Double qty = extractQuantity(rec, section);
        if (qty != null && qty > 0) {
            return FieldDef.format(qty) + " " + unitLabel(extractUnitRaw(rec, section, null), defaultUnit);
        }
        if (section != null) {
            for (FieldDef fd : section.fields()) {
                if (fd.type() == FieldDef.Type.SELECT) {
                    String shown = fd.display(safeGet(rec, fd.key()));
                    if (isMeaningful(shown)) {
                        return shown;
                    }
                }
            }
        }
        return "";
    }
}