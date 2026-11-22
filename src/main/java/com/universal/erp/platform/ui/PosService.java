package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Kassa xizmati: mahsulot qidirish, savdoni saqlash, ombor qoldig'ini yuritish.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/PosService.java
 */
@Service
public class PosService {

    private static final Logger log = LoggerFactory.getLogger(PosService.class);

    public record Item(String id, String code, String name, BigDecimal price, String unit, String image) {
    }

    public record SaleLine(String name, BigDecimal qty) {
    }

    private final RecordStore store;

    public PosService(RecordStore store) {
        this.store = store;
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s\\p{Cntrl}]+", "");
    }

    // ------------------------------------------------------------------ qidirish

    public Optional<Item> find(String company, String rawCode) {
        String code = normalize(rawCode);
        if (code.isEmpty() || company == null || company.isBlank()) {
            return Optional.empty();
        }
        AppSection section = AppSection.get(AppModule.INVENTORY, "products");
        List<RecordStore.Rec> all = store.list(company, section.key());
        for (RecordStore.Rec r : all) {
            if (normalize(r.get("barcode")).equalsIgnoreCase(code)) {
                return Optional.of(toItem(section, r, code));
            }
        }
        for (RecordStore.Rec r : all) {
            if (normalize(r.get("sku")).equalsIgnoreCase(code)) {
                return Optional.of(toItem(section, r, code));
            }
        }
        return Optional.empty();
    }

    /** Nomi, shtrix-kodi yoki ichki kodi bo'yicha qidirish (kamida 2 belgi). */
    public List<Item> search(String company, String query) {
        List<Item> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2 || company == null || company.isBlank()) {
            return out;
        }
        AppSection section = AppSection.get(AppModule.INVENTORY, "products");
        for (RecordStore.Rec r : store.list(company, section.key())) {
            if (r.get("name").toLowerCase(Locale.ROOT).contains(q)
                    || r.get("barcode").toLowerCase(Locale.ROOT).contains(q)
                    || r.get("sku").toLowerCase(Locale.ROOT).contains(q)) {
                out.add(toItem(section, r, r.get("barcode")));
                if (out.size() >= 8) {
                    break;
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ savdo

    public String nextNumber(String company) {
        String companyKey = CurrentCompany.key(company);

        AppSection section = AppSection.get(AppModule.POS, "sales");

        LocalDate today = LocalDate.now();
        String date = today.toString();

        long number = store.list(companyKey, section.key())
                .stream()
                .filter(record -> date.equals(record.get("date")))
                .count() + 1;

        return "S-" + today.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", number);
    }

    /** Savdoni saqlaydi, ombor qoldig'ini kamaytiradi, chiqim yozadi. */
    public synchronized String saveSale(String company, String method, BigDecimal total, BigDecimal received,
                                        BigDecimal change, String items, String cashier, List<SaleLine> lines) {
        // POS alohida modul: savdolar "pos/sales" bo'limiga yoziladi
        AppSection s = AppSection.get(AppModule.POS, "sales");
        String no = nextNumber(company);

        Map<String, String> d = new LinkedHashMap<>();
        d.put("number", no);
        d.put("date", LocalDate.now().toString());
        d.put("time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        d.put("method", method);
        d.put("amount", total.stripTrailingZeros().toPlainString());
        if (received != null) {
            d.put("received", received.stripTrailingZeros().toPlainString());
        }
        if (change != null) {
            d.put("change", change.stripTrailingZeros().toPlainString());
        }
        if (cashier != null && !cashier.isBlank()) {
            d.put("cashier", cashier);
        }
        d.put("items", items);
        store.save(company, s.key(), null, d);

        for (SaleLine l : lines) {
            try {
                deductStock(company, l);
                writeIssue(company, l);
            } catch (RuntimeException ex) {
                // savdo allaqachon saqlangan; ombor yozuvi xatosi savdoni to'xtatmasin, lekin izi qolsin
                log.warn("Savdo {} uchun ombor yozuvi bajarilmadi (mahsulot={}, miqdor={})",
                        no, l.name(), l.qty(), ex);
            }
        }
        return no;
    }

    // ------------------------------------------------------------------ qoldiq

    public synchronized void setStock(String company, String product, BigDecimal qty) {
        AppSection st = AppSection.get(AppModule.INVENTORY, "stock");
        for (RecordStore.Rec r : store.list(company, st.key())) {
            if (r.get("product").trim().equalsIgnoreCase(product.trim())) {
                Map<String, String> data = new LinkedHashMap<>(r.data());
                data.put("quantity", qty.stripTrailingZeros().toPlainString());
                store.save(company, st.key(), r.id(), data);
                return;
            }
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("product", product.trim());
        data.put("quantity", qty.stripTrailingZeros().toPlainString());
        store.save(company, st.key(), null, data);
    }

    public synchronized void renameStock(String company, String from, String to) {
        AppSection st = AppSection.get(AppModule.INVENTORY, "stock");
        for (RecordStore.Rec r : store.list(company, st.key())) {
            if (r.get("product").trim().equalsIgnoreCase(from.trim())) {
                Map<String, String> data = new LinkedHashMap<>(r.data());
                data.put("product", to.trim());
                store.save(company, st.key(), r.id(), data);
            }
        }
    }

    public synchronized void deleteStock(String company, String product) {
        AppSection st = AppSection.get(AppModule.INVENTORY, "stock");
        for (RecordStore.Rec r : store.list(company, st.key())) {
            if (r.get("product").trim().equalsIgnoreCase(product.trim())) {
                store.delete(company, st.key(), r.id());
            }
        }
    }

    private void deductStock(String company, SaleLine l) {
        AppSection st = AppSection.get(AppModule.INVENTORY, "stock");
        for (RecordStore.Rec r : store.list(company, st.key())) {
            if (r.get("product").trim().equalsIgnoreCase(l.name().trim())) {
                Map<String, String> data = new LinkedHashMap<>(r.data());
                BigDecimal now = BigDecimal.valueOf(FieldDef.parse(r.get("quantity"))).subtract(l.qty());
                data.put("quantity", now.stripTrailingZeros().toPlainString());
                store.save(company, st.key(), r.id(), data);
                return;
            }
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("product", l.name());
        data.put("quantity", l.qty().negate().stripTrailingZeros().toPlainString());
        store.save(company, st.key(), null, data);
    }

    private void writeIssue(String company, SaleLine l) {
        AppSection is = AppSection.get(AppModule.INVENTORY, "issues");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("date", LocalDate.now().toString());
        data.put("customer", "POS");
        data.put("product", l.name());
        data.put("quantity", l.qty().stripTrailingZeros().toPlainString());
        data.put("reason", "sale");
        store.save(company, is.key(), null, data);
    }

    private static Item toItem(AppSection section, RecordStore.Rec r, String code) {
        FieldDef unitField = section.field("unit");
        String unitRaw = r.get("unit");
        String unit = unitField == null || unitRaw.isBlank() ? "" : unitField.display(unitRaw);
        String shownCode = r.get("barcode").isBlank() ? (r.get("sku").isBlank() ? code : r.get("sku")) : r.get("barcode");
        return new Item(r.id(), shownCode, r.get("name"), BigDecimal.valueOf(FieldDef.parse(r.get("price"))),
                unit, r.get("image"));
    }
}