package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiInsights.Range;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import java.time.LocalDate;

/**
 * Davr tanlash paneli (bu oy / o'tgan oy / chorak / yil / sana oralig'i).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiPeriodBar.java
 */
public class BiPeriodBar extends Div {

    private String preset = "month";
    private LocalDate cFrom = LocalDate.now().withDayOfMonth(1);
    private LocalDate cTo = LocalDate.now();
    private final Div seg = new Div();
    private final Div custom = new Div();
    private final Runnable onChange;

    public BiPeriodBar(Runnable onChange) {
        this.onChange = onChange;
        addClassName("bi-controls");
        String[][] presets = {
                {"month", L.t("Bu oy", "Этот месяц", "This month")},
                {"lastMonth", L.t("O'tgan oy", "Прошлый месяц", "Last month")},
                {"quarter", L.t("Chorak", "Квартал", "Quarter")},
                {"year", L.t("Yil", "Год", "Year")},
                {"custom", L.t("Sana oralig'i", "Период", "Custom")}};
        seg.addClassName("dx-seg");
        for (String[] p : presets) {
            NativeButton b = new NativeButton(p[1]);
            b.addClassName("dx-seg-btn");
            b.getElement().setAttribute("data-key", p[0]);
            b.addClickListener(e -> {
                preset = p[0];
                sync();
                onChange.run();
            });
            seg.add(b);
        }
        DatePicker from = new DatePicker(L.t("Dan", "С", "From"));
        DatePicker to = new DatePicker(L.t("Gacha", "По", "To"));
        from.setValue(cFrom);
        to.setValue(cTo);
        from.addValueChangeListener(e -> { if (e.getValue() != null) { cFrom = e.getValue(); onChange.run(); } });
        to.addValueChangeListener(e -> { if (e.getValue() != null) { cTo = e.getValue(); onChange.run(); } });
        custom.addClassName("bi-custom");
        custom.add(from, to);
        add(seg, custom);
        sync();
    }

    private void sync() {
        seg.getChildren().forEach(c -> c.getElement().getClassList()
                .set("is-active", preset.equals(c.getElement().getAttribute("data-key"))));
        custom.setVisible("custom".equals(preset));
    }

    public Range range() {
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
                return cTo.isBefore(cFrom) ? new Range(t.withDayOfMonth(1), t) : new Range(cFrom, cTo);
            default:
                return new Range(t.withDayOfMonth(1), t);
        }
    }
}