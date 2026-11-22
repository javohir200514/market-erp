package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiInsights.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BI dashboard: tahlil, taqqoslash, qaror. Hamma summalar so'mda.
 * Stil: frontend/styles/dashboard.css (dx-*) + frontend/styles/bi.css (bi-*).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiDashboardView.java
 */
@Route(value = "app/bi/dashboard", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/dashboard.css")
@CssImport("./styles/bi.css")
public class BiDashboardView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final Map<String, String> ICONS = Map.of(
            "trend", "M3 17l6-6 4 4 8-8|M15 7h6v6",
            "wallet", "M3 5a2 2 0 0 1 2-2h13v4|M3 5v14a2 2 0 0 0 2 2h16V7H5a2 2 0 0 1-2-2|M16 13.5h.01",
            "users", "M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2|M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8|M21 21v-2a4 4 0 0 0-3-3.9|M16 3.1a4 4 0 0 1 0 7.8",
            "briefcase", "M3 8h18v12H3z|M8 8V5a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v3|M3 13h18",
            "box", "M21 8l-9-5-9 5v8l9 5 9-5z|M3.3 7.5L12 12l8.7-4.5|M12 22V12",
            "chart", "M3 3v18h18|M7 15l4-4 3 3 5-6",
            "alert", "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z|M12 9v4|M12 17h.01",
            "activity", "M22 12h-4l-3 9L9 3l-3 9H2");

    private static final String FIX_SVG = "$0.querySelectorAll('svg').forEach(s=>{"
            + "s.setAttribute('viewBox', s.dataset.vb || '0 0 24 24');"
            + "if(s.dataset.par) s.setAttribute('preserveAspectRatio', s.dataset.par);});";

    private final BiInsights service;
    private final Div body = new Div();
    private String company = "";
    private String preset = "month";
    private LocalDate cFrom = LocalDate.now().withDayOfMonth(1);
    private LocalDate cTo = LocalDate.now();
    private final Div presetBar = new Div();
    private final Div customBox = new Div();

    public BiDashboardView(BiInsights service) {
        this.service = service;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        company = CurrentCompany.get();
        removeAll();
        addClassNames("dx", "bi");
        add(head(), body);
        refresh();
    }

    // ================================================================ sarlavha va filtr

    private Component head() {
        H1 title = new H1(L.t("BI dashboard", "BI дашборд", "BI dashboard"));
        title.addClassName("dx-title");
        Paragraph sub = new Paragraph(L.t("Biznesning o'sishi, foydasi va qarzlari bir joyda. Barcha summalar so'mda.",
                "Рост, прибыль и долги бизнеса в одном месте. Все суммы в сумах.",
                "Growth, profit and debts in one place. All amounts in UZS."));
        sub.addClassName("dx-sub");
        Div text = new Div(title, sub);

        String[][] presets = {
                {"month", L.t("Bu oy", "Этот месяц", "This month")},
                {"lastMonth", L.t("O'tgan oy", "Прошлый месяц", "Last month")},
                {"quarter", L.t("Chorak", "Квартал", "Quarter")},
                {"year", L.t("Yil", "Год", "Year")},
                {"custom", L.t("Sana oralig'i", "Период", "Custom")}};
        presetBar.addClassName("dx-seg");
        for (String[] p : presets) {
            NativeButton b = new NativeButton(p[1]);
            b.addClassName("dx-seg-btn");
            b.getElement().setAttribute("data-key", p[0]);
            b.addClickListener(e -> {
                preset = p[0];
                refresh();
            });
            presetBar.add(b);
        }

        DatePicker from = new DatePicker(L.t("Dan", "С", "From"));
        DatePicker to = new DatePicker(L.t("Gacha", "По", "To"));
        from.setValue(cFrom);
        to.setValue(cTo);
        from.addValueChangeListener(e -> { if (e.getValue() != null) { cFrom = e.getValue(); refresh(); } });
        to.addValueChangeListener(e -> { if (e.getValue() != null) { cTo = e.getValue(); refresh(); } });
        customBox.addClassName("bi-custom");
        customBox.add(from, to);

        Div controls = new Div(presetBar, customBox);
        controls.addClassName("bi-controls");
        Div head = new Div(text, controls);
        head.addClassName("dx-head");
        return head;
    }

    private Range range() {
        LocalDate t = LocalDate.now();
        switch (preset) {
            case "lastMonth": {
                LocalDate f = t.withDayOfMonth(1).minusMonths(1);
                return new Range(f, f.withDayOfMonth(f.lengthOfMonth()));
            }
            case "quarter":
                return new Range(LocalDate.of(t.getYear(), ((t.getMonthValue() - 1) / 3) * 3 + 1, 1), t);
            case "year":
                return new Range(LocalDate.of(t.getYear(), 1, 1), t);
            case "custom":
                if (!cTo.isBefore(cFrom)) {
                    return new Range(cFrom, cTo);
                }
                return new Range(t.withDayOfMonth(1), t);
            default:
                return new Range(t.withDayOfMonth(1), t);
        }
    }

    private void refresh() {
        presetBar.getChildren().forEach(c -> c.getElement().getClassList()
                .set("is-active", preset.equals(c.getElement().getAttribute("data-key"))));
        customBox.setVisible("custom".equals(preset));
        Range r = range();
        Report rep = service.load(company, r);
        body.removeAll();
        body.add(toolbar(rep), insights(rep), kpis(rep));

        Div fin = new Div(financeChart(rep), profitCard(rep));
        fin.addClassName("dx-main");
        Div row2 = new Div(expenseCard(rep), debtCard(rep), crmCard(rep));
        row2.addClassName("dx-grid3");
        Div row3 = new Div(stockCard(rep), abcCard(rep), hrCard(rep));
        row3.addClassName("dx-grid3");
        body.add(fin, row2, row3);
        getElement().executeJs(FIX_SVG, getElement());
    }

    // ================================================================ asboblar paneli

    private Component toolbar(Report rep) {
        Span period = new Span(rep.range().from() + " — " + rep.range().to());
        period.addClassName("dx-date");

        byte[] data = csv(rep).getBytes(StandardCharsets.UTF_8);
        StreamResource res = new StreamResource("bi-" + rep.range().from() + "_" + rep.range().to() + ".csv",
                () -> new ByteArrayInputStream(data));
        Anchor export = new Anchor(res, L.t("CSV yuklab olish", "Скачать CSV", "Download CSV"));
        export.getElement().setAttribute("download", true);
        export.addClassNames("dx-seg-btn", "bi-btn");

        NativeButton print = new NativeButton(L.t("Chop etish", "Печать", "Print"));
        print.addClassNames("dx-seg-btn", "bi-btn");
        print.addClickListener(e -> getElement().executeJs("window.print()"));

        Div tools = new Div(export, print);
        tools.addClassName("bi-tools");
        Div bar = new Div(period, tools);
        bar.addClassName("bi-toolbar");
        return bar;
    }

    private Component insights(Report rep) {
        Div box = new Div();
        box.addClassName("bi-ins");
        for (String s : rep.insights()) {
            Span chip = new Span(s);
            chip.addClassName("bi-ins-item");
            box.add(chip);
        }
        box.setVisible(!rep.insights().isEmpty());
        return box;
    }

    // ================================================================ KPI

    private Component kpis(Report r) {
        Totals c = r.cur();
        Div strip = new Div(
                kpi("trend", L.t("Daromad", "Выручка", "Revenue"), compact(c.revenue()),
                        delta(c.revenue(), r.prev().revenue(), r.year().revenue()), ""),
                kpi("wallet", L.t("Sof foyda", "Чистая прибыль", "Net profit"), compact(c.net()),
                        delta(c.net(), r.prev().net(), r.year().net()), c.net() < 0 ? "is-warn" : ""),
                kpi("chart", L.t("Foyda marjasi", "Маржа прибыли", "Profit margin"), pct1(c.margin()) + "%",
                        text(L.t("Sof foyda / daromad", "Чистая прибыль / выручка", "Net profit / revenue")), ""),
                kpi("users", L.t("Mijozlar", "Клиенты", "Customers"), String.valueOf(r.customersTotal()),
                        text("+" + r.customersNew() + " " + L.t("yangi", "новых", "new")), ""),
                kpi("briefcase", L.t("Ochiq bitimlar", "Открытые сделки", "Open deals"), String.valueOf(r.openDeals()),
                        text(compact(r.openDealsSum())), ""),
                kpi("box", L.t("Mahsulotlar", "Товары", "Products"), String.valueOf(r.stockCount()),
                        text(L.t("Ombor: ", "Склад: ", "Stock: ") + compact(r.stockValue())), ""),
                kpi("activity", L.t("Xodimlar", "Сотрудники", "Employees"), String.valueOf(r.employees()),
                        text(L.t("Ish haqi: ", "ФОТ: ", "Payroll: ") + compact(c.payroll())), ""),
                kpi("alert", L.t("Mijozlar qarzi", "Долги клиентов", "Customer debt"), compact(r.debtTotal()),
                        text(L.t("Muddati o'tgan: ", "Просрочено: ", "Overdue: ") + compact(r.overdueTotal())),
                        r.overdueTotal() > 0 ? "is-warn" : ""));
        strip.addClassNames("dx-kpis", "bi-kpis");
        return strip;
    }

    private static Div kpi(String icon, String label, String value, Component foot, String tone) {
        Span l = new Span(label);
        l.addClassName("dx-kpi-label");
        Div top = new Div(iconBox(icon), l);
        top.addClassName("dx-kpi-top");
        Span v = new Span(value);
        v.addClassName("dx-kpi-value");
        Div card = new Div(top, v, foot);
        card.addClassName("dx-kpi");
        if (!tone.isBlank()) {
            card.addClassName(tone);
        }
        return card;
    }

    private static Component text(String s) {
        Span f = new Span(s);
        f.addClassName("dx-kpi-foot");
        return f;
    }

    /** O'tgan davrga va o'tgan yilga nisbatan o'zgarish. */
    private static Component delta(double cur, double prev, double year) {
        Div d = new Div();
        d.addClassName("bi-deltas");
        d.add(deltaSpan(cur, prev, L.t("o'tgan davrga", "к прошлому периоду", "vs previous")));
        d.add(deltaSpan(cur, year, L.t("o'tgan yilga", "к прошлому году", "vs last year")));
        return d;
    }

    private static Span deltaSpan(double cur, double base, String tail) {
        Span s = new Span();
        s.addClassName("dx-kpi-foot");
        if (base == 0) {
            s.setText("— " + tail);
            return s;
        }
        double p = (cur - base) * 100 / Math.abs(base);
        Span d = new Span((p >= 0 ? "▲ " : "▼ ") + Math.abs(Math.round(p)) + "%");
        d.addClassNames("dx-delta", p >= 0 ? "is-up" : "is-down");
        s.add(d, new Span(" " + tail));
        return s;
    }

    // ================================================================ moliya

    private Component financeChart(Report r) {
        Div c = card("dx-card dx-chart-card");
        c.add(cardHead(L.t("Daromad, xarajat va foyda (12 oy)", "Выручка, расходы и прибыль (12 мес.)",
                "Revenue, costs and profit (12 months)"), null));
        Div chart = new Div();
        chart.addClassName("dx-chart");
        chart.getElement().appendChild(new Html(barsSvg(r.months())).getElement());
        Div labels = new Div();
        labels.addClassName("bi-xlabels");
        for (Month m : r.months()) {
            labels.add(new Span(m.label()));
        }
        Div legend = new Div(legendItem(0, L.t("Daromad", "Выручка", "Revenue")),
                legendItem(1, L.t("Xarajat va soliqlar", "Расходы и налоги", "Costs and taxes")),
                legendItem(2, L.t("Sof foyda", "Чистая прибыль", "Net profit")));
        legend.addClassName("bi-legend");
        c.add(chart, labels, legend);
        return c;
    }

    /** Sof foyda hisobi: daromaddan soliqlargacha. */
    private Component profitCard(Report r) {
        Totals t = r.cur();
        Div c = card("dx-card");
        c.add(cardHead(L.t("Sof foyda hisobi", "Расчёт чистой прибыли", "Net profit breakdown"),
                pill(pct1(t.margin()) + "%", t.net() < 0 ? "is-danger" : "is-ok")));
        Div list = new Div();
        list.addClassName("bi-wf");
        list.add(wf(L.t("Daromad", "Выручка", "Revenue"), t.revenue(), "is-total"));
        list.add(wf(L.t("Tannarx", "Себестоимость", "Cost of goods"), -t.cogs(), ""));
        list.add(wf(L.t("Xarajatlar", "Расходы", "Operating expenses"), -t.opex(), ""));
        list.add(wf(L.t("Ish haqi", "Зарплата", "Payroll"), -t.payroll(), ""));
        list.add(wf(L.t("Ijtimoiy soliq (12%)", "Соц. налог (12%)", "Social tax (12%)"), -t.socialTax(), ""));
        list.add(wf(L.t("Boshqa to'lovlar", "Прочие платежи", "Other payments"), -t.other(), ""));
        list.add(wf(L.t("Soliq oldi foyda", "Прибыль до налога", "Pre-tax profit"), t.pretax(), "is-total"));
        list.add(wf(L.t("Foyda solig'i", "Налог на прибыль", "Profit tax") + (t.taxEstimated() ? " (15%*)" : ""),
                -t.profitTax(), ""));
        list.add(wf(L.t("Sof foyda", "Чистая прибыль", "Net profit"), t.net(), "is-net"));
        c.add(list);
        if (t.taxEstimated()) {
            Paragraph n = new Paragraph(L.t("* Moliyada soliq to'lovi topilmadi, foyda solig'i taxminan hisoblandi.",
                    "* Платежей по налогам не найдено, налог на прибыль рассчитан приблизительно.",
                    "* No tax payments found in Finance, profit tax is estimated."));
            n.addClassName("dx-note");
            c.add(n);
        }
        return c;
    }

    private static Component wf(String label, double v, String cls) {
        Span l = new Span(label);
        Span a = new Span((v < 0 ? "− " : "") + money(Math.abs(v)));
        a.addClassName(v < 0 && cls.isEmpty() ? "is-minus" : "is-plus");
        Div row = new Div(l, a);
        row.addClassName("bi-wf-row");
        if (!cls.isEmpty()) {
            row.addClassName(cls);
        }
        return row;
    }

    private Component expenseCard(Report r) {
        Div c = card("dx-card dx-panel");
        double tot = r.expenseMix().stream().mapToDouble(Slice::value).sum();
        c.add(cardHead(L.t("Xarajatlar tarkibi", "Структура расходов", "Expense mix"), pill(compact(tot), "")));
        if (r.expenseMix().isEmpty()) {
            c.add(note(L.t("Bu davrda xarajat yo'q.", "Расходов за период нет.", "No expenses in this period.")));
            return c;
        }
        c.add(donutBlock(r.expenseMix(), true));
        return c;
    }

    // ================================================================ qarzlar

    private Component debtCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Mijozlar qarzi", "Долги клиентов", "Customer debt"),
                pill(compact(r.debtTotal()), r.debtTotal() > 0 ? "is-warn" : "is-ok")));
        if (r.debtTotal() <= 0) {
            c.add(note(L.t("Mijozlarning qarzi yo'q.", "Долгов клиентов нет.", "No customer debts.")));
            return c;
        }
        Paragraph sum = new Paragraph(r.debtCustomers() + " " + L.t("ta mijoz qarzdor", "должников", "debtors")
                + " · " + L.t("muddati o'tgan: ", "просрочено: ", "overdue: ") + money(r.overdueTotal()));
        sum.addClassName("dx-note");
        c.add(sum, hbars(r.debtors(), true, "is-warn"));
        Span cap = new Span(L.t("Qarz yoshi", "Возраст долга", "Debt aging"));
        cap.addClassName("dx-cap");
        c.add(cap, hbars(r.aging(), true, "is-bad"));
        return c;
    }

    // ================================================================ CRM

    private Component crmCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("CRM voronkasi", "Воронка CRM", "CRM funnel"), pill(pct1(r.conversion()) + "%", "is-info")));
        double max = r.funnel().get(0).value();
        if (max <= 0) {
            c.add(note(L.t("Bu davrda bitimlar yo'q.", "Сделок за период нет.", "No deals in this period.")));
            return c;
        }
        Div f = new Div();
        f.addClassName("bi-funnel");
        int i = 0;
        for (Slice s : r.funnel()) {
            Span lab = new Span(s.label() + " · " + Math.round(s.value()));
            Div fill = new Div(lab);
            fill.addClassNames("bi-funnel-bar", "bi-c" + (i++));
            fill.getStyle().set("width", Math.max(22, Math.round(s.value() * 100 / max)) + "%");
            f.add(fill);
        }
        c.add(f);
        Paragraph avg = new Paragraph(L.t("O'rtacha bitim: ", "Средняя сделка: ", "Average deal: ") + money(r.avgDeal()));
        avg.addClassName("dx-note");
        Span cap = new Span(L.t("Eng faol mijozlar (TOP 5)", "Топ-5 клиентов", "Top 5 customers"));
        cap.addClassName("dx-cap");
        c.add(avg, cap, r.topCustomers().isEmpty()
                ? note(L.t("Yutilgan bitim yo'q.", "Выигранных сделок нет.", "No won deals."))
                : hbars(r.topCustomers(), true, ""));
        return c;
    }

    // ================================================================ ombor

    private Component stockCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Ombor qiymati", "Стоимость склада", "Stock value"), pill(compact(r.stockValue()), "")));
        if (r.stockByCategory().isEmpty()) {
            c.add(note(L.t("Omborda tovar yo'q.", "На складе нет товаров.", "No stock.")));
            return c;
        }
        c.add(hbars(r.stockByCategory(), true, ""));
        Span cap1 = new Span(L.t("Tez sotiladigan (dona)", "Быстро продаются (шт)", "Fast movers (qty)"));
        cap1.addClassName("dx-cap");
        c.add(cap1, r.fast().isEmpty() ? note("—") : hbars(r.fast(), false, "is-ok"));
        Span cap2 = new Span(L.t("Yotib qolgan (60+ kun)", "Залежавшиеся (60+ дн.)", "Slow stock (60+ days)"));
        cap2.addClassName("dx-cap");
        c.add(cap2, r.dead().isEmpty()
                ? note(L.t("Yotib qolgan tovar yo'q.", "Залежавшихся нет.", "No slow stock."))
                : hbars(r.dead(), true, "is-bad"));
        return c;
    }

    private Component abcCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("ABC tahlil", "ABC-анализ", "ABC analysis"), pill("80 / 15 / 5", "")));
        if (r.abc().stream().allMatch(s -> s.value() == 0)) {
            c.add(note(L.t("Sotuvlar yo'q.", "Продаж нет.", "No sales.")));
            return c;
        }
        c.add(donutBlock(r.abc(), false));
        Paragraph n = new Paragraph(L.t("A: daromadning 80% ini beradigan tovarlar. B: keyingi 15%. C: oxirgi 5%.",
                "A — товары, дающие 80% выручки. B — следующие 15%. C — последние 5%.",
                "A: items giving 80% of revenue. B: next 15%. C: last 5%."));
        n.addClassName("dx-note");
        Span cap = new Span(L.t("A guruh TOP", "Топ группы A", "Top of group A"));
        cap.addClassName("dx-cap");
        c.add(n, cap, hbars(r.abcTop(), true, "is-ok"));
        return c;
    }

    // ================================================================ kadrlar

    private Component hrCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Kadrlar", "Кадры", "People"), pill(r.employees() + " " + L.t("ta", "чел.", "staff"), "")));
        if (r.employees() == 0) {
            c.add(note(L.t("Xodimlar kiritilmagan.", "Сотрудников нет.", "No employees.")));
            return c;
        }
        c.add(hbars(r.departments(), false, ""));
        Totals t = r.cur();
        Paragraph a = new Paragraph(L.t("Ish haqi fondi: ", "Фонд зарплаты: ", "Payroll fund: ") + money(t.payroll()));
        Paragraph b = new Paragraph(L.t("Ijtimoiy soliq bilan: ", "С соц. налогом: ", "With social tax: ")
                + money(t.payroll() + t.socialTax()));
        Paragraph d = new Paragraph(L.t("Daromadga nisbati: ", "Доля от выручки: ", "Share of revenue: ") + pct1(r.payrollShare()) + "%");
        for (Paragraph p : List.of(a, b, d)) {
            p.addClassName("dx-note");
            c.add(p);
        }
        return c;
    }

    // ================================================================ grafiklar

    private static String barsSvg(List<Month> m) {
        int w = 600, h = 220, top = 14, bot = 6, n = m.size();
        double max = 0, min = 0;
        for (Month x : m) {
            max = Math.max(max, Math.max(x.revenue(), x.cost()));
            min = Math.min(min, x.net());
        }
        if (max <= min) {
            max = min + 1;
        }
        double span = max - min, slot = w / (double) n;
        StringBuilder sb = new StringBuilder("<svg class='dx-chart-svg' data-vb='0 0 " + w + " " + h
                + "' data-par='none' viewBox='0 0 " + w + " " + h + "' preserveAspectRatio='none' aria-hidden='true'>");
        double zero = y(0, min, span, h, top, bot);
        sb.append("<line class='dx-gridline' x1='0' x2='").append(w).append("' y1='").append(f(zero))
                .append("' y2='").append(f(zero)).append("' vector-effect='non-scaling-stroke'/>");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double x0 = i * slot + slot * 0.14, bw = slot * 0.34;
            double yr = y(m.get(i).revenue(), min, span, h, top, bot), yc = y(m.get(i).cost(), min, span, h, top, bot);
            sb.append("<rect class='bi-bar bi-c0' x='").append(f(x0)).append("' y='").append(f(yr)).append("' width='")
                    .append(f(bw)).append("' height='").append(f(Math.max(0, zero - yr))).append("'/>");
            sb.append("<rect class='bi-bar bi-c1' x='").append(f(x0 + bw + 2)).append("' y='").append(f(yc)).append("' width='")
                    .append(f(bw)).append("' height='").append(f(Math.max(0, zero - yc))).append("'/>");
            line.append(i == 0 ? "M" : " L").append(f(i * slot + slot / 2)).append(' ')
                    .append(f(y(m.get(i).net(), min, span, h, top, bot)));
        }
        return sb.append("<path class='bi-line bi-c2' d='").append(line)
                .append("' fill='none' vector-effect='non-scaling-stroke'/></svg>").toString();
    }

    private static double y(double v, double min, double span, int h, int top, int bot) {
        return h - bot - (v - min) / span * (h - top - bot);
    }

    private static Component donutBlock(List<Slice> s, boolean asMoney) {
        double tot = s.stream().mapToDouble(Slice::value).sum();
        double circ = 2 * Math.PI * 40, off = 0;
        StringBuilder sb = new StringBuilder("<svg class='bi-donut' data-vb='0 0 100 100' viewBox='0 0 100 100' aria-hidden='true'>");
        for (int i = 0; i < s.size(); i++) {
            double len = tot <= 0 ? 0 : s.get(i).value() / tot * circ;
            sb.append("<circle class='bi-ring bi-s").append(i % 6).append("' cx='50' cy='50' r='40' fill='none' stroke-width='16' ")
                    .append("stroke-dasharray='").append(f(len)).append(' ').append(f(circ - len))
                    .append("' stroke-dashoffset='").append(f(-off)).append("' transform='rotate(-90 50 50)'/>");
            off += len;
        }
        Div ring = new Div();
        ring.addClassName("bi-donut-wrap");
        ring.getElement().appendChild(new Html(sb.append("</svg>").toString()).getElement());
        Div legend = new Div();
        legend.addClassName("bi-legend-col");
        for (int i = 0; i < s.size(); i++) {
            double pc = tot <= 0 ? 0 : s.get(i).value() * 100 / tot;
            legend.add(legendItem(i, s.get(i).label() + " · " + (asMoney ? compact(s.get(i).value()) : Math.round(s.get(i).value()))
                    + " (" + Math.round(pc) + "%)"));
        }
        Div block = new Div(ring, legend);
        block.addClassName("bi-donut-block");
        return block;
    }

    private static Component hbars(List<Slice> s, boolean asMoney, String tone) {
        double max = s.stream().mapToDouble(Slice::value).max().orElse(1);
        Div bars = new Div();
        bars.addClassName("dx-bars");
        for (Slice x : s) {
            Span name = new Span(x.label());
            Span num = new Span(asMoney ? compact(x.value()) : FieldDef.format(x.value()));
            Div t = new Div(name, num);
            t.addClassName("dx-bar-top");
            Div fill = new Div();
            fill.getStyle().set("width", Math.max(4, Math.round(x.value() * 100 / (max <= 0 ? 1 : max))) + "%");
            if (!tone.isBlank()) {
                fill.addClassName(tone);
            }
            Div track = new Div(fill);
            track.addClassName("dx-bar-track");
            Div row = new Div(t, track);
            row.addClassName("dx-bar-row");
            bars.add(row);
        }
        return bars;
    }

    // ================================================================ CSV

    private static String csv(Report r) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        Totals t = r.cur();
        sb.append("Davr;").append(r.range().from()).append(';').append(r.range().to()).append('\n');
        row(sb, "Daromad", t.revenue());
        row(sb, "Tannarx", t.cogs());
        row(sb, "Xarajatlar", t.opex());
        row(sb, "Ish haqi", t.payroll());
        row(sb, "Ijtimoiy soliq", t.socialTax());
        row(sb, "Boshqa to'lovlar", t.other());
        row(sb, "Soliq oldi foyda", t.pretax());
        row(sb, "Foyda solig'i", t.profitTax());
        row(sb, "Sof foyda", t.net());
        row(sb, "Mijozlar qarzi", r.debtTotal());
        row(sb, "Shundan muddati o'tgan", r.overdueTotal());
        row(sb, "Ombor qiymati", r.stockValue());
        sb.append("\nOy;Daromad;Xarajat;Sof foyda\n");
        for (Month m : r.months()) {
            sb.append(m.label()).append(';').append(Math.round(m.revenue())).append(';')
                    .append(Math.round(m.cost())).append(';').append(Math.round(m.net())).append('\n');
        }
        sb.append("\nQarzdor;Summa\n");
        for (Slice s : r.debtors()) {
            row(sb, s.label().replace(';', ','), s.value());
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, String k, double v) {
        sb.append(k).append(';').append(Math.round(v)).append('\n');
    }

    // ================================================================ mayda yordamchilar

    private static Div card(String cls) {
        Div d = new Div();
        for (String c : cls.split(" ")) {
            d.addClassName(c);
        }
        return d;
    }

    private static Component cardHead(String title, Component badge) {
        H2 h = new H2(title);
        h.addClassName("dx-card-title");
        Div head = badge == null ? new Div(h) : new Div(h, badge);
        head.addClassName("dx-card-head");
        return head;
    }

    private static Span pill(String text, String tone) {
        Span s = new Span(text);
        s.addClassName("dx-pill");
        if (!tone.isBlank()) {
            s.addClassName(tone);
        }
        return s;
    }

    private static Paragraph note(String text) {
        Paragraph p = new Paragraph(text);
        p.addClassName("dx-note");
        return p;
    }

    private static Component legendItem(int i, String text) {
        Span dot = new Span();
        dot.addClassNames("bi-dot", "bi-s" + (i % 6));
        Span t = new Span(text);
        Div d = new Div(dot, t);
        d.addClassName("bi-legend-item");
        return d;
    }

    private static Div iconBox(String name) {
        Div d = new Div();
        d.addClassName("dx-ico");
        d.getElement().setAttribute("aria-hidden", "true");
        StringBuilder sb = new StringBuilder("<svg class='dx-svg' data-vb='0 0 24 24' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
                + "stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'>");
        for (String p : ICONS.getOrDefault(name, ICONS.get("box")).split("\\|")) {
            sb.append("<path d='").append(p).append("'/>");
        }
        d.getElement().appendChild(new Html(sb.append("</svg>").toString()).getElement());
        return d;
    }

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String pct1(double d) {
        return String.format(Locale.ROOT, "%.1f", d).replace('.', ',');
    }

    private static String money(double v) {
        return FieldDef.format(v) + " " + L.t("so'm", "сум", "UZS");
    }

    /** 12 400 000 → "12,4 mln so'm". */
    private static String compact(double v) {
        double a = Math.abs(v);
        String s;
        if (a >= 1_000_000_000) {
            s = trim(v / 1_000_000_000) + " " + L.t("mlrd", "млрд", "B");
        } else if (a >= 1_000_000) {
            s = trim(v / 1_000_000) + " " + L.t("mln", "млн", "M");
        } else {
            s = FieldDef.format(v);
        }
        return s + " " + L.t("so'm", "сум", "UZS");
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.1f", d);
        return (s.endsWith(".0") ? s.substring(0, s.length() - 2) : s).replace('.', ',');
    }

    @Override
    public String getPageTitle() {
        return "DOKONIX — " + L.t("BI dashboard", "BI дашборд", "BI dashboard");
    }
}