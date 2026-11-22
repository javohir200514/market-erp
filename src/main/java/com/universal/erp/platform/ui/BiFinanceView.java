package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.BiUi.*;

import com.universal.erp.platform.ui.BiFinanceInsights.*;
import com.universal.erp.platform.ui.BiInsights.Slice;
import com.universal.erp.platform.ui.BiInsights.Totals;
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
import java.util.List;

/**
 * BI: Moliyaviy tahlil. Hamma summalar so'mda.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiFinanceView.java
 */
@Route(value = "app/bi/finance", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/dashboard.css")
@CssImport("./styles/bi.css")
public class BiFinanceView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private final BiFinanceInsights service;
    private final Div body = new Div();
    private final BiPeriodBar periodBar = new BiPeriodBar(this::refresh);
    private String company = "";

    public BiFinanceView(BiFinanceInsights service) {
        this.service = service;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        company = CurrentCompany.get();
        removeAll();
        addClassNames("dx", "bi");
        H1 title = new H1(L.t("Moliyaviy tahlil", "Финансовый анализ", "Financial analysis"));
        title.addClassName("dx-title");
        Paragraph sub = new Paragraph(L.t("Foyda va zarar, pul oqimi, zararsizlik nuqtasi, soliqlar va qarzlar. Summalar so'mda.",
                "Прибыли и убытки, денежный поток, безубыточность, налоги и долги. Суммы в сумах.",
                "Profit and loss, cash flow, break-even, taxes and debts. Amounts in UZS."));
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
        Div main = new Div(cashCard(r), plCard(r));
        main.addClassName("dx-main");
        Div row2 = new Div(breakEvenCard(r), forecastCard(r), ratiosCard(r));
        row2.addClassName("dx-grid3");
        Div row3 = new Div(categoriesCard(r), taxCard(r), debtsCard(r));
        row3.addClassName("dx-grid3");
        body.add(main, row2, row3);
        getElement().executeJs(FIX_SVG, getElement());
    }

    // ---------------------------------------------------------------- yuqori qism

    private Component toolbar(Report r) {
        Span period = new Span(r.range().from() + " — " + r.range().to());
        period.addClassName("dx-date");
        byte[] data = csv(r).getBytes(StandardCharsets.UTF_8);
        Anchor export = new Anchor(new StreamResource("moliya-" + r.range().from() + "_" + r.range().to() + ".csv",
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
        Totals c = r.cur(), p = r.prev();
        String vs = L.t("o'tgan davrga", "к прошлому периоду", "vs previous");
        double cost = c.revenue() - c.net(), pcost = p.revenue() - p.net();
        Div strip = new Div(
                kpi("trend", L.t("Daromad", "Выручка", "Revenue"), compact(c.revenue()), deltaSpan(c.revenue(), p.revenue(), vs), ""),
                kpi("wallet", L.t("Jami xarajat", "Все расходы", "Total costs"), compact(cost), deltaSpan(cost, pcost, vs), ""),
                kpi("chart", L.t("Sof foyda", "Чистая прибыль", "Net profit"), compact(c.net()), deltaSpan(c.net(), p.net(), vs), c.net() < 0 ? "is-warn" : ""),
                kpi("wallet", L.t("Pul qoldig'i", "Остаток денег", "Cash on hand"), compact(r.cash()), text(L.t("Sof pozitsiya: ", "Чистая позиция: ", "Net position: ") + compact(r.netPosition())), ""),
                kpi("users", L.t("Debitorlik", "Дебиторка", "Receivables"), compact(r.recvTotal()), text(L.t("Muddati o'tgan: ", "Просрочено: ", "Overdue: ") + compact(r.recvOverdue())), r.recvOverdue() > 0 ? "is-warn" : ""),
                kpi("alert", L.t("Kreditorlik", "Кредиторка", "Payables"), compact(r.payTotal()), text(L.t("Muddati o'tgan: ", "Просрочено: ", "Overdue: ") + compact(r.payOverdue())), r.payOverdue() > 0 ? "is-warn" : ""),
                kpi("receipt", L.t("Soliq yuki", "Налоговая нагрузка", "Tax burden"), pct1(r.ratios().taxLoad()) + "%", text(L.t("Daromadga nisbatan", "От выручки", "Of revenue")), ""),
                kpi("activity", L.t("Zararsizlik nuqtasi", "Точка безубыточности", "Break-even"), r.breakEvenKnown() ? compact(r.breakEven()) : "—",
                        text(L.t("Davr uchun daromad", "Выручка за период", "Revenue for the period")), ""));
        strip.addClassNames("dx-kpis", "bi-kpis");
        return strip;
    }

    // ---------------------------------------------------------------- pul oqimi va P&L

    private Component cashCard(Report r) {
        Div c = card("dx-card dx-chart-card");
        c.add(cardHead(L.t("Pul oqimi (12 oy)", "Денежный поток (12 мес.)", "Cash flow (12 months)"),
                pill(L.t("Qoldiq: ", "Остаток: ", "Balance: ") + compact(r.cash()), "")));
        Div chart = new Div();
        chart.addClassName("dx-chart");
        chart.getElement().appendChild(new Html(flowSvg(r.flows())).getElement());
        Div labels = new Div();
        labels.addClassName("bi-xlabels");
        r.flows().forEach(f -> labels.add(new Span(f.label())));
        Div legend = new Div(legendItem(2, L.t("Kirim", "Приход", "Cash in")),
                legendItem(1, L.t("Chiqim (soliqlar bilan)", "Расход (с налогами)", "Cash out (with taxes)")),
                legendItem(3, L.t("Pul qoldig'i", "Остаток денег", "Cash balance")));
        legend.addClassName("bi-legend");
        c.add(chart, labels, legend);
        return c;
    }

    private static String flowSvg(List<Flow> m) {
        int w = 600, h = 220, top = 14, bot = 6, n = m.size();
        double max = 0, min = 0;
        for (Flow x : m) {
            max = Math.max(max, Math.max(Math.max(x.in(), x.out()), x.balance()));
            min = Math.min(min, x.balance());
        }
        if (max <= min) max = min + 1;
        double span = max - min, slot = w / (double) n;
        StringBuilder sb = new StringBuilder("<svg class='dx-chart-svg' data-vb='0 0 " + w + " " + h
                + "' data-par='none' viewBox='0 0 " + w + " " + h + "' preserveAspectRatio='none' aria-hidden='true'>");
        double zero = h - bot - (0 - min) / span * (h - top - bot);
        sb.append("<line class='dx-gridline' x1='0' x2='").append(w).append("' y1='").append(f(zero)).append("' y2='")
                .append(f(zero)).append("' vector-effect='non-scaling-stroke'/>");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double x0 = i * slot + slot * 0.14, bw = slot * 0.34;
            double yi = h - bot - (m.get(i).in() - min) / span * (h - top - bot);
            double yo = h - bot - (m.get(i).out() - min) / span * (h - top - bot);
            double yb = h - bot - (m.get(i).balance() - min) / span * (h - top - bot);
            sb.append("<rect class='bi-bar bi-c2' x='").append(f(x0)).append("' y='").append(f(yi)).append("' width='")
                    .append(f(bw)).append("' height='").append(f(Math.max(0, zero - yi))).append("'/>");
            sb.append("<rect class='bi-bar bi-c1' x='").append(f(x0 + bw + 2)).append("' y='").append(f(yo)).append("' width='")
                    .append(f(bw)).append("' height='").append(f(Math.max(0, zero - yo))).append("'/>");
            line.append(i == 0 ? "M" : " L").append(f(i * slot + slot / 2)).append(' ').append(f(yb));
        }
        return sb.append("<path class='bi-line bi-c3' d='").append(line)
                .append("' fill='none' vector-effect='non-scaling-stroke'/></svg>").toString();
    }

    private Component plCard(Report r) {
        Totals c = r.cur(), p = r.prev();
        Div card = card("dx-card");
        card.add(cardHead(L.t("Foyda va zarar", "Прибыли и убытки", "Profit and loss"),
                pill(pct1(c.margin()) + "%", c.net() < 0 ? "is-danger" : "is-ok")));
        Div t = new Div();
        t.addClassName("bi-pl");
        t.add(plRow(L.t("Ko'rsatkich", "Показатель", "Item"), L.t("Joriy", "Текущий", "Current"),
                L.t("O'tgan", "Прошлый", "Previous"), "", "is-head"));
        t.add(pl(L.t("Daromad", "Выручка", "Revenue"), c.revenue(), p.revenue(), false, "is-total"));
        t.add(pl(L.t("Tannarx", "Себестоимость", "Cost of goods"), -c.cogs(), -p.cogs(), true, ""));
        t.add(pl(L.t("Yalpi foyda", "Валовая прибыль", "Gross profit"), c.revenue() - c.cogs(), p.revenue() - p.cogs(), false, "is-total"));
        t.add(pl(L.t("Xarajatlar", "Расходы", "Operating expenses"), -c.opex(), -p.opex(), true, ""));
        t.add(pl(L.t("Ish haqi", "Зарплата", "Payroll"), -c.payroll(), -p.payroll(), true, ""));
        t.add(pl(L.t("Ijtimoiy soliq", "Соц. налог", "Social tax"), -c.socialTax(), -p.socialTax(), true, ""));
        t.add(pl(L.t("Boshqa to'lovlar", "Прочие платежи", "Other payments"), -c.other(), -p.other(), true, ""));
        t.add(pl(L.t("Soliq oldi foyda", "Прибыль до налога", "Pre-tax profit"), c.pretax(), p.pretax(), false, "is-total"));
        t.add(pl(L.t("Foyda solig'i", "Налог на прибыль", "Profit tax") + (c.taxEstimated() ? "*" : ""), -c.profitTax(), -p.profitTax(), true, ""));
        t.add(pl(L.t("Sof foyda", "Чистая прибыль", "Net profit"), c.net(), p.net(), false, "is-net"));
        card.add(t);
        if (c.taxEstimated()) {
            card.add(note(L.t("* Foyda solig'i taxminan hisoblangan (15%).", "* Налог на прибыль рассчитан приблизительно (15%).",
                    "* Profit tax is estimated (15%).")));
        }
        return card;
    }

    private static Component pl(String label, double cur, double prev, boolean costRow, String cls) {
        String ch = "—";
        if (prev != 0) {
            double d = (cur - prev) * 100 / Math.abs(prev);
            ch = (d >= 0 ? "▲ " : "▼ ") + Math.abs(Math.round(d)) + "%";
        }
        Div row = (Div) plRow(label, signed(cur), signed(prev), ch, cls);
        if (prev != 0 && ch.length() > 1) {
            boolean up = cur > prev;
            row.getChildren().reduce((a, b) -> b).ifPresent(last ->
                    last.getElement().getClassList().add(up ? "is-up" : "is-down"));
        }
        return row;
    }

    private static Component plRow(String a, String b, String c, String d, String cls) {
        Div row = new Div(new Span(a), new Span(b), new Span(c), new Span(d));
        row.addClassName("bi-pl-row");
        if (!cls.isEmpty()) row.addClassName(cls);
        return row;
    }

    private static String signed(double v) {
        return (v < 0 ? "− " : "") + compact(Math.abs(v)).replace(" " + L.t("so'm", "сум", "UZS"), "");
    }

    // ---------------------------------------------------------------- 2-qator

    private Component breakEvenCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Zararsizlik nuqtasi", "Точка безубыточности", "Break-even point"),
                pill(r.breakEvenKnown() ? pct1(r.safety()) + "%" : "—", r.safety() >= 0 ? "is-ok" : "is-danger")));
        if (!r.breakEvenKnown()) {
            c.add(note(L.t("Hisoblash uchun daromad va xarajat ma'lumotlari kerak.", "Нужны данные по выручке и расходам.",
                    "Revenue and cost data are needed.")));
            return c;
        }
        Div bar = new Div();
        bar.addClassName("dx-progress");
        Div fill = new Div();
        fill.getStyle().set("width", Math.max(2, Math.min(100, Math.round(r.cur().revenue() * 100 / r.breakEven()))) + "%");
        bar.add(fill);
        c.add(bar,
                note(L.t("Doimiy xarajatlar: ", "Постоянные расходы: ", "Fixed costs: ") + money(r.fixedCosts())),
                note(L.t("Zararsizlik uchun daromad: ", "Выручка для безубыточности: ", "Revenue to break even: ") + money(r.breakEven())),
                note(L.t("Hozirgi daromad: ", "Текущая выручка: ", "Current revenue: ") + money(r.cur().revenue())),
                note(r.safety() >= 0
                        ? L.t("Xavfsizlik zaxirasi: daromad " + Math.round(r.safety()) + "% ga tushsa ham zarar bo'lmaydi.",
                                "Запас прочности: выручка может упасть на " + Math.round(r.safety()) + "% без убытка.",
                                "Safety margin: revenue can fall " + Math.round(r.safety()) + "% before a loss.")
                        : L.t("Zararsizlik uchun daromadni " + money(r.breakEven() - r.cur().revenue()) + " ga oshirish kerak.",
                                "Для безубыточности выручку нужно увеличить на " + money(r.breakEven() - r.cur().revenue()) + ".",
                                "Revenue must grow by " + money(r.breakEven() - r.cur().revenue()) + " to break even.")));
        return c;
    }

    private Component forecastCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Keyingi oy prognozi", "Прогноз на след. месяц", "Next month forecast"), pill(L.t("Taxminiy", "Оценка", "Estimate"), "is-info")));
        if (!r.fcKnown()) {
            c.add(note(L.t("Prognoz uchun kamida bir necha oylik savdo kerak.", "Для прогноза нужна история за несколько месяцев.",
                    "A few months of sales history are needed.")));
            return c;
        }
        c.add(hbars(List.of(new Slice(L.t("Daromad", "Выручка", "Revenue"), r.fcRevenue()),
                new Slice(L.t("Sof foyda", "Чистая прибыль", "Net profit"), Math.max(0, r.fcNet()))), true, ""),
                note(r.fcNet() < 0 ? L.t("Prognoz bo'yicha sof foyda manfiy: " + money(r.fcNet()), "Прогноз чистой прибыли отрицателен: " + money(r.fcNet()),
                        "Forecast net profit is negative: " + money(r.fcNet())) : ""),
                note(L.t("Oxirgi 6 oy trendi bo'yicha chiziqli hisob. Bu kafolat emas.", "Линейный тренд за последние 6 мес. Это не гарантия.",
                        "Linear trend over the last 6 months. Not a guarantee.")));
        return c;
    }

    private Component ratiosCard(Report r) {
        Ratios x = r.ratios();
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Moliyaviy ko'rsatkichlar", "Финансовые показатели", "Financial ratios"), null));
        Div t = new Div();
        t.addClassName("bi-wf");
        t.add(ratio(L.t("Yalpi marja", "Валовая маржа", "Gross margin"), pct1(x.gross()) + "%"));
        t.add(ratio(L.t("Operatsion marja", "Операционная маржа", "Operating margin"), pct1(x.operating()) + "%"));
        t.add(ratio(L.t("Sof marja", "Чистая маржа", "Net margin"), pct1(x.net()) + "%"));
        t.add(ratio(L.t("Xarajatlar ulushi", "Доля расходов", "Cost share"), pct1(x.costShare()) + "%"));
        t.add(ratio(L.t("Ish haqi ulushi (soliq bilan)", "Доля ФОТ (с налогом)", "Payroll share (with tax)"), pct1(x.payrollShare()) + "%"));
        t.add(ratio(L.t("Soliq yuki", "Налоговая нагрузка", "Tax burden"), pct1(x.taxLoad()) + "%"));
        t.add(ratio(L.t("Qarz yig'ish muddati", "Срок сбора долга", "Days to collect"), Math.round(x.dso()) + " " + L.t("kun", "дн.", "days")));
        c.add(t);
        return c;
    }

    private static Component ratio(String label, String value) {
        Div row = new Div(new Span(label), new Span(value));
        row.addClassName("bi-wf-row");
        return row;
    }

    // ---------------------------------------------------------------- 3-qator

    private Component categoriesCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Xarajat toifalari", "Категории расходов", "Expense categories"), null));
        if (r.cats().isEmpty()) {
            c.add(note(L.t("Bu davrda xarajat yo'q.", "Расходов за период нет.", "No expenses in this period.")));
            return c;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (Cat k : r.cats()) {
            Span name = new Span(k.label());
            name.addClassName("dx-row-title");
            Span sub = new Span(compact(k.cur()));
            sub.addClassName("dx-row-sub");
            Div main = new Div(name, sub);
            main.addClassName("dx-row-main");
            String tone = "";
            String txt = "—";
            if (k.prev() > 0) {
                double d = (k.cur() - k.prev()) * 100 / k.prev();
                txt = (d >= 0 ? "▲ " : "▼ ") + Math.abs(Math.round(d)) + "%";
                tone = d > 20 ? "is-danger" : d > 0 ? "is-warn" : "is-ok";
            }
            Div row = new Div(main, pill(txt, tone));
            row.addClassName("dx-row");
            list.add(row);
        }
        c.add(list);
        return c;
    }

    private Component taxCard(Report r) {
        Div c = card("dx-card dx-panel");
        double tot = r.taxes().stream().mapToDouble(Slice::value).sum();
        c.add(cardHead(L.t("Soliqlar", "Налоги", "Taxes"), pill(compact(tot), "")));
        if (r.taxes().isEmpty()) {
            c.add(note(L.t("Bu davrda soliq hisoblanmadi.", "Налоги за период не начислены.", "No taxes for this period.")));
            return c;
        }
        c.add(donutBlock(r.taxes(), true),
                note(L.t("Daromadga nisbatan: ", "От выручки: ", "Of revenue: ") + pct1(r.ratios().taxLoad()) + "%"));
        if (r.cur().taxEstimated()) {
            c.add(note(L.t("Foyda solig'i taxminiy (15%): Moliyada soliq to'lovi topilmadi.",
                    "Налог на прибыль ориентировочный (15%): платежей в Финансах нет.",
                    "Profit tax is estimated (15%): no tax payments in Finance.")));
        }
        return c;
    }

    private Component debtsCard(Report r) {
        Div c = card("dx-card dx-panel");
        c.add(cardHead(L.t("Qarzlar balansi", "Баланс долгов", "Debt balance"),
                pill(compact(r.netPosition()), r.netPosition() >= 0 ? "is-ok" : "is-danger")));
        c.add(note(L.t("Pul + debitorlik − kreditorlik = ", "Деньги + дебиторка − кредиторка = ", "Cash + receivables − payables = ") + money(r.netPosition())));
        c.add(cap(L.t("Mijozlar qarzi (debitorlik)", "Долги клиентов (дебиторка)", "Customer debt (receivables)")));
        c.add(r.recvTotal() <= 0 ? note(L.t("Mijozlar qarzi yo'q.", "Долгов клиентов нет.", "No customer debts."))
                : hbars(r.aging(), true, "is-warn"));
        if (!r.debtors().isEmpty()) {
            c.add(cap(L.t("Eng katta qarzdorlar", "Крупнейшие должники", "Largest debtors")), hbars(r.debtors(), true, "is-bad"));
        }
        c.add(cap(L.t("Yetkazib beruvchilarga qarz (kreditorlik)", "Долги поставщикам (кредиторка)", "Supplier debt (payables)")));
        c.add(r.suppliers().isEmpty() ? note(L.t("Qarz yo'q.", "Долгов нет.", "No debts.")) : hbars(r.suppliers(), true, "is-bad"));
        return c;
    }

    // ---------------------------------------------------------------- CSV

    private static String csv(Report r) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        Totals t = r.cur(), p = r.prev();
        sb.append("Davr;").append(r.range().from()).append(';').append(r.range().to()).append('\n');
        sb.append("Ko'rsatkich;Joriy;O'tgan\n");
        row(sb, "Daromad", t.revenue(), p.revenue());
        row(sb, "Tannarx", t.cogs(), p.cogs());
        row(sb, "Xarajatlar", t.opex(), p.opex());
        row(sb, "Ish haqi", t.payroll(), p.payroll());
        row(sb, "Ijtimoiy soliq", t.socialTax(), p.socialTax());
        row(sb, "Boshqa to'lovlar", t.other(), p.other());
        row(sb, "Soliq oldi foyda", t.pretax(), p.pretax());
        row(sb, "Foyda solig'i", t.profitTax(), p.profitTax());
        row(sb, "Sof foyda", t.net(), p.net());
        sb.append("\nOy;Kirim;Chiqim;Sof;Qoldiq\n");
        for (Flow f : r.flows()) {
            sb.append(f.label()).append(';').append(Math.round(f.in())).append(';').append(Math.round(f.out())).append(';')
                    .append(Math.round(f.net())).append(';').append(Math.round(f.balance())).append('\n');
        }
        sb.append("\nPul qoldig'i;").append(Math.round(r.cash())).append('\n');
        sb.append("Debitorlik;").append(Math.round(r.recvTotal())).append('\n');
        sb.append("Kreditorlik;").append(Math.round(r.payTotal())).append('\n');
        sb.append("Zararsizlik nuqtasi;").append(Math.round(r.breakEven())).append('\n');
        return sb.toString();
    }

    private static void row(StringBuilder sb, String k, double a, double b) {
        sb.append(k).append(';').append(Math.round(a)).append(';').append(Math.round(b)).append('\n');
    }

    @Override
    public String getPageTitle() {
        return "DOKONIX — " + L.t("Moliyaviy tahlil", "Финансовый анализ", "Financial analysis");
    }
}