package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;


/** Telefondan TCP orqali kelgan mahsulot so'rovlarini bajaradi. */
@Component
public class PhoneProductService {

    private final RecordStore store;
    private final Object lock = new Object();

    private final GlobalProductService globals;
    private final PosSettings settings;

    public PhoneProductService(RecordStore store, GlobalProductService globals, PosSettings settings) {
        this.store = store;
        this.globals = globals;
        this.settings = settings;
    }

    public Map<String, Object> handle(String company, JsonNode req) {
        String op = req.path("op").asText("");
        switch (op) {
            case "product":
                return product(company, req.path("code").asText(""));
            case "meta":
                return meta();
            case "price":
                return updatePrice(company, req);
            case "create":
                return create(company, req);
            default:
                throw new IllegalArgumentException("Noma'lum amal: " + op);
        }
    }

    // ------------------------------------------------------------ amallar

    private Map<String, Object> meta() {
        AppSection ps = products();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("categories", options(ps.field("category")));
        r.put("units", options(ps.field("unit")));
        return r;
    }

    private Map<String, Object> product(String company, String rawCode) {
        RecordStore.Rec rec = findByCode(company, rawCode);
        if (rec == null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("found", false);
            String code = PosService.normalize(rawCode);
            if (settings.get(company).global() && code.matches("\\d{6,14}")) {
                globals.lookup(code).ifPresent(g -> r.put("global", g.toMap()));
            }
            return r;
        }
        return productJson(company, rec);
    }

    private Map<String, Object> updatePrice(String company, JsonNode req) {
        String id = req.path("id").asText("");
        BigDecimal price = dec(req.get("price"));
        BigDecimal cost = dec(req.get("cost"));

        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Sotish narxi 0 dan katta bo'lishi kerak");
        }
        if (cost != null && cost.signum() < 0) {
            throw new IllegalArgumentException("Tannarx manfiy bo'lmasligi kerak");
        }

