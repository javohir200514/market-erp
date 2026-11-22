package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.BiUi.*;

import com.universal.erp.platform.ui.BiInsights.Slice;
import com.universal.erp.platform.ui.BiSalesInsights.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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
import java.util.ArrayList;
import java.util.List;

/**
 * BI: Savdo tahlili. Hamma summalar so'mda.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiSalesView.java
 */
@Route(value = "app/bi/sales", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/dashboard.css")
@CssImport("./styles/bi.css")
public class BiSalesView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private final BiSalesInsights service;
    private final Div body = new Div();
    private final BiPeriodBar periodBar = new BiPeriodBar(this::refresh);
    private String company = "";

    public BiSalesView(BiSalesInsights service) {
        this.service = service;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        company = CurrentCompany.get();
        removeAll();
        addClassNames("dx", "bi");
        H1 title = new H1(L.t("Savdo tahlili", "Анализ продаж", "Sales analysis"));
        title.addClassName("dx-title");
        Paragraph sub = new Paragraph(L.t("Nima, qachon, kimga va qancha foyda bilan sotilyapti. Summalar so'mda.",
                "Что, когда, кому и с какой прибылью продаётся. Суммы в сумах.",
                "What sells, when, to whom and at what profit. Amounts in UZS."));
        sub.addClassName("dx-sub");
        Div head = new Div(new Div(title, sub), periodBar);
        head.addClassName("dx-head");
        add(head, body);
        refresh();
    }

    private void refresh() {
        Report r = service.load(company, periodBar.range());
        body.removeAll();
        body.add(toolbar(r), insights(r), kpis(r));

        Div main = new Div(trendCard(r), paymentCard(r));
        main.addClassName("dx-main");
        Div row2 = new Div(weekdayCard(r), hourCard(r), categoryCard(r));
        row2.addClassName("dx-grid3");
        Div row3 = new Div(topCard(L.t("Eng ko'p daromad", "Топ по выручке", "Top by revenue"), r.byRevenue(), ""),
                topCard(L.t("Eng ko'p yalpi foyda", "Топ по валовой прибыли", "Top by gross profit"), r.byProfit(), "is-ok"),
                lowMarginCard(r));
        row3.addClassName("dx-grid3");
        Div row4 = new Div(cashierCard(r), customerCard(r), returnCard(r));
        row4.addClassName("dx-grid3");
        body.add(main, row2, row3, row4);
        getElement().executeJs(FIX_SVG, getElement());
    }

    private Component toolbar(Report r) {
        Span period = new Span(r.range().from() + " — " + r.range().to());
        period.addClassName("dx-date");
        byte[] data = csv(r).getBytes(StandardCharsets.UTF_8);
        Anchor export = new Anchor(new StreamResource("savdo-" + r.range().from() + "_" + r.range().to() + ".csv",
                () -> new ByteArrayInputStream(data)), L.t("CSV yuklab olish", "Скачать CSV", "Download CSV"));
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

    private Component insights(Report r) {
        Div box = new Div();
        box.addClassName("bi-ins");
        r.insights().forEach(s -> {
            Span chip = new Span(s);
            chip.addClassName("bi-ins-item");
            box.add(chip);
        });
        box.setVisible(!r.insights().isEmpty());
        return box;
    }

    private Component kpis(Report r) {
        Totals c = r.cur(), p = r.prev(), y = r.year();
        Div strip = new Div(
                kpi("trend", L.t("Savdo summasi", "Сумма продаж", "Sales"), compact(c.revenue()), deltas(c.revenue(), p.revenue(), y.revenue()), ""),
                kpi("receipt", L.t("Cheklar soni", "Количество чеков", "Receipts"), FieldDef.format(c.receipts()), deltas(c.receipts(), p.receipts(), y.receipts()), ""),
                kpi("wallet", L.t("O'rtacha chek", "Средний чек", "Average receipt"), compact(c.avgReceipt()), deltas(c.avgReceipt(), p.avgReceipt(), y.avgReceipt()), ""),
                kpi("box", L.t("Sotilgan dona", "Продано шт.", "Units sold"), FieldDef.format(c.qty()), deltas(c.qty(), p.qty(), y.qty()), ""),
                kpi("chart", L.t("Yalpi foyda", "Валовая прибыль", "Gross profit"), compact(c.gross()), deltas(c.gross(), p.gross(), y.gross()), c.gross() < 0 ? "is-warn" : ""),
                kpi("activity", L.t("Yalpi marja", "Валовая маржа", "Gross margin"), pct1(c.margin()) + "%", text(L.t("O'tgan davr: ", "Прошлый период: ", "Previous: ") + pct1(p.margin()) + "%"), ""),
                kpi("alert", L.t("Qaytarishlar", "Возвраты", "Returns"), compact(c.returns()), text(c.revenue() + c.returns() <= 0 ? "—" : pct1(c.returns() * 100 / (c.revenue() + c.returns())) + "% " + L.t("savdodan", "от продаж", "of sales")), c.returns() > 0 ? "is-warn" : ""),
                kpi("users", L.t("Xaridor mijozlar", "Покупающие клиенты", "Buying customers"), String.valueOf(c.customers()), text(L.t("O'tgan davr: ", "Прошлый период: ", "Previous: ") + p.customers()), ""));
        strip.addClassNames("dx-kpis", "bi-kpis");
        return strip;
    }

    // ---------------------------------------------------------------- kartalar

    private Component trendCard(Report r) {
        List<Point> t = r.trend();
        Div c = card("dx-card dx-chart-card");
        c.add(cardHead(L.t("Savdo dinamikasi", "Динамика продаж", "Sales trend"), pill(compact(r.cur().revenue()), "")));
        double[] cur = t.stream().mapToDouble(Point::value).toArray();
        double[] prev = t.stream().mapToDouble(Point::prev).toArray();
        Div chart = new Div();
        chart.addClassName("dx-chart");
        chart.getElement().appendChild(new Html(lineSvg(cur, r.hasPrev() ? prev : null)).getElement());
        Div hits = new Div();
        hits.addClassName("dx-hits");
        for (Point p : t) {
            Div hit = new Div();
            hit.addClassName("dx-hit");
            hit.getElement().setAttribute("tabindex", "0");
            hit.getElement().setAttribute("data-tip", p.label() + " · " + money(p.value()));
            hits.add(hit);
        }
        chart.add(hits);
        if (r.cur().revenue() <= 0) {
            Span empty = new Span(L.t("Bu davrda savdo yo'q", "За период продаж нет", "No sales in this period"));
            empty.addClassName("dx-chart-empty");
            chart.add(empty);
        }
        Div labels = new Div();
        labels.addClassName("dx-xlabels");
        int step = Math.max(1, (int) Math.ceil(t.size() / 8.0));
        for (int i = 0; i < t.size(); i += step) {
            labels.add(new Span(t.get(i).label()));
        }
        c.add(chart, labels);
        if (r.hasPrev()) {
            Div lg = new Div(legendItem(0, L.t("Joriy davr", "Текущий период", "Current")),
                    legendItem(5, L.t("O'tgan davr (punktir)", "Прошлый период (пунктир)", "Previous (dashed)")));
            lg.addClassName("bi-legend");
            c.add(lg);
        }
        return c;
    }

    private Component paymentCard(Report r) {
        Div c = card("dx-card");
        c.add(cardHead(L.t("To'lov turlari", "Способы оплаты", "Payment methods"), null));
        c.add(r.payments().isEmpty() ? note(L.t("Ma'lumot yo'q.", "Нет данных.", "No data.")) : donutBlock(r.payments(), true));
        return c;
    }

    private Component weekdayCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Hafta kunlari bo'yicha", "По дням недели", "By weekday"), null));
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 7; i++) labels.add(BiSalesInsights.dayName(i));
        c.add(r.cur().revenue() <= 0 ? note("—") : cols(labels, r.weekday()));
        return c;
    }

    private Component hourCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Soatlar bo'yicha", "По часам", "By hour"), null));
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < r.hours().length; i++) labels.add(String.valueOf(r.hourFrom() + i));
        c.add(r.cur().revenue() <= 0 ? note("—") : cols(labels, r.hours()));
        return c;
    }

    private Component categoryCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Toifalar bo'yicha", "По категориям", "By category"), null));
        c.add(r.categories().isEmpty() ? note("—") : donutBlock(r.categories(), true));
        return c;
    }

    private Component topCard(String title, List<Slice> items, String tone) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(title, null));
        c.add(items.isEmpty() ? note(L.t("Ma'lumot yo'q.", "Нет данных.", "No data.")) : hbars(items, true, tone));
        return c;
    }

    private Component lowMarginCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Past marjali tovarlar", "Товары с низкой маржой", "Low-margin items"),
                pill(r.lowMargin().size() + "", r.lowMargin().isEmpty() ? "is-ok" : "is-warn")));
        if (r.lowMargin().isEmpty()) {
            c.add(note(L.t("Marjasi 10% dan past tovar yo'q.", "Товаров с маржой ниже 10% нет.", "No items under 10% margin.")));
            return c;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (Slice s : r.lowMargin()) {
            Span name = new Span(s.label());
            name.addClassName("dx-row-title");
            Div main = new Div(name);
            main.addClassName("dx-row-main");
            Span m = pill(pct1(s.value()) + "%", s.value() < 0 ? "is-danger" : "is-warn");
            Div row = new Div(main, m);
            row.addClassName("dx-row");
            list.add(row);
        }
        c.add(list);
        return c;
    }

    private Component cashierCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Kassirlar", "Кассиры", "Cashiers"), null));
        if (r.cashiers().isEmpty()) {
            c.add(note(L.t("Ma'lumot yo'q.", "Нет данных.", "No data.")));
            return c;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (Cashier k : r.cashiers()) {
            Span name = new Span(k.name());
            name.addClassName("dx-row-title");
            Span sub = new Span(k.receipts() + " " + L.t("ta chek", "чеков", "receipts"));
            sub.addClassName("dx-row-sub");
            Div main = new Div(name, sub);
            main.addClassName("dx-row-main");
            Div row = new Div(main, pill(compact(k.revenue()), ""));
            row.addClassName("dx-row");
            list.add(row);
        }
        c.add(list);
        return c;
    }

    private Component customerCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Eng yirik xaridorlar", "Крупнейшие покупатели", "Top buyers"), null));
        c.add(r.customers().isEmpty() ? note(L.t("Mijozga bog'langan savdo yo'q.", "Нет продаж с привязкой к клиентам.", "No sales linked to customers."))
                : hbars(r.customers(), true, ""));
        return c;
    }

    private Component returnCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Qaytarilgan tovarlar", "Возвращённые товары", "Returned items"), pill(compact(r.cur().returns()), r.cur().returns() > 0 ? "is-warn" : "is-ok")));
        c.add(r.returned().isEmpty() ? note(L.t("Qaytarish yo'q.", "Возвратов нет.", "No returns.")) : hbars(r.returned(), true, "is-bad"));
        return c;
    }

    private static String csv(Report r) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        Totals t = r.cur();
        sb.append("Davr;").append(r.range().from()).append(';').append(r.range().to()).append('\n');
        row(sb, "Savdo summasi", t.revenue());
        row(sb, "Cheklar soni", t.receipts());
        row(sb, "O'rtacha chek", t.avgReceipt());
        row(sb, "Sotilgan dona", t.qty());
        row(sb, "Yalpi foyda", t.gross());
        row(sb, "Qaytarishlar", t.returns());
        sb.append("\nSana;Savdo\n");
        r.trend().forEach(p -> row(sb, p.label(), p.value()));
        sb.append("\nTovar;Daromad\n");
        r.byRevenue().forEach(s -> row(sb, s.label().replace(';', ','), s.value()));
        sb.append("\nTovar;Yalpi foyda\n");
        r.byProfit().forEach(s -> row(sb, s.label().replace(';', ','), s.value()));
        return sb.toString();
    }

    private static void row(StringBuilder sb, String k, double v) {
        sb.append(k).append(';').append(Math.round(v)).append('\n');
    }

    @Override
    public String getPageTitle() {
        return "DOKONIX — " + L.t("Savdo tahlili", "Анализ продаж", "Sales analysis");
    }
}