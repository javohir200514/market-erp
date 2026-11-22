package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Hisobot / tahlil bo'limlari: boshqa bo'limlardagi yozuvlardan avtomatik yig'iladi
 * (Moliya → Hisobotlar, BI → Boshqaruv paneli, Savdo tahlili, Moliyaviy tahlil).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/ReportPanel.java
 */
final class ReportPanel {

    private final RecordStore store;
    private final String company;

    ReportPanel(RecordStore store, String company) {
        this.store = store;
        this.company = company;
    }

    Component build(AppSection section) {
        Div root = new Div();
        root.addClassName("sh-report");

        Paragraph hint = new Paragraph(t("rep.hint"));
        hint.addClassName("sh-report-hint");
        root.add(hint);

        switch (section.key()) {
            case "finance/reports" -> financeReport(root);
            case "bi/dashboard" -> dashboard(root);
            case "bi/sales" -> sales(root);
            case "bi/finance" -> financeAnalytics(root);
            default -> { }
        }
        return root;
    }

    // ------------------------------------------------------------------ hisobotlar

    private void financeReport(Div root) {
        double billed = sum("finance/invoices", "amount", r -> !"draft".equals(r.get("status")));
        double paidInv = sum("finance/invoices", "amount", r -> "paid".equals(r.get("status")));
        double income = sum("finance/payments", "amount", r -> true);
        double expenses = sum("finance/expenses", "amount", r -> true);

        root.add(kpis(
                kpi(t("rep.billed"), money(billed), null),
                kpi(t("rep.income"), money(income), "good"),
                kpi(t("rep.expenses"), money(expenses), "bad"),
                kpi(t("rep.profit"), money(income - expenses), income - expenses >= 0 ? "good" : "bad"),
                kpi(t("rep.receivable"), money(billed - paidInv), billed - paidInv > 0 ? "warn" : null)));

        root.add(chart(t("rep.by.expense"), group("finance/expenses", "category", "amount")));
        root.add(chart(t("rep.by.invoice"), group("finance/invoices", "status", "amount")));
    }

    private void dashboard(Div root) {
        long customers = store.list(company, "crm/customers").size();
        long open = store.list(company, "crm/deals").stream()
                .filter(r -> !"won".equals(r.get("stage")) && !"lost".equals(r.get("stage"))).count();
        long products = store.list(company, "inventory/products").size();
        long employees = store.list(company, "hr/employees").stream()
                .filter(r -> !"fired".equals(r.get("status"))).count();
        double income = sum("finance/payments", "amount", r -> true);
        double expenses = sum("finance/expenses", "amount", r -> true);

        root.add(kpis(
                kpi(t("rep.customers"), String.valueOf(customers), null),
                kpi(t("rep.opendeals"), String.valueOf(open), null),
                kpi(t("rep.products"), String.valueOf(products), null),
                kpi(t("rep.employees"), String.valueOf(employees), null),
                kpi(t("rep.profit"), money(income - expenses), income - expenses >= 0 ? "good" : "bad")));

        root.add(chart(t("rep.by.stage"), count("crm/deals", "stage")));
    }

    private void sales(Div root) {
        double all = sum("crm/deals", "amount", r -> true);
        double won = sum("crm/deals", "amount", r -> "won".equals(r.get("stage")));
        long wonN = store.list(company, "crm/deals").stream().filter(r -> "won".equals(r.get("stage"))).count();
        long lostN = store.list(company, "crm/deals").stream().filter(r -> "lost".equals(r.get("stage"))).count();
        String rate = wonN + lostN == 0 ? "—" : Math.round(100.0 * wonN / (wonN + lostN)) + "%";

        root.add(kpis(
                kpi(t("rep.dealsum"), money(all), null),
                kpi(t("rep.won"), money(won), "good"),
                kpi(t("rep.winrate"), rate, null)));

        root.add(chart(t("rep.by.stage"), group("crm/deals", "stage", "amount")));
        root.add(chart(t("rep.by.customer"), top(group("crm/deals", "customer", "amount"), 6)));
    }

