package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bosh sahifa (dashboard): oq fon, mobilga moslashgan.
 * Stil: frontend/styles/dashboard.css (dx-* klasslari).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/DashboardView.java
 */
@Route(value = "app", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/dashboard.css")
public class DashboardView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final String[] UZ_MONTH = {"yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul",
            "avgust", "sentabr", "oktabr", "noyabr", "dekabr"};
    private static final String[] RU_MONTH = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля",
            "августа", "сентября", "октября", "ноября", "декабря"};
    private static final String[] EN_MONTH = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};
    private static final String[] UZ_DAY = {"Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba",
            "Yakshanba"};
    private static final String[] RU_DAY = {"понедельник", "вторник", "среда", "четверг", "пятница", "суббота",
            "воскресенье"};
    private static final String[] EN_DAY = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Sunday"};

    /** Ikonkalar (24x24, stroke). Bir nechta yo'l '|' bilan ajratilgan. */
    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("activity", "M22 12h-4l-3 9L9 3l-3 9H2"),
            Map.entry("alert", "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z|M12 9v4|M12 17h.01"),
            Map.entry("grid", "M3 3h7v7H3z|M14 3h7v7h-7z|M14 14h7v7h-7z|M3 14h7v7H3z"),
            Map.entry("clock", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0|M12 7v5l3 2"),
            Map.entry("wallet", "M3 5a2 2 0 0 1 2-2h13v4|M3 5v14a2 2 0 0 0 2 2h16V7H5a2 2 0 0 1-2-2|M16 13.5h.01"),
            Map.entry("box", "M21 8l-9-5-9 5v8l9 5 9-5z|M3.3 7.5L12 12l8.7-4.5|M12 22V12"),
            Map.entry("users", "M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2|M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8|M21 21v-2a4 4 0 0 0-3-3.9|M16 3.1a4 4 0 0 1 0 7.8"),
            Map.entry("briefcase", "M3 8h18v12H3z|M8 8V5a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v3|M3 13h18"),
            Map.entry("chart", "M3 3v18h18|M7 15l4-4 3 3 5-6"),
            Map.entry("plus", "M12 5v14|M5 12h14"),
            Map.entry("arrow", "M5 12h14|M13 6l6 6-6 6"),
            Map.entry("trend", "M3 17l6-6 4 4 8-8|M15 7h6v6"),
            Map.entry("in", "M12 5v14|M6 13l6 6 6-6"),
            Map.entry("out", "M12 19V5|M6 11l6-6 6 6"),
            Map.entry("clipboard", "M9 3h6v4H9z|M7 5H5a1 1 0 0 0-1 1v15a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1h-2|M8 13l2.5 2.5L16 10"),
            Map.entry("check", "M5 12.5l4.5 4.5L19 7"),
            Map.entry("receipt", "M6 3h12v18l-3-2-3 2-3-2-3 2z|M9 8h6|M9 12h6"),
            Map.entry("calendar", "M4 6h16v15H4z|M4 10h16|M8 3v4|M16 3v4"),
            Map.entry("hourglass", "M6 3h12|M6 21h12|M7 3c0 5 5 6 5 9s-5 4-5 9|M17 3c0 5-5 6-5 9s5 4 5 9"));

    private final TenantModules tenantModules;
    private final DashboardSummaryService summaryService;
    private final DashboardInsights insights;
    private final CompanyBusinessTypes businessTypes;

    private String company = "";
    private int period = 7;
    private Div chartHost;
    private NativeButton btn7;
    private NativeButton btn30;

    public DashboardView(TenantModules tenantModules, DashboardSummaryService summaryService,
                         DashboardInsights insights, CompanyBusinessTypes businessTypes) {
        this.tenantModules = tenantModules;
        this.summaryService = summaryService;
        this.insights = insights;
        this.businessTypes = businessTypes;
    }

    private static String tx(String key, String fallback) {
        String v = t(key);
        return (v == null || v.isBlank() || v.equals(key)) ? fallback : v;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        company = CurrentCompany.get();
        List<AppModule> modules = tenantModules.enabled(company).stream()
                .filter(RolePolicy::canView)
                .toList();

        Set<BusinessType> types = businessTypes.find(company);
        build(types,
                summaryService.loadLowStockItems(company, modules),
                summaryService.loadProfileHighlights(company, modules),
                summaryService.loadRecentActivities(company, modules),
                summaryService.loadModuleSummaries(company, modules),
                insights.load(company, types));
    }

    // ================================================================ sahifa

    private void build(Set<BusinessType> types,
                       List<DashboardSummaryService.LowStockItem> lowStock,
                       DashboardSummaryService.ProfileHighlightGroup profileGroup,
                       List<DashboardSummaryService.RecentActivityItem> recent,
                       List<DashboardSummaryService.ModuleCardSummary> summaries,
                       DashboardInsights.Info info) {
        removeAll();
        addClassName("dx");
        CatalogTerms.Terms terms = CatalogTerms.of(types, AppTexts.lang());

        add(head(), alerts(lowStock, info));
        add(kpis(terms, lowStock, summaries, info));

        chartHost = new Div();
        chartHost.addClassName("dx-chart-host");
        Div chartCard = card("dx-card dx-chart-card", 3);
        chartCard.add(chartHead(), chartHost);
        renderChart();

        Div side = new Div(quickActions(terms, info));
        side.addClassName("dx-side");
        Component steps = steps(info);
        if (steps != null) {
            side.add(steps);
        }
        Div main = new Div(chartCard, side);
        main.addClassName("dx-main");
        add(main);

        Div row2 = new Div(
                lowStockPanel(lowStock, info),
                profilePanel(profileGroup),
                recentPanel(recent));
        row2.addClassNames("dx-grid3");
        add(row2);

        Div row3 = new Div(
                info.tracksExpiry() ? expiringPanel(info) : categoriesPanel(terms, info),
                topPanel(info),
                debtPanel(info));
        row3.addClassNames("dx-grid3");
        add(row3);

        add(modulesRow(summaries));

        getElement().executeJs(FIX_SVG, getElement());
        getElement().executeJs(COUNT_UP, getElement());
    }

    /** Html komponenti viewBox registrini buzadi — brauzerda qayta o'rnatamiz. */
    private static final String FIX_SVG = "$0.querySelectorAll('svg').forEach(s=>{"
            + "s.setAttribute('viewBox', s.dataset.vb || '0 0 24 24');"
            + "if(s.dataset.par) s.setAttribute('preserveAspectRatio', s.dataset.par);});";

    private static final String COUNT_UP = """
            const root = $0;
            if (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) { return; }
            root.querySelectorAll('.dx-count').forEach(function (el) {
              const to = Number(el.dataset.to);
              if (!(to > 0)) { return; }
              const fin = el.textContent;
              const t0 = performance.now();
              (function step(now) {
                const p = Math.min(1, (now - t0) / 800);
                const e = 1 - Math.pow(1 - p, 3);
                el.textContent = Math.round(to * e).toLocaleString('ru-RU');
                if (p < 1) { requestAnimationFrame(step); } else { el.textContent = fin; }
              })(t0);
            });
            """;

    private Component head() {
        H1 title = new H1(greeting() + (company == null || company.isBlank() ? "" : ", " + company));
        title.addClassName("dx-title");
        Paragraph sub = new Paragraph(tx("dash.sub", L.t(
                "Bugungi ko'rsatkichlar va bo'limlar holati.",
                "Показатели и состояние разделов на сегодня.",
                "Today's figures and the state of your modules.")));
        sub.addClassName("dx-sub");
        Div text = new Div(title, sub);

        Span date = new Span(todayLabel());
        date.addClassName("dx-date");
        date.getElement().insertChild(0, new Html(svg("calendar")).getElement());

        Div head = new Div(text, date);
        head.addClassName("dx-head");
        return head;
    }

    private static String greeting() {
        int h = LocalTime.now().getHour();
        if (h >= 5 && h < 12) {
            return L.t("Xayrli tong", "Доброе утро", "Good morning");
        }
        if (h >= 12 && h < 18) {
            return L.t("Xayrli kun", "Добрый день", "Good afternoon");
        }
        if (h >= 18 && h < 23) {
            return L.t("Xayrli kech", "Добрый вечер", "Good evening");
        }
        return L.t("Xayrli tun", "Доброй ночи", "Good night");
    }

    private static String todayLabel() {
        int li = 0;
        try {
            li = Integer.parseInt(L.t("0", "1", "2"));
        } catch (RuntimeException ignored) {
            // standart: o'zbekcha
        }
        LocalDate d = LocalDate.now();
        int wd = d.getDayOfWeek().getValue() - 1;
        String[] months = li == 1 ? RU_MONTH : li == 2 ? EN_MONTH : UZ_MONTH;
        String[] days = li == 1 ? RU_DAY : li == 2 ? EN_DAY : UZ_DAY;
        return d.getDayOfMonth() + " " + months[d.getMonthValue() - 1] + " " + d.getYear() + ", " + days[wd];
    }

    // ================================================================ ogohlantirishlar

    private Component alerts(List<DashboardSummaryService.LowStockItem> lowStock, DashboardInsights.Info info) {
        Div box = new Div();
        box.addClassName("dx-alerts");
        if (!lowStock.isEmpty()) {
            box.add(alertChip("alert", "is-warn", "app/inventory/stock",
                    lowStock.size() + " " + L.t("ta tovar kam qoldi", "поз. заканчивается", "items running low")));
        }
        if (info.expired() > 0) {
            box.add(alertChip("hourglass", "is-danger", "app/inventory/products",
                    info.expired() + " " + L.t("ta tovarning muddati o'tgan", "поз. просрочено", "items expired")));
        }
        if (info.soon() > 0) {
            box.add(alertChip("hourglass", "is-warn", "app/inventory/products",
                    info.soon() + " " + L.t("ta tovar muddati yaqin", "поз. скоро истекает", "items expiring soon")));
        }
        if (info.debtKnown() && info.debtTotal() > 0 && info.invoiceRoute() != null) {
            box.add(alertChip("wallet", "is-info", info.invoiceRoute(),
                    L.t("Qarzdorlik: ", "Задолженность: ", "Outstanding: ") + compact(info.debtTotal())));
        }
        box.setVisible(box.getChildren().findAny().isPresent());
        return box;
    }

    private static Component alertChip(String icon, String tone, String route, String text) {
        Anchor a = new Anchor(route);
        a.addClassNames("dx-alert", tone);
        a.getElement().appendChild(new Html(svg(icon)).getElement());
        a.add(new Span(text));
        return a;
    }

    // ================================================================ KPI

    private Component kpis(CatalogTerms.Terms terms,
                           List<DashboardSummaryService.LowStockItem> lowStock,
                           List<DashboardSummaryService.ModuleCardSummary> summaries,
                           DashboardInsights.Info info) {
        DashboardInsights.Trend tr = insights.trend(company, 7);

        int todayTotal = summaries.stream()
                .mapToInt(DashboardSummaryService.ModuleCardSummary::todayRecords).sum();
        double[] week = new double[7];
        for (DashboardSummaryService.ModuleCardSummary s : summaries) {
            List<Integer> bars = s.last7DaysBars();
            for (int i = 0; i < Math.min(7, bars.size()); i++) {
                week[7 - Math.min(7, bars.size()) + i] += bars.get(i);
            }
        }

        // 1) tushum
        Span revenue = new Span(tr.available() ? compact(tr.total()) : "—");
        revenue.addClassName("dx-kpi-value");
        Div k1 = kpi("trend", L.t("Tushum · 7 kun", "Выручка · 7 дн.", "Revenue · 7 days"), revenue,
                deltaNode(tr.deltaPct(), tr.available() && tr.total() > 0),
                "", sparkline(tr.days().stream().mapToDouble(DashboardInsights.Day::value).toArray()), 0);

        // 2) qarzdorlik
        Span debt = new Span(info.debtKnown() ? compact(info.debtTotal()) : "—");
        debt.addClassName("dx-kpi-value");
        Span debtFoot = new Span(info.debtKnown()
                ? info.debtCount() + " " + L.t("ta hisob ochiq", "открытых счетов", "open invoices")
                : L.t("Ma'lumot yo'q", "Нет данных", "No data"));
        debtFoot.addClassName("dx-kpi-foot");
        Div k2 = kpi("wallet", L.t("Qarzdorlik", "Задолженность", "Outstanding"), debt, debtFoot,
                info.debtTotal() > 0 ? "is-warn" : "", null, 1);

        // 3) katalog
        Span cnt = countSpan(info.productCount());
        Span catFoot = new Span(lowStock.isEmpty()
                ? L.t("Hammasi yetarli", "Всё в наличии", "All stocked")
                : lowStock.size() + " " + L.t("tasi kam qolgan", "на исходе", "running low"));
        catFoot.addClassName("dx-kpi-foot");
        Div k3 = kpi("box", terms.title(), cnt, catFoot, lowStock.isEmpty() ? "" : "is-warn", null, 2);

        // 4) bugungi yozuvlar
        Span today = countSpan(todayTotal);
        Div k4 = kpi("activity", L.t("Bugungi yozuvlar", "Записи сегодня", "Records today"), today, null,
                "", sparkline(week), 3);

        Div strip = new Div(k1, k2, k3, k4);
        strip.addClassName("dx-kpis");
        return strip;
    }

    private static Span countSpan(int n) {
        Span v = new Span(FieldDef.format(n));
        v.addClassNames("dx-kpi-value", "dx-count");
        v.getElement().setAttribute("data-to", String.valueOf(n));
        return v;
    }

    private static Div kpi(String icon, String label, Span value, Component foot, String tone, String spark, int idx) {
        Div ico = iconBox(icon, "dx-ico");
        Span l = new Span(label);
        l.addClassName("dx-kpi-label");
        Div top = new Div(ico, l);
        top.addClassName("dx-kpi-top");

        Div card = new Div(top, value);
        if (foot != null) {
            card.add(foot);
        }
        if (spark != null) {
            Div sp = new Div();
            sp.addClassName("dx-spark");
            sp.getElement().appendChild(new Html(spark).getElement());
            card.add(sp);
        }
        card.addClassName("dx-kpi");
        card.getStyle().set("--i", String.valueOf(idx));
        if (!tone.isBlank()) {
            card.addClassName(tone);
        }
        return card;
    }

    private static Component deltaNode(Double pct, boolean show) {
        Span s = new Span();
        s.addClassName("dx-kpi-foot");
        if (!show) {
            s.setText(L.t("Hali tushum yo'q", "Пока нет выручки", "No revenue yet"));
            return s;
        }
        if (pct == null) {
            s.setText(L.t("Yangi davr", "Новый период", "New period"));
            return s;
        }
        Span d = new Span((pct >= 0 ? "▲ " : "▼ ") + Math.abs(Math.round(pct)) + "%");
        d.addClassNames("dx-delta", pct >= 0 ? "is-up" : "is-down");
        Span tail = new Span(" " + L.t("o'tgan davrga nisbatan", "к прошлому периоду", "vs previous period"));
        s.add(d, tail);
        return s;
    }

    // ================================================================ grafik

    private Component chartHead() {
        Div titles = new Div();
        H2 h = new H2(L.t("Tushum dinamikasi", "Динамика выручки", "Revenue trend"));
        h.addClassName("dx-card-title");
        Span sub = new Span(L.t("Hisob-fakturalar va kassa savdosi", "Счета и продажи по кассе",
                "Invoices and register sales"));
        sub.addClassName("dx-card-sub");
        titles.add(h, sub);

        btn7 = new NativeButton(L.t("7 kun", "7 дней", "7 days"));
        btn30 = new NativeButton(L.t("30 kun", "30 дней", "30 days"));
        for (NativeButton b : List.of(btn7, btn30)) {
            b.addClassName("dx-seg-btn");
        }
        btn7.addClickListener(e -> setPeriod(7));
        btn30.addClickListener(e -> setPeriod(30));
        Div seg = new Div(btn7, btn30);
        seg.addClassName("dx-seg");

        Div head = new Div(titles, seg);
        head.addClassName("dx-card-head");
        return head;
    }

    private void setPeriod(int days) {
        period = days;
        renderChart();
    }

    private void renderChart() {
        btn7.setClassName("is-active", period == 7);
        btn30.setClassName("is-active", period == 30);
        chartHost.removeAll();

        DashboardInsights.Trend tr = insights.trend(company, period);
        List<DashboardInsights.Day> days = tr.days();

        Span total = new Span(tr.available() ? money(tr.total()) : "—");
        total.addClassName("dx-chart-total");
        Div summary = new Div(total);
        summary.addClassName("dx-chart-sum");
        if (tr.available() && tr.total() > 0) {
            summary.add(deltaNode(tr.deltaPct(), true));
        }

        Div chart = new Div();
        chart.addClassName("dx-chart");
        double[] vals = days.stream().mapToDouble(DashboardInsights.Day::value).toArray();
        chart.getElement().appendChild(new Html(areaSvg(vals)).getElement());
        chart.getElement().executeJs(FIX_SVG, chart.getElement());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        Div hits = new Div();
        hits.addClassName("dx-hits");
        for (DashboardInsights.Day d : days) {
            Div hit = new Div();
            hit.addClassName("dx-hit");
            hit.getElement().setAttribute("tabindex", "0");
            hit.getElement().setAttribute("data-tip", d.date().format(fmt) + " · " + money(d.value()));
            hits.add(hit);
        }
        chart.add(hits);
        if (!tr.available() || tr.total() <= 0) {
            Span empty = new Span(L.t("Bu davrda tushum yo'q", "За период выручки нет", "No revenue in this period"));
            empty.addClassName("dx-chart-empty");
            chart.add(empty);
        }

        Div labels = new Div();
        labels.addClassName("dx-xlabels");
        int step = days.size() <= 7 ? 1 : 7;
        for (int i = 0; i < days.size(); i += step) {
            labels.add(new Span(days.get(i).date().format(fmt)));
        }
        if (step > 1 && (days.size() - 1) % step != 0) {
            labels.add(new Span(days.get(days.size() - 1).date().format(fmt)));
        }
        chartHost.add(summary, chart, labels);
    }

    private static String areaSvg(double[] v) {
        int w = 600;
        int h = 200;
        int top = 16;
        int bot = 6;
        int n = v.length;
        if (n < 2) {
            return "<svg data-vb='0 0 600 200' viewBox='0 0 600 200'></svg>";
        }
        double max = 0;
        for (double x : v) {
            max = Math.max(max, x);
        }
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = i * (w / (double) (n - 1));
            ys[i] = max <= 0 ? h - bot : h - bot - (v[i] / max) * (h - top - bot);
        }
        StringBuilder line = new StringBuilder("M").append(f(xs[0])).append(' ').append(f(ys[0]));
        for (int i = 1; i < n; i++) {
            double mx = (xs[i - 1] + xs[i]) / 2;
            line.append(" C").append(f(mx)).append(' ').append(f(ys[i - 1]))
                    .append(' ').append(f(mx)).append(' ').append(f(ys[i]))
                    .append(' ').append(f(xs[i])).append(' ').append(f(ys[i]));
        }
        String area = line + " L" + f(xs[n - 1]) + " " + h + " L0 " + h + " Z";
        StringBuilder grid = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            double y = top + i * ((h - top - bot) / 3.0);
            grid.append("<line class='dx-gridline' x1='0' x2='").append(w).append("' y1='").append(f(y))
                    .append("' y2='").append(f(y)).append("' vector-effect='non-scaling-stroke'/>");
        }
        return "<svg class='dx-chart-svg' data-vb='0 0 " + w + " " + h + "' data-par='none' viewBox='0 0 " + w + " " + h + "' preserveAspectRatio='none' aria-hidden='true'>"
                + grid
                + "<path class='dx-area' d='" + area + "'/>"
                + "<path class='dx-line' d='" + line + "' fill='none' vector-effect='non-scaling-stroke'/></svg>";
    }

    private static String sparkline(double[] v) {
        int n = v.length;
        if (n < 2) {
            return null;
        }
        double max = 0;
        for (double x : v) {
            max = Math.max(max, x);
        }
        StringBuilder p = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double x = i * (100.0 / (n - 1));
            double y = max <= 0 ? 26 : 26 - (v[i] / max) * 22;
            p.append(i == 0 ? "M" : " L").append(f(x)).append(' ').append(f(y));
        }
        return "<svg data-vb='0 0 100 28' data-par='none' viewBox='0 0 100 28' preserveAspectRatio='none' aria-hidden='true'>"
                + "<path d='" + p + "' fill='none' vector-effect='non-scaling-stroke'/></svg>";
    }

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    // ================================================================ yon panel

    private Component quickActions(CatalogTerms.Terms terms, DashboardInsights.Info info) {
        Div grid = new Div();
        grid.addClassName("dx-actions");
        grid.add(action("plus", terms.add(), "app/inventory/products"));
        if (info.receiptRoute() != null) {
            grid.add(action("in", L.t("Kirim", "Приход", "Receipt"), info.receiptRoute()));
        }
        if (info.outRoute() != null) {
            grid.add(action("out", L.t("Chiqim", "Расход", "Issue"), info.outRoute()));
        }
        if (info.invoiceRoute() != null) {
            grid.add(action("receipt", L.t("Hisob-faktura", "Счёт", "Invoice"), info.invoiceRoute()));
        }
        Div c = card("dx-card", 4);
        H2 h = new H2(L.t("Tezkor amallar", "Быстрые действия", "Quick actions"));
        h.addClassName("dx-card-title");
        c.add(h, grid);
        return c;
    }

    private static Component action(String icon, String label, String route) {
        Anchor a = new Anchor(route);
        a.addClassName("dx-action");
        a.add(iconBox(icon, "dx-ico"), new Span(label));
        return a;
    }

    /** "Boshlash" ro'yxati; hammasi bajarilgan bo'lsa ko'rsatilmaydi. */
    private Component steps(DashboardInsights.Info info) {
        List<DashboardInsights.Step> steps = info.steps();
        long done = steps.stream().filter(DashboardInsights.Step::done).count();
        if (steps.isEmpty() || done == steps.size()) {
            return null;
        }
        Div c = card("dx-card", 5);
        H2 h = new H2(L.t("Boshlash", "Начало работы", "Getting started"));
        h.addClassName("dx-card-title");
        Span count = new Span(done + " / " + steps.size());
        count.addClassName("dx-pill");
        Div head = new Div(h, count);
        head.addClassName("dx-card-head");

        Div bar = new Div();
        bar.addClassName("dx-progress");
        Div fill = new Div();
        fill.getStyle().set("width", (int) Math.round(done * 100.0 / steps.size()) + "%");
        bar.add(fill);

        Div list = new Div();
        list.addClassName("dx-steps");
        for (DashboardInsights.Step s : steps) {
            Anchor a = new Anchor(s.route());
            a.addClassName("dx-step");
            a.setClassName("is-done", s.done());
            Div mark = iconBox(s.done() ? "check" : "plus", "dx-step-mark");
            a.add(mark, new Span(s.label()));
            list.add(a);
        }
        c.add(head, bar, list);
        return c;
    }

    // ================================================================ panellar

    private Component lowStockPanel(List<DashboardSummaryService.LowStockItem> items, DashboardInsights.Info info) {
        Div p = panel(L.t("Kam qolgan tovarlar", "Заканчивается", "Running low"),
                pill(items.size() + " " + tx("dash.lowstock.unit", L.t("ta", "шт", "pcs")),
                        items.isEmpty() ? "is-ok" : "is-warn"), 6);
        if (items.isEmpty()) {
            p.add(note(tx("dash.lowstock.empty", L.t("Hozircha kam qolgan tovar yo'q.",
                    "Нет товаров на исходе.", "Nothing is running low."))));
            return p;
        }
        double max = items.stream().mapToDouble(DashboardSummaryService.LowStockItem::quantity).max().orElse(1);
        Div list = new Div();
        list.addClassName("dx-list");
        for (DashboardSummaryService.LowStockItem it : items) {
            Anchor row = new Anchor(it.targetRoute());
            row.addClassName("dx-row");
            Div meter = new Div();
            meter.addClassName("dx-meter");
            Div fill = new Div();
            double pct = it.quantity() <= 0 ? 4 : Math.max(10, Math.min(100, it.quantity() / (max <= 0 ? 1 : max) * 100));
            fill.getStyle().set("width", Math.round(pct) + "%");
            fill.addClassName(it.quantity() <= 0 ? "is-bad" : "is-warn");
            meter.add(fill);

            Div main = new Div(rowTitle(it.title()), meter);
            main.addClassName("dx-row-main");
            Span qty = new Span(FieldDef.format(it.quantity()) + " " + it.unit());
            qty.addClassNames("dx-pill", it.quantity() <= 0 ? "is-danger" : "is-warn");
            row.add(thumb(it.imageUrl(), it.title(), it.iconKey()), main, qty);
            list.add(row);
        }
        p.add(list);
        return p;
    }

    private Component profilePanel(DashboardSummaryService.ProfileHighlightGroup g) {
        String tone = "ok".equals(g.badgeTone()) ? "is-ok" : "info".equals(g.badgeTone()) ? "is-info" : "";
        Div p = panel(g.sectionTitle(), pill(g.countBadgeText(), tone), 7);
        if (g.items().isEmpty()) {
            p.add(note(g.emptyMessage()));
            return p;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (DashboardSummaryService.ProfileHighlightItem it : g.items()) {
            Anchor row = new Anchor(it.targetRoute());
            row.addClassName("dx-row");
            Span sub = new Span(it.detail());
            sub.addClassName("dx-row-sub");
            Div main = new Div(rowTitle(it.title()), sub);
            main.addClassName("dx-row-main");
            Span badge = new Span(it.badgeText());
            badge.addClassName("dx-pill");
            if ("bad".equals(it.badgeTone())) {
                badge.addClassName("is-danger");
            } else if ("info".equals(it.badgeTone())) {
                badge.addClassName("is-info");
            }
            row.add(thumb(it.imageUrl(), it.title(), it.iconKey()), main, badge);
            list.add(row);
        }
        p.add(list);
        return p;
    }

    private Component recentPanel(List<DashboardSummaryService.RecentActivityItem> items) {
        Span live = new Span(tx("dash.recent.tag", L.t("Jonli", "Онлайн", "Live")));
        live.addClassNames("dx-pill", "is-live");
        Div p = panel(tx("dash.recent.title", L.t("Oxirgi harakatlar", "Последние действия", "Recent activity")),
                live, 8);
        if (items.isEmpty()) {
            p.add(note(tx("dash.recent.empty", L.t("Hali yozuvlar kiritilmagan.",
                    "Записей пока нет.", "No records yet."))));
            return p;
        }
        Div list = new Div();
        list.addClassName("dx-timeline");
        for (DashboardSummaryService.RecentActivityItem it : items) {
            String probe = (it.subtitle() == null ? "" : it.subtitle()).toLowerCase(Locale.ROOT);
            String kind = probe.contains("kirim") || probe.contains("приход") || probe.contains("receipt") ? "in"
                    : probe.contains("chiqim") || probe.contains("расход") || probe.contains("issue") ? "out"
                    : probe.contains("inventar") || probe.contains("инвент") ? "clipboard" : "activity";

            Anchor row = new Anchor(it.targetRoute());
            row.addClassName("dx-tl-item");
            Div dot = iconBox(kind, "dx-tl-dot");
            dot.addClassName("in".equals(kind) ? "is-in" : "out".equals(kind) ? "is-out" : "is-neutral");
            Span sub = new Span(it.subtitle());
            sub.addClassName("dx-row-sub");
            Div main = new Div(rowTitle(it.title()), sub);
            main.addClassName("dx-row-main");
            row.add(dot, main);
            if (it.statusOrAmount() != null && !it.statusOrAmount().isBlank()) {
                Span tag = new Span(it.statusOrAmount());
                tag.addClassName("dx-pill");
                row.add(tag);
            }
            list.add(row);
        }
        p.add(list);
        return p;
    }

    private Component expiringPanel(DashboardInsights.Info info) {
        int n = info.expired() + info.soon();
        Div p = panel(L.t("Muddati yaqinlar", "Скоро истекает срок", "Expiring soon"),
                pill(n + " " + L.t("ta", "шт", "pcs"), n == 0 ? "is-ok" : info.expired() > 0 ? "is-danger" : "is-warn"), 9);
        if (info.expiring().isEmpty()) {
            p.add(note(L.t("30 kun ichida muddati tugaydigan tovar yo'q.",
                    "Нет товаров со сроком в ближайшие 30 дней.", "Nothing expires in the next 30 days.")));
            return p;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (DashboardInsights.Expiring e : info.expiring()) {
            Anchor row = new Anchor("app/inventory/products");
            row.addClassName("dx-row");
            Span when = new Span(e.daysLeft() < 0
                    ? L.t("Muddati o'tgan", "Просрочено", "Expired")
                    : e.daysLeft() + " " + L.t("kun qoldi", "дн.", "days left"));
            when.addClassNames("dx-pill", e.daysLeft() < 0 ? "is-danger" : e.daysLeft() <= 7 ? "is-warn" : "is-info");
            Div main = new Div(rowTitle(e.name()));
            main.addClassName("dx-row-main");
            row.add(iconBox("hourglass", "dx-ico"), main, when);
            list.add(row);
        }
        p.add(list);
        return p;
    }

    private Component categoriesPanel(CatalogTerms.Terms terms, DashboardInsights.Info info) {
        Div p = panel(L.t("Tovarlar toifalar bo'yicha", "Товары по категориям", "Products by category"),
                pill(info.productCount() + " " + L.t("ta", "шт", "pcs"), ""), 9);
        if (info.categories().isEmpty()) {
            p.add(note(L.t("Hali tovar kiritilmagan.", "Товары ещё не добавлены.", "No items added yet.")));
            return p;
        }
        long max = info.categories().stream().mapToLong(DashboardInsights.Bucket::count).max().orElse(1);
        Div bars = new Div();
        bars.addClassName("dx-bars");
        for (DashboardInsights.Bucket b : info.categories()) {
            Span name = new Span(b.label());
            Span num = new Span(b.count() + " " + L.t("ta", "шт", "pcs"));
            Div top = new Div(name, num);
            top.addClassName("dx-bar-top");
            Div fill = new Div();
            fill.getStyle().set("width", Math.max(6, Math.round(b.count() * 100.0 / max)) + "%");
            Div track = new Div(fill);
            track.addClassName("dx-bar-track");
            Div row = new Div(top, track);
            row.addClassName("dx-bar-row");
            bars.add(row);
        }
        p.add(bars);
        if (!info.sizes().isEmpty()) {
            Span cap = new Span(L.t("O'lchamlar (nechta tovar)", "Размеры (сколько товаров)", "Sizes (how many items)"));
            cap.addClassName("dx-cap");
            Div chips = new Div();
            chips.addClassName("dx-chips");
            for (DashboardInsights.Bucket b : info.sizes()) {
                Span chip = new Span(b.label() + " · " + b.count());
                chip.addClassName("dx-chip");
                chips.add(chip);
            }
            p.add(cap, chips);
        }
        return p;
    }

    private Component topPanel(DashboardInsights.Info info) {
        Div p = panel(L.t("Eng ko'p sotilganlar", "Самые продаваемые", "Best sellers"),
                pill(L.t("Oxirgi 30 kun", "Последние 30 дн.", "Last 30 days"), ""), 10);
        if (info.top().isEmpty()) {
            p.add(note(L.t("Oxirgi 30 kunda sotuv bo'lmagan. Kassada savdo qilsangiz, eng ko'p sotilgan tovarlar shu yerda chiqadi.",
                    "За 30 дней продаж нет. После продаж на кассе здесь появятся самые продаваемые товары.",
                    "No sales in the last 30 days. Best sellers will appear here after register sales.")));
            return p;
        }
        double max = info.top().get(0).qty();
        Div list = new Div();
        list.addClassName("dx-list");
        int rank = 1;
        for (DashboardInsights.Ranked r : info.top()) {
            Span no = new Span(String.valueOf(rank++));
            no.addClassName("dx-rank");
            Div fill = new Div();
            fill.getStyle().set("width", Math.max(8, Math.round(r.qty() / (max <= 0 ? 1 : max) * 100)) + "%");
            Div meter = new Div(fill);
            meter.addClassName("dx-meter");
            Div main = new Div(rowTitle(r.name()), meter);
            main.addClassName("dx-row-main");
            Span qty = new Span(FieldDef.format(r.qty()) + " " + L.t("ta", "шт", "pcs"));
            qty.addClassName("dx-pill");
            Div row = new Div(no, main, qty);
            row.addClassName("dx-row");
            list.add(row);
        }
        p.add(list);
        return p;
    }

    private Component debtPanel(DashboardInsights.Info info) {
        Div p = panel(L.t("Qarzdorlik", "Задолженность", "Outstanding"),
                pill(info.debtKnown() ? compact(info.debtTotal()) : "—", info.debtTotal() > 0 ? "is-warn" : "is-ok"), 11);
        if (!info.debtKnown()) {
            p.add(note(L.t("Hisob-fakturalarda 'status' maydoni topilmadi.",
                    "В счетах не найдено поле «status».", "No 'status' field found on invoices.")));
            return p;
        }
        if (info.debts().isEmpty()) {
            p.add(note(L.t("Ochiq qarzlar yo'q.", "Открытых долгов нет.", "No open debts.")));
            return p;
        }
        Div list = new Div();
        list.addClassName("dx-list");
        for (DashboardInsights.Debt d : info.debts()) {
            Anchor row = new Anchor(info.invoiceRoute() == null ? "app" : info.invoiceRoute());
            row.addClassName("dx-row");
            Span sub = new Span(d.sub());
            sub.addClassName("dx-row-sub");
            Div main = new Div(rowTitle(d.title()), sub);
            main.addClassName("dx-row-main");
            Span amt = new Span(compact(d.amount()));
            amt.addClassNames("dx-pill", "is-warn");
            row.add(iconBox("wallet", "dx-ico"), main, amt);
            list.add(row);
        }
        p.add(list);
        return p;
    }

    // ================================================================ modullar

    private Component modulesRow(List<DashboardSummaryService.ModuleCardSummary> summaries) {
        Div wrap = new Div();
        wrap.addClassName("dx-mods-wrap");
        H2 h = new H2(tx("dash.modules", L.t("Bo'limlar", "Разделы", "Modules")));
        h.addClassName("dx-section-title");
        Div grid = new Div();
        grid.addClassName("dx-mods");
        for (DashboardSummaryService.ModuleCardSummary s : summaries) {
            AppModule m = s.module();
            Anchor a = new Anchor(m.route());
            a.addClassName("dx-mod");
            Span title = new Span(m.title());
            title.addClassName("dx-mod-title");
            Span sub = new Span(s.todayRecords() == 0
                    ? L.t("Bugun bo'sh", "Сегодня пусто", "Empty today")
                    : L.t("Bugun: ", "Сегодня: ", "Today: ") + s.todayRecords());
            sub.addClassName("dx-mod-sub");
            Div txt = new Div(title, sub);
            txt.addClassName("dx-mod-text");
            Span arrow = new Span();
            arrow.addClassName("dx-mod-arrow");
            arrow.getElement().appendChild(new Html(svg("arrow")).getElement());
            a.add(iconBox(moduleIcon(m), "dx-ico"), txt, arrow);
            grid.add(a);
        }
        wrap.add(h, grid);
        wrap.setVisible(!summaries.isEmpty());
        return wrap;
    }

    private static String moduleIcon(AppModule m) {
        String probe = (m.key() + " " + m.title() + " " + m.route()).toLowerCase(Locale.ROOT);
        if (probe.contains("moliya") || probe.contains("finance") || probe.contains("kassa")) {
            return "wallet";
        }
        if (probe.contains("crm") || probe.contains("mijoz") || probe.contains("client")) {
            return "users";
        }
        if (probe.contains("ombor") || probe.contains("inventory") || probe.contains("stock")) {
            return "box";
        }
        if (probe.contains("kadr") || probe.contains("staff") || probe.contains("hr")) {
            return "briefcase";
        }
        if (probe.contains("bi") || probe.contains("hisobot") || probe.contains("report") || probe.contains("analit")) {
            return "chart";
        }
        return "grid";
    }

    // ================================================================ mayda yordamchilar

    private static Div card(String cls, int idx) {
        Div d = new Div();
        for (String c : cls.split(" ")) {
            d.addClassName(c);
        }
        d.getStyle().set("--i", String.valueOf(idx));
        return d;
    }

    private static Div panel(String title, Component badge, int idx) {
        Div p = card("dx-card dx-panel", idx);
        H2 h = new H2(title);
        h.addClassName("dx-card-title");
        Div head = new Div(h, badge);
        head.addClassName("dx-card-head");
        p.add(head);
        return p;
    }

    private static Span pill(String text, String tone) {
        Span s = new Span(text);
        s.addClassName("dx-pill");
        if (!tone.isBlank()) {
            s.addClassName(tone);
        }
        return s;
    }

    private static Span rowTitle(String text) {
        Span s = new Span(text);
        s.addClassName("dx-row-title");
        return s;
    }

    private static Paragraph note(String text) {
        Paragraph p = new Paragraph(text);
        p.addClassName("dx-note");
        return p;
    }

    private static Div thumb(String imageUrl, String alt, String iconKey) {
        Div d = new Div();
        d.addClassName("dx-thumb");
        if (imageUrl != null && !imageUrl.isBlank()) {
            Image img = new Image(imageUrl, alt);
            d.add(img);
        } else {
            d.getElement().appendChild(new Html(svg(ICONS.containsKey(iconKey) ? iconKey : "box")).getElement());
        }
        return d;
    }

    private static Div iconBox(String name, String cls) {
        Div d = new Div();
        d.addClassName(cls);
        d.getElement().setAttribute("aria-hidden", "true");
        d.getElement().appendChild(new Html(svg(name)).getElement());
        return d;
    }

    private static String svg(String name) {
        String def = ICONS.getOrDefault(name, ICONS.get("box"));
        StringBuilder sb = new StringBuilder("<svg class='dx-svg' data-vb='0 0 24 24' viewBox='0 0 24 24' fill='none' stroke='currentColor' "
                + "stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true' focusable='false'>");
        for (String path : def.split("\\|")) {
            sb.append("<path d='").append(path).append("'/>");
        }
        return sb.append("</svg>").toString();
    }

    private static String money(double v) {
        return FieldDef.format(v) + " " + L.t("so'm", "сум", "UZS");
    }

    /** 12 400 000 → "12,4 mln". */
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
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s.replace('.', ',');
    }

    @Override
    public String getPageTitle() {
        return "DOKONIX — " + tx("dash.title", L.t("Bosh sahifa", "Главная", "Home"));
    }
}