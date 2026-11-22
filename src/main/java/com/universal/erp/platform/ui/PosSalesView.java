package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * POS savdolari: bugun / tanlangan sana / oy bo'yicha / hammasi.
 * Oy rejimida kunlar bo'yicha diagramma (kunni bossangiz o'sha kun ochiladi) va kunlar bo'yicha guruhlangan ro'yxat.
 * Stil: frontend/styles/pos-sales.css (prefiks "sl-").
 */
@PageTitle("POS savdolari")
@Route(value = "app/inventory/pos-sales", layout = MainLayout.class)
@CssImport("./styles/pos-sales.css")
@PermitAll
public class PosSalesView extends Div implements BeforeEnterObserver {

    private enum Mode { TODAY, DAY, MONTH, ALL }

    private static final String[][] MONTHS = {
            {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun", "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"},
            {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"},
            {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"}
    };
    private static final String[][] WEEKDAYS = {
            {"Du", "Se", "Chor", "Pay", "Ju", "Sha", "Yak"},
            {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
    };
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RecordStore store;
    private final String company = CurrentCompany.get();

    private final Div body = new Div();
    private final Map<Mode, NativeButton> tabs = new EnumMap<>(Mode.class);
    private final Div dayCtl = new Div();
    private final Div monthCtl = new Div();
    private final Span monthLabel = new Span();
    private final DatePicker datePicker = new DatePicker();

    private Mode mode = Mode.TODAY;
    private LocalDate day = LocalDate.now();
    private YearMonth month = YearMonth.now();

    public PosSalesView(RecordStore store) {
        this.store = store;
        addClassName("sl-root");

        H2 title = new H2(label("POS savdolari", "Продажи POS", "POS sales"));
        title.addClassName("sl-title");
        Paragraph sub = new Paragraph(label("Kassada yopilgan savdolar: kun, oy yoki butun davr bo'yicha",
                "Продажи, закрытые на кассе: по дню, месяцу или за всё время",
                "Sales closed at the register: by day, month or all time"));
        sub.addClassName("sl-sub");
        Div titleBox = new Div(title, sub);
        titleBox.addClassName("sl-title-box");

        Div tabBar = new Div();
        tabBar.addClassName("sl-tabs");
        addTab(tabBar, Mode.TODAY, label("Bugun", "Сегодня", "Today"));
        addTab(tabBar, Mode.DAY, label("Sana", "Дата", "Date"));
        addTab(tabBar, Mode.MONTH, label("Oy", "Месяц", "Month"));
        addTab(tabBar, Mode.ALL, label("Hammasi", "Все", "All"));

        buildDayControl();
        buildMonthControl();

        Div head = new Div(titleBox, tabBar);
        head.addClassName("sl-head");

        Div filters = new Div(dayCtl, monthCtl);
        filters.addClassName("sl-filters");

        body.addClassName("sl-body");
        add(head, filters, body);
        render();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (company.isEmpty() || !RolePolicy.canView(AppModule.INVENTORY)) {
            event.forwardTo("app");
        }
    }

    // ================================================================ boshqaruv elementlari

    private void addTab(Div bar, Mode m, String text) {
        NativeButton b = new NativeButton(text);
        b.addClassName("sl-tab");
        b.addClickListener(e -> {
            mode = m;
            render();
        });
        tabs.put(m, b);
        bar.add(b);
    }

    private void buildDayControl() {
        datePicker.setValue(day);
        datePicker.setClearButtonVisible(false);
        datePicker.addClassName("sl-date");
        datePicker.addValueChangeListener(e -> {
            if (e.getValue() == null || e.getValue().equals(day)) {
                return;
            }
            day = e.getValue();
            render();
        });

        NativeButton prev = navBtn("‹", label("Oldingi kun", "Предыдущий день", "Previous day"));
        prev.addClickListener(e -> goDay(day.minusDays(1)));
        NativeButton next = navBtn("›", label("Keyingi kun", "Следующий день", "Next day"));
        next.addClickListener(e -> goDay(day.plusDays(1)));

        dayCtl.add(prev, datePicker, next);
        dayCtl.addClassName("sl-ctl");
    }

    private void buildMonthControl() {
        monthLabel.addClassName("sl-month-label");

        NativeButton prev = navBtn("‹", label("Oldingi oy", "Предыдущий месяц", "Previous month"));
        prev.addClickListener(e -> {
            month = month.minusMonths(1);
            render();
        });
        NativeButton next = navBtn("›", label("Keyingi oy", "Следующий месяц", "Next month"));
        next.addClickListener(e -> {
            month = month.plusMonths(1);
            render();
        });
        NativeButton thisMonth = new NativeButton(label("Shu oy", "Этот месяц", "This month"));
        thisMonth.addClassName("sl-chip-btn");
        thisMonth.addClickListener(e -> {
            month = YearMonth.now();
            render();
        });

        monthCtl.add(prev, monthLabel, next, thisMonth);
        monthCtl.addClassName("sl-ctl");
    }

    private static NativeButton navBtn(String text, String aria) {
        NativeButton b = new NativeButton(text);
        b.addClassName("sl-nav");
        b.getElement().setAttribute("aria-label", aria);
        b.getElement().setAttribute("title", aria);
        return b;
    }

    private void goDay(LocalDate d) {
        day = d;
        datePicker.setValue(d);
        render();
    }

    // ================================================================ chizish

    private void render() {
        tabs.forEach((m, b) -> b.setClassName("is-active", m == mode));
        dayCtl.setVisible(mode == Mode.DAY);
        monthCtl.setVisible(mode == Mode.MONTH);
        monthLabel.setText(MONTHS[AppTexts.index()][month.getMonthValue() - 1] + " " + month.getYear());
        body.removeAll();

        AppSection s = AppSection.get(AppModule.INVENTORY, "pos-sales");
        List<RecordStore.Rec> rows = store.list(company, s.key()).stream()
                .filter(this::matches)
                .sorted(newestFirst())
                .toList();

        double total = 0;
        double cash = 0;
        double card = 0;
        for (RecordStore.Rec r : rows) {
            double a = FieldDef.parse(val(r, "amount"));
            total += a;
            if ("card".equals(val(r, "method"))) {
                card += a;
            } else {
                cash += a;
            }
        }
        double avg = rows.isEmpty() ? 0 : total / rows.size();

        Div stats = new Div(
                stat(label("Savdolar soni", "Кол-во продаж", "Sales"), String.valueOf(rows.size()), false),
                stat(label("Jami summa", "Общая сумма", "Total"), FieldDef.format(total) + " UZS", true),
                stat(PosTexts.t("cash"), FieldDef.format(cash), false),
                stat(PosTexts.t("card"), FieldDef.format(card), false),
                stat(label("O'rtacha chek", "Средний чек", "Average sale"), FieldDef.format(avg), false));
        stats.addClassName("sl-stats");
        body.add(stats);

        if (mode == Mode.MONTH && !rows.isEmpty()) {
            body.add(monthChart(rows));
        }

        if (rows.isEmpty()) {
            Div empty = new Div();
            empty.setText(label("Bu davrda savdolar yo'q", "За этот период продаж нет", "No sales in this period"));
            empty.addClassName("sl-empty");
            body.add(empty);
            return;
        }

        if (mode == Mode.MONTH || mode == Mode.ALL) {
            body.add(groupedList(s, rows));
        } else {
            Div list = new Div();
            list.addClassName("sl-list");
            for (RecordStore.Rec r : rows) {
                list.add(row(r));
            }
            body.add(list);
        }
    }

    private boolean matches(RecordStore.Rec r) {
        String d = val(r, "date");
        return switch (mode) {
            case TODAY -> LocalDate.now().toString().equals(d);
            case DAY -> day.toString().equals(d);
            case MONTH -> d.startsWith(month.toString());
            case ALL -> true;
        };
    }

    private static Comparator<RecordStore.Rec> newestFirst() {
        Comparator<RecordStore.Rec> byDate = Comparator.comparing(r -> val(r, "date"));
        Comparator<RecordStore.Rec> byTime = Comparator.comparing(r -> val(r, "time"));
        return byDate.thenComparing(byTime).reversed();
    }

    /** Oy bo'yicha kunlik savdo ustunlari. Ustunni bossangiz o'sha kun ochiladi. */
    private Component monthChart(List<RecordStore.Rec> rows) {
        int len = month.lengthOfMonth();
        double[] sums = new double[len + 1];
        for (RecordStore.Rec r : rows) {
            String d = val(r, "date");
            try {
                sums[LocalDate.parse(d).getDayOfMonth()] += FieldDef.parse(val(r, "amount"));
            } catch (RuntimeException ignored) {
                // noto'g'ri sana — diagrammaga kirmaydi
            }
        }
        double max = 0;
        for (double v : sums) {
            max = Math.max(max, v);
        }

        Div bars = new Div();
        bars.addClassName("sl-chart-bars");
        for (int d = 1; d <= len; d++) {
            LocalDate date = month.atDay(d);
            double v = sums[d];

            Div bar = new Div();
            bar.addClassName("sl-bar");
            bar.getStyle().set("height", (max > 0 && v > 0 ? Math.max(6, v / max * 100) : 3) + "%");
            Span num = new Span(String.valueOf(d));
            num.addClassName("sl-bar-day");

            Div col = new Div(bar, num);
            col.addClassName("sl-bar-col");
            col.getElement().setAttribute("title", date.format(DMY) + ": " + FieldDef.format(v) + " UZS");
            if (v > 0) {
                col.addClassName("has-sales");
                col.addClickListener(e -> {
                    mode = Mode.DAY;
                    goDay(date);
                });
            }
            bars.add(col);
        }

        Span cap = new Span(label("Kunlar bo'yicha savdo", "Продажи по дням", "Sales by day"));
        cap.addClassName("sl-chart-title");
        Span hint = new Span(label("Kunni bossangiz batafsil ochiladi", "Нажмите на день для деталей",
                "Click a day to see details"));
        hint.addClassName("sl-chart-hint");
        Div capBox = new Div(cap, hint);
        capBox.addClassName("sl-chart-cap");

        Div chart = new Div(capBox, bars);
        chart.addClassName("sl-chart");
        return chart;
    }

    /** Kunlar bo'yicha guruhlangan ro'yxat (oy va "hammasi" rejimlarida). */
    private Component groupedList(AppSection s, List<RecordStore.Rec> rows) {
        Map<String, List<RecordStore.Rec>> byDate = new LinkedHashMap<>();
        for (RecordStore.Rec r : rows) {
            byDate.computeIfAbsent(val(r, "date"), k -> new ArrayList<>()).add(r);
        }

        Div wrap = new Div();
        wrap.addClassName("sl-groups");
        byDate.forEach((date, list) -> {
            double sum = 0;
            for (RecordStore.Rec r : list) {
                sum += FieldDef.parse(val(r, "amount"));
            }

            Span dl = new Span(dateLabel(date));
            dl.addClassName("sl-group-date");
            Span cnt = new Span(list.size() + " " + label("ta savdo", "продаж", "sales"));
            cnt.addClassName("sl-group-count");
            Span tot = new Span(FieldDef.format(sum) + " UZS");
            tot.addClassName("sl-group-total");
            Div ghead = new Div(dl, cnt, tot);
            ghead.addClassName("sl-group-head");

            Div items = new Div();
            items.addClassName("sl-list");
            for (RecordStore.Rec r : list) {
                items.add(row(r));
            }

            Div group = new Div(ghead, items);
            group.addClassName("sl-group");
            wrap.add(group);
        });
        return wrap;
    }

    private Component stat(String label, String value, boolean accent) {
        Span l = new Span(label);
        l.addClassName("sl-stat-label");
        Span v = new Span(value);
        v.addClassName("sl-stat-value");
        Div d = new Div(l, v);
        d.addClassName("sl-stat");
        if (accent) {
            d.addClassName("is-accent");
        }
        return d;
    }

    private Component row(RecordStore.Rec r) {
        Span no = new Span(val(r, "number"));
        no.addClassName("sl-no");
        String time = val(r, "time");
        Span when = new Span(time.isBlank() ? "—" : time);
        when.addClassName("sl-when");
        Div left = new Div(no, when);
        left.addClassName("sl-left");

        boolean card = "card".equals(val(r, "method"));
        Span badge = new Span(PosTexts.t(card ? "card" : "cash"));
        badge.addClassNames("sl-badge", card ? "is-card" : "is-cash");

        String cashierName = val(r, "cashier");
        Span cashier = new Span(cashierName.isBlank() ? "—" : cashierName);
        cashier.addClassName("sl-cashier");

        Span amount = new Span(FieldDef.format(FieldDef.parse(val(r, "amount"))) + " UZS");
        amount.addClassName("sl-amount");

        Div top = new Div(left, badge, cashier, amount);
        top.addClassName("sl-row-top");

        Div row = new Div(top);
        row.addClassName("sl-row");

        Div items = new Div();
        items.addClassName("sl-items");
        for (String part : val(r, "items").split(";")) {
            if (!part.isBlank()) {
                Span chip = new Span(part.trim());
                chip.addClassName("sl-chip");
                items.add(chip);
            }
        }
        if (items.getChildren().findAny().isPresent()) {
            row.add(items);
        }
        return row;
    }

    // ================================================================ yordamchi

    private static String val(RecordStore.Rec r, String key) {
        String v = r.get(key);
        return v == null ? "" : v;
    }

    private static String dateLabel(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso);
            return WEEKDAYS[AppTexts.index()][d.getDayOfWeek().getValue() - 1] + ", " + d.format(DMY);
        } catch (RuntimeException e) {
            return iso.isEmpty() ? "—" : iso;
        }
    }

    private static String label(String uz, String ru, String en) {
        return new String[] {uz, ru, en}[AppTexts.index()];
    }
}