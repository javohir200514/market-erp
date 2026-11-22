package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Yozuvlarni saqlashda bo'limlar o'rtasidagi bog'lanishlarni yuritadi.
 * View RecordStore.save/delete o'rniga SHU klassning save/delete metodlarini chaqirishi kerak.
 *
 * Avtomatik harakatlar:
 *  - REF maydonlari: ko'rsatilgan yozuv mavjudligi tekshiriladi.
 *  - To'lov (payments): hisob-faktura holati (paid) va kassa/bank qoldig'i (+summa) yangilanadi.
 *  - Xarajat (expenses): kassa/bank qoldig'i (-summa) yangilanadi.
 *  Tahrirlash va o'chirishda eski ta'sir qaytarilib, yangisi qo'llanadi.
 *
 * Eslatma: payments hozircha faqat KIRIM deb hisoblanadi (maydonda yo'nalish yo'q).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RecordService.java
 */
@Service
public class RecordService {

    private static final String INVOICES = "finance/invoices";
    private static final String PAYMENTS = "finance/payments";
    private static final String EXPENSES = "finance/expenses";
    private static final String ACCOUNTS = "finance/accounts";

    private record Effect(String accountId, BigDecimal amount) { }

    private final RecordStore store;

    public RecordService(RecordStore store) {
        this.store = store;
    }

    /** @throws IllegalArgumentException REF ko'rsatgan yozuv topilmasa */
    public synchronized RecordStore.Rec save(String company, String section, String id, Map<String, String> data) {
        checkRefs(company, section, data);
        RecordStore.Rec before = id == null ? null : store.find(company, section, id).orElse(null);
        RecordStore.Rec saved = store.save(company, section, id, data);
        afterChange(company, section, before, saved);
        return saved;
    }

    public synchronized void delete(String company, String section, String id) {
        RecordStore.Rec before = store.find(company, section, id).orElse(null);
        store.delete(company, section, id);
        afterChange(company, section, before, null);
    }

    // ------------------------------------------------------------------ tekshiruv

    private void checkRefs(String company, String section, Map<String, String> data) {
        Optional<AppSection> def = AppSection.findByKey(section);
        if (def.isEmpty()) {
            return;
        }
        for (FieldDef f : def.get().refFields()) {
            String v = data.get(f.key());
            if (v != null && !v.isBlank() && store.find(company, f.target(), v.trim()).isEmpty()) {
                throw new IllegalArgumentException(
                        "Bog'langan yozuv topilmadi: " + section + "." + f.key() + " -> " + f.target());
            }
        }
    }

    // ------------------------------------------------------------------ avtomatik harakatlar

    private void afterChange(String company, String section, RecordStore.Rec before, RecordStore.Rec after) {
        Effect old = effect(section, before);
        Effect now = effect(section, after);
        if (old != null) {
            adjustBalance(company, old.accountId(), old.amount().negate());
        }
        if (now != null) {
            adjustBalance(company, now.accountId(), now.amount());
        }

        if (PAYMENTS.equals(section)) {
            Set<String> invoiceIds = new LinkedHashSet<>();
            if (before != null) {
                invoiceIds.add(before.get("invoice"));
            }
            if (after != null) {
                invoiceIds.add(after.get("invoice"));
            }
            invoiceIds.stream()
                    .filter(i -> !i.isBlank())
                    .forEach(i -> refreshInvoice(company, i));
        }
    }

    /** Yozuvning kassa/bank hisobiga ta'siri (kirim +, chiqim -). */
    private static Effect effect(String section, RecordStore.Rec rec) {
        if (rec == null) {
            return null;
        }
        String account = rec.get("account");
        if (account.isBlank()) {
            return null;
        }
        BigDecimal amount = dec(rec.get("amount"));
        return switch (section) {
            case PAYMENTS -> new Effect(account, amount);
            case EXPENSES -> new Effect(account, amount.negate());
            default -> null;
        };
    }

    private void adjustBalance(String company, String accountId, BigDecimal delta) {
        Optional<RecordStore.Rec> acc = store.find(company, ACCOUNTS, accountId);
        if (acc.isEmpty() || delta.signum() == 0) {
            return;
        }
        Map<String, String> data = new LinkedHashMap<>(acc.get().data());
        BigDecimal balance = dec(acc.get().get("balance")).add(delta);
        data.put("balance", balance.stripTrailingZeros().toPlainString());
        store.save(company, ACCOUNTS, accountId, data);
    }

    /**
     * Hisob-faktura holatini to'lovlar yig'indisiga qarab qayta hisoblaydi:
     * to'lovlar summasi >= faktura summasi bo'lsa — paid;
     * to'lov o'chirilib/kamaytirilib paid holati buzilsa — overdue (muddat o'tgan bo'lsa) yoki sent.
     * Boshqa holatlarga tegilmaydi.
     */
    private void refreshInvoice(String company, String invoiceId) {
        Optional<RecordStore.Rec> found = store.find(company, INVOICES, invoiceId);
        if (found.isEmpty()) {
            return;
        }
        RecordStore.Rec invoice = found.get();

        BigDecimal paid = BigDecimal.ZERO;
        for (RecordStore.Rec p : store.list(company, PAYMENTS)) {
            if (invoiceId.equals(p.get("invoice"))) {
                paid = paid.add(dec(p.get("amount")));
            }
        }

        BigDecimal total = dec(invoice.get("amount"));
        String status = invoice.get("status");
        String next = status;
        if (total.signum() > 0 && paid.compareTo(total) >= 0) {
            next = "paid";
        } else if ("paid".equals(status)) {
            next = isOverdue(invoice) ? "overdue" : "sent";
        }

        if (!next.equals(status)) {
            Map<String, String> data = new LinkedHashMap<>(invoice.data());
            data.put("status", next);
            store.save(company, INVOICES, invoiceId, data);
        }
    }

    private static boolean isOverdue(RecordStore.Rec invoice) {
        try {
            String due = invoice.get("due");
            return !due.isBlank() && LocalDate.parse(due).isBefore(LocalDate.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static BigDecimal dec(String s) {
        try {
            return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim().replace(" ", ""));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}