        synchronized (lock) {
            AppSection ps = products();
            RecordStore.Rec rec = null;
            for (RecordStore.Rec it : store.list(company, ps.key())) {
                if (it.id().equals(id)) {
                    rec = it;
                    break;
                }
            }
            if (rec == null) {
                throw new IllegalArgumentException("Mahsulot topilmadi");
            }
            Map<String, String> data = new LinkedHashMap<>(rec.data());
            data.put("price", plain(price));
            if (cost != null) {
                data.put("cost", plain(cost));
            }
            RecordStore.Rec saved = store.save(company, ps.key(), id, data);
            return productJson(company, saved);
        }
    }

    private Map<String, Object> create(String company, JsonNode req) {
        String barcode = PosService.normalize(req.path("barcode").asText(""));
        String name = req.path("name").asText("").trim();
        BigDecimal price = dec(req.get("price"));
        BigDecimal cost = dec(req.get("cost"));
        BigDecimal qty = dec(req.get("quantity"));

        if (barcode.isEmpty()) {
            throw new IllegalArgumentException("Shtrix-kod kerak");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Mahsulot nomini kiriting");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Sotish narxi 0 dan katta bo'lishi kerak");
        }
        if (cost != null && cost.signum() < 0) {
            throw new IllegalArgumentException("Tannarx manfiy bo'lmasligi kerak");
        }
        if (qty != null && qty.signum() < 0) {
            throw new IllegalArgumentException("Qoldiq manfiy bo'lmasligi kerak");
        }

        AppSection ps = products();
        String category = req.path("category").asText("");
        String unit = req.path("unit").asText("");
        if (category.isBlank()) {
            category = "other";
        }
        if (unit.isBlank()) {
            unit = "pcs";
        }
        if (ps.field("category").option(category) == null) {
            throw new IllegalArgumentException("Noto'g'ri toifa");
        }
        if (ps.field("unit").option(unit) == null) {
            throw new IllegalArgumentException("Noto'g'ri o'lchov birligi");
        }

        synchronized (lock) {
            for (RecordStore.Rec it : store.list(company, ps.key())) {
                if (PosService.normalize(it.get("barcode")).equalsIgnoreCase(barcode)
                        || PosService.normalize(it.get("sku")).equalsIgnoreCase(barcode)) {
                    throw new IllegalArgumentException("Bu shtrix-kod allaqachon ro'yxatda bor");
                }
                if (it.get("name").trim().equalsIgnoreCase(name)) {
                    throw new IllegalArgumentException("Bunday nomli mahsulot allaqachon bor");
                }
            }

            Map<String, String> data = new LinkedHashMap<>();
            data.put("barcode", barcode);
            data.put("name", name);
            data.put("category", category);
            data.put("unit", unit);
            if (cost != null) {
                data.put("cost", plain(cost));
            }
            data.put("price", plain(price));

            RecordStore.Rec saved = store.save(company, ps.key(), null, data);

            if (qty != null && qty.signum() > 0) {
                AppSection stockSection = AppSection.get(AppModule.INVENTORY, "stock");
                Map<String, String> stock = new LinkedHashMap<>();
                stock.put("product", name);
                stock.put("quantity", plain(qty));
                store.save(company, stockSection.key(), null, stock);

                AppSection receiptSection = AppSection.get(AppModule.INVENTORY, "receipts");
                Map<String, String> receipt = new LinkedHashMap<>();
                receipt.put("date", LocalDate.now().toString());
                receipt.put("supplier", "Mobil ilova");
                receipt.put("product", name);
                receipt.put("quantity", plain(qty));
                if (cost != null) {
                    receipt.put("price", plain(cost));
                }
                store.save(company, receiptSection.key(), null, receipt);
            }
            return productJson(company, saved);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private static AppSection products() {
        return AppSection.get(AppModule.INVENTORY, "products");
    }

    private RecordStore.Rec findByCode(String company, String rawCode) {
        String code = PosService.normalize(rawCode);
        if (code.isEmpty()) {
            return null;
        }
        List<RecordStore.Rec> all = store.list(company, products().key());
        for (RecordStore.Rec it : all) {
            if (PosService.normalize(it.get("barcode")).equalsIgnoreCase(code)) {
                return it;
            }
        }
        for (RecordStore.Rec it : all) {
            if (PosService.normalize(it.get("sku")).equalsIgnoreCase(code)) {
                return it;
            }
        }
        return null;
    }

    private Map<String, Object> productJson(String company, RecordStore.Rec rec) {
        AppSection ps = products();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("found", true);
        r.put("id", rec.id());
        r.put("name", rec.get("name"));
        r.put("barcode", rec.get("barcode"));
        r.put("sku", rec.get("sku"));
        r.put("category", rec.get("category"));
        r.put("categoryLabel", label(ps.field("category"), rec.get("category")));
        r.put("unit", rec.get("unit"));
        r.put("unitLabel", label(ps.field("unit"), rec.get("unit")));
        r.put("cost", FieldDef.parse(rec.get("cost")));
        r.put("price", FieldDef.parse(rec.get("price")));
        r.put("stock", stockOf(company, rec.get("name")));
        return r;
    }

    private Double stockOf(String company, String name) {
        AppSection stockSection = AppSection.get(AppModule.INVENTORY, "stock");
        for (RecordStore.Rec it : store.list(company, stockSection.key())) {
            if (it.get("product").trim().equalsIgnoreCase(name.trim())) {
                return FieldDef.parse(it.get("quantity"));
            }
        }
        return null;
    }

    private static String label(FieldDef field, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        FieldDef.Opt o = field.option(key);
        return o == null ? key : o.names()[0];
    }

    private static List<Map<String, String>> options(FieldDef field) {
        List<Map<String, String>> res = new ArrayList<>();
        for (FieldDef.Opt o : field.options()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", o.key());
            m.put("label", o.names()[0]);
            res.add(m);
        }
        return res;
    }

    private static BigDecimal dec(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        try {
            return new BigDecimal(n.asText().trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
