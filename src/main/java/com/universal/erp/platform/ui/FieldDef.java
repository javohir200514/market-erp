package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bo'lim yozuvidagi bitta maydon. Ta'rif matni:
 * <pre>
 *   [*]kalit|tur|Uz nomi|Ru nomi|En nomi[|qo'shimcha]
 * </pre>
 * Boshidagi * — majburiy maydon.
 * Turlar: text, long, number, money, date, select, ref.
 *
 * select uchun qo'shimcha:  tanlov1:uz:ru:en,tanlov2:uz:ru:en
 * ref uchun qo'shimcha:     maqsad bo'lim kaliti, masalan crm/customers
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/FieldDef.java
 */
public final class FieldDef {

    public enum Type { TEXT, LONG, NUMBER, MONEY, DATE, SELECT, REF }

    /** Tanlov (select) varianti. */
    public record Opt(String key, String[] names) {
        public String label() {
            return names[AppTexts.index()];
        }
    }

    private static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final String key;
    private final boolean required;
    private final Type type;
    private final String[] labels;
    private final List<Opt> options = new ArrayList<>();

    /** REF turi uchun: bog'langan bo'lim kaliti (masalan "crm/customers"). */
    private String target;

    private FieldDef(String key, boolean required, Type type, String[] labels) {
        this.key = key;
        this.required = required;
        this.type = type;
        this.labels = labels;
    }

    public static FieldDef of(String spec) {
        String[] p = spec.split("\\|", -1);
        boolean req = p[0].startsWith("*");
        String key = req ? p[0].substring(1) : p[0];
        Type type = Type.valueOf(p[1].toUpperCase(Locale.ROOT));
        FieldDef f = new FieldDef(key, req, type, new String[] {p[2], p[3], p[4]});

        if (type == Type.REF) {
            if (p.length <= 5 || p[5].isBlank()) {
                throw new IllegalArgumentException(
                        "REF maydonida maqsad bo'lim ko'rsatilmagan: " + spec);
            }
            f.target = p[5].trim();
        } else if (p.length > 5 && !p[5].isBlank()) {
            for (String o : p[5].split(",")) {
                String[] q = o.split(":");
                f.options.add(new Opt(q[0], new String[] {q[1], q[2], q[3]}));
            }
        }
        return f;
    }

    public String key() {
        return key;
    }

    public boolean required() {
        return required;
    }

    public Type type() {
        return type;
    }

    public List<Opt> options() {
        return options;
    }

    public String label() {
        return labels[AppTexts.index()];
    }

    public boolean numeric() {
        return type == Type.NUMBER || type == Type.MONEY;
    }

    /** REF maydoni bog'langan bo'lim kaliti, masalan "crm/customers". REF bo'lmasa null. */
    public String target() {
        return target;
    }

    public boolean isRef() {
        return type == Type.REF;
    }

    /**
     * Ro'yxatda ko'rsatiladigan matn
     * (select — tarjimasi, sana — dd.MM.yyyy, son — probel bilan, ref — bog'langan yozuv nomi).
     */
    public String display(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return switch (type) {
            case SELECT -> options.stream().filter(o -> o.key().equals(raw)).findFirst().map(Opt::label).orElse(raw);
            case DATE -> {
                try {
                    yield LocalDate.parse(raw).format(SHOW);
                } catch (RuntimeException ex) {
                    yield raw;
                }
            }
            case NUMBER, MONEY -> format(parse(raw));
            case REF -> RefResolver.label(target, raw);
            default -> raw;
        };
    }

    public Opt option(String k) {
        return options.stream().filter(o -> o.key().equals(k)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ yordamchilar

    public static double parse(String s) {
        try {
            return s == null || s.isBlank() ? 0 : new BigDecimal(s.trim()).doubleValue();
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** 1250000 -> "1 250 000"; kasr bo'lsa 2 xonagacha. */
    public static String format(double v) {
        boolean whole = Math.abs(v - Math.rint(v)) < 0.005;
        String s = String.format(Locale.ROOT, whole ? "%,.0f" : "%,.2f", v);
        return s.replace(',', ' ');
    }
}