    private void financeAnalytics(Div root) {
        double income = sum("finance/payments", "amount", r -> true);
        double expenses = sum("finance/expenses", "amount", r -> true);
        double profit = income - expenses;
        String margin = income <= 0 ? "—" : Math.round(100.0 * profit / income) + "%";

        root.add(kpis(
                kpi(t("rep.income"), money(income), "good"),
                kpi(t("rep.expenses"), money(expenses), "bad"),
                kpi(t("rep.profit"), money(profit), profit >= 0 ? "good" : "bad"),
                kpi(t("rep.margin"), margin, null)));

        root.add(chart(t("rep.by.expense"), group("finance/expenses", "category", "amount")));
        root.add(chart(t("rep.by.method"), group("finance/payments", "method", "amount")));
    }

    // ------------------------------------------------------------------ hisoblash

    private double sum(String section, String field, Predicate<RecordStore.Rec> filter) {
        double s = 0;
        for (RecordStore.Rec r : store.list(company, section)) {
            if (filter.test(r)) {
                s += FieldDef.parse(r.get(field));
            }
        }
        return s;
    }

    /** kalit (tarjima qilingan) -> summa */
    private Map<String, Double> group(String section, String keyField, String sumField) {
        AppSection sec = byKey(section);
        FieldDef kf = sec.field(keyField);
        Map<String, Double> m = new LinkedHashMap<>();
        for (RecordStore.Rec r : store.list(company, section)) {
            String k = r.get(keyField);
            String label = k.isBlank() ? "—" : kf.display(k);
            m.merge(label, FieldDef.parse(r.get(sumField)), Double::sum);
        }
        return sorted(m);
    }

    private Map<String, Double> count(String section, String keyField) {
        AppSection sec = byKey(section);
        FieldDef kf = sec.field(keyField);
        Map<String, Double> m = new LinkedHashMap<>();
        for (RecordStore.Rec r : store.list(company, section)) {
            String k = r.get(keyField);
            m.merge(k.isBlank() ? "—" : kf.display(k), 1.0, Double::sum);
        }
        return sorted(m);
    }

    private static Map<String, Double> sorted(Map<String, Double> m) {
        Map<String, Double> out = new LinkedHashMap<>();
        m.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    private static Map<String, Double> top(Map<String, Double> m, int n) {
        Map<String, Double> out = new LinkedHashMap<>();
        m.entrySet().stream().limit(n).forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    private static AppSection byKey(String key) {
        String[] p = key.split("/");
        return AppSection.get(AppModule.byKey(p[0]).orElseThrow(), p[1]);
    }

    private static String money(double v) {
        return FieldDef.format(v) + " " + t("rec.cur");
    }

    // ------------------------------------------------------------------ ko'rinish

    private static Div kpis(Component... cards) {
        Div d = new Div(cards);
        d.addClassName("sh-kpis");
        return d;
    }

    private static Component kpi(String label, String value, String tone) {
        Span l = new Span(label);
        l.addClassName("sh-kpi-label");
        Span v = new Span(value);
        v.addClassName("sh-kpi-value");
        Div d = new Div(l, v);
        d.addClassName("sh-kpi");
        if (tone != null) {
            d.addClassName("is-" + tone);
        }
        return d;
    }

    private Component chart(String title, Map<String, Double> data) {
        H3 h = new H3(title);
        Div box = new Div(h);
        box.addClassName("sh-chart");

        if (data.isEmpty() || data.values().stream().allMatch(v -> v <= 0)) {
            Span none = new Span(t("rep.nodata"));
            none.addClassName("sh-chart-empty");
            box.add(none);
            return box;
        }
        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        for (Map.Entry<String, Double> e : data.entrySet()) {
            Span name = new Span(e.getKey());
            name.addClassName("sh-bar-name");
            Span val = new Span(FieldDef.format(e.getValue()));
            val.addClassName("sh-bar-val");
            Div fill = new Div();
            fill.addClassName("sh-bar-fill");
            fill.getStyle().set("width", Math.max(2, Math.round(100 * e.getValue() / max)) + "%");
            Div track = new Div(fill);
            track.addClassName("sh-bar-track");
            Div head = new Div(name, val);
            head.addClassName("sh-bar-head");
            Div row = new Div(head, track);
            row.addClassName("sh-bar");
            box.add(row);
        }
        return box;
    }

}