package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Ombor qoldig'i bilan ishlash: kirim, chiqim va inventarizatsiya shu orqali qoldiqni o'zgartiradi.
 * Qoldiq mahsulot nomi bo'yicha saqlanadi (ProductsView va POS bilan bir xil).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/StockLedger.java
 */
@Service
public class StockLedger {

    static final String STOCK_KEY = "inventory/stock";

    private final RecordStore store;

    public StockLedger(RecordStore store) {
        this.store = store;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private RecordStore.Rec find(String company, String product) {
        for (RecordStore.Rec r : store.list(company, STOCK_KEY)) {
            if (norm(r.get("product")).equals(norm(product))) {
                return r;
            }
        }
        return null;
    }

    /** Joriy qoldiq (yozuv bo'lmasa 0). */
    public synchronized BigDecimal quantity(String company, String product) {
        RecordStore.Rec r = find(company, product);
        BigDecimal q = r == null ? null : Dk.num(r.get("quantity"));
        return q == null ? BigDecimal.ZERO : q;
    }

    /**
     * Qoldiqni delta ga o'zgartiradi. allowNegative=false bo'lsa va natija manfiy bo'lsa,
     * hech narsa o'zgarmaydi va IllegalStateException tashlanadi.
     */
    public synchronized BigDecimal apply(String company, String product, BigDecimal delta, boolean allowNegative) {
        RecordStore.Rec r = find(company, product);
        BigDecimal current = BigDecimal.ZERO;
        if (r != null) {
            BigDecimal q = Dk.num(r.get("quantity"));
            current = q == null ? BigDecimal.ZERO : q;
        }
        BigDecimal next = current.add(delta);
        if (!allowNegative && next.signum() < 0) {
            throw new IllegalStateException("Insufficient stock");
        }
        Map<String, String> d = r == null ? new LinkedHashMap<>() : new LinkedHashMap<>(r.data());
        d.put("product", r == null ? product.trim() : r.get("product"));
        d.putIfAbsent("warehouse", "");
        d.putIfAbsent("min", "");
        d.put("quantity", Dk.plain(next));
        store.save(company, STOCK_KEY, r == null ? null : r.id(), d);
        return next;
    }
}