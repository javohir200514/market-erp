package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiInsights.Slice;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BI sahifalari uchun umumiy UI yordamchilari (savdo tahlili va keyingi bo'limlar).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiUi.java
 */
final class BiUi {

    private BiUi() {}

    static final String FIX_SVG = "$0.querySelectorAll('svg').forEach(s=>{"
            + "s.setAttribute('viewBox', s.dataset.vb || '0 0 24 24');"
            + "if(s.dataset.par) s.setAttribute('preserveAspectRatio', s.dataset.par);});";

    private static final Map<String, String> ICONS = Map.of(
            "trend", "M3 17l6-6 4 4 8-8|M15 7h6v6",
            "wallet", "M3 5a2 2 0 0 1 2-2h13v4|M3 5v14a2 2 0 0 0 2 2h16V7H5a2 2 0 0 1-2-2|M16 13.5h.01",
            "users", "M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2|M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8|M21 21v-2a4 4 0 0 0-3-3.9|M16 3.1a4 4 0 0 1 0 7.8",
            "box", "M21 8l-9-5-9 5v8l9 5 9-5z|M3.3 7.5L12 12l8.7-4.5|M12 22V12",
            "chart", "M3 3v18h18|M7 15l4-4 3 3 5-6",
            "alert", "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z|M12 9v4|M12 17h.01",
            "receipt", "M6 3h12v18l-3-2-3 2-3-2-3 2z|M9 8h6|M9 12h6",
            "activity", "M22 12h-4l-3 9L9 3l-3 9H2");

    static Div card(String cls) {
        Div d = new Div();
        for (String c : cls.split(" ")) {
            d.addClassName(c);
        }
        return d;
    }

    static Component cardHead(String title, Component badge) {
        H2 h = new H2(title);
        h.addClassName("dx-card-title");
        Div head = badge == null ? new Div(h) : new Div(h, badge);
        head.addClassName("dx-card-head");
        return head;
    }

    static Span pill(String text, String tone) {
        Span s = new Span(text);
        s.addClassName("dx-pill");
        if (!tone.isBlank()) {
            s.addClassName(tone);
        }
        return s;
    }

    static Paragraph note(String text) {
        Paragraph p = new Paragraph(text);
        p.addClassName("dx-note");
        return p;
    }

    static Span cap(String text) {
        Span s = new Span(text);
        s.addClassName("dx-cap");
        return s;
    }

    static Component legendItem(int i, String text) {
        Span dot = new Span();
        dot.addClassNames("bi-dot", "bi-s" + (i % 6));
        Div d = new Div(dot, new Span(text));
        d.addClassName("bi-legend-item");
        return d;
    }

    static Div iconBox(String name) {
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

    static Div kpi(String icon, String label, String value, Component foot, String tone) {
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

    static Component text(String s) {
        Span f = new Span(s);
        f.addClassName("dx-kpi-foot");
        return f;
    }

    static Component deltas(double cur, double prev, double year) {
        Div d = new Div(deltaSpan(cur, prev, L.t("o'tgan davrga", "к прошлому периоду", "vs previous")),
                deltaSpan(cur, year, L.t("o'tgan yilga", "к прошлому году", "vs last year")));
        d.addClassName("bi-deltas");
        return d;
    }

    static Span deltaSpan(double cur, double base, String tail) {
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

    static Component hbars(List<Slice> s, boolean asMoney, String tone) {
        double max = s.stream().mapToDouble(Slice::value).max().orElse(1);
        Div bars = new Div();
        bars.addClassName("dx-bars");
        for (Slice x : s) {
            Div t = new Div(new Span(x.label()), new Span(asMoney ? compact(x.value()) : FieldDef.format(x.value())));
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

    static Component donutBlock(List<Slice> s, boolean asMoney) {
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
            legend.add(legendItem(i, s.get(i).label() + " · "
                    + (asMoney ? compact(s.get(i).value()) : String.valueOf(Math.round(s.get(i).value())))
                    + " (" + Math.round(pc) + "%)"));
        }
        Div block = new Div(ring, legend);
        block.addClassName("bi-donut-block");
        return block;
    }

    /** Ustunli diagramma (hafta kunlari, soatlar). Eng baland ustun ajratib ko'rsatiladi. */
    static Component cols(List<String> labels, double[] vals) {
        double max = 0;
        int top = 0;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] > max) {
                max = vals[i];
                top = i;
            }
        }
        Div row = new Div();
        row.addClassName("bi-cols");
        for (int i = 0; i < vals.length; i++) {
            Div bar = new Div();
            bar.addClassName("bi-col-bar");
            bar.getStyle().set("height", (max <= 0 ? 0 : Math.max(2, Math.round(vals[i] * 100 / max))) + "%");
            Div wrap = new Div(bar);
            wrap.addClassName("bi-col-wrap");
            Span lab = new Span(labels.get(i));
            Div col = new Div(wrap, lab);
            col.addClassName("bi-col");
            col.getElement().setAttribute("title", labels.get(i) + ": " + money(vals[i]));
            if (i == top && max > 0) {
                col.addClassName("is-top");
            }
            row.add(col);
        }
        return row;
    }

    /** Chiziqli/maydonli grafik; prev != null bo'lsa o'tgan davr punktir chiziq bilan chiziladi. */
    static String lineSvg(double[] v, double[] prev) {
        int w = 600, h = 200, top = 16, bot = 6, n = v.length;
        if (n < 2) {
            return "<svg data-vb='0 0 600 200' viewBox='0 0 600 200'></svg>";
        }
        double max = 0;
        for (int i = 0; i < n; i++) {
            max = Math.max(max, v[i]);
            if (prev != null) {
                max = Math.max(max, prev[i]);
            }
        }
        StringBuilder sb = new StringBuilder("<svg class='dx-chart-svg' data-vb='0 0 " + w + " " + h
                + "' data-par='none' viewBox='0 0 " + w + " " + h + "' preserveAspectRatio='none' aria-hidden='true'>");
        for (int i = 0; i < 4; i++) {
            double y = top + i * ((h - top - bot) / 3.0);
            sb.append("<line class='dx-gridline' x1='0' x2='").append(w).append("' y1='").append(f(y)).append("' y2='")
                    .append(f(y)).append("' vector-effect='non-scaling-stroke'/>");
        }
        String line = path(v, max, w, h, top, bot);
        sb.append("<path class='dx-area' d='").append(line).append(" L").append(w).append(' ').append(h).append(" L0 ").append(h).append(" Z'/>");
        if (prev != null) {
            sb.append("<path class='bi-prev' d='").append(path(prev, max, w, h, top, bot)).append("' vector-effect='non-scaling-stroke'/>");
        }
        return sb.append("<path class='dx-line' d='").append(line).append("' fill='none' vector-effect='non-scaling-stroke'/></svg>").toString();
    }

    private static String path(double[] v, double max, int w, int h, int top, int bot) {
        int n = v.length;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = i * (w / (double) (n - 1));
            ys[i] = max <= 0 ? h - bot : h - bot - (v[i] / max) * (h - top - bot);
        }
        StringBuilder p = new StringBuilder("M").append(f(xs[0])).append(' ').append(f(ys[0]));
        for (int i = 1; i < n; i++) {
            double mx = (xs[i - 1] + xs[i]) / 2;
            p.append(" C").append(f(mx)).append(' ').append(f(ys[i - 1])).append(' ').append(f(mx)).append(' ')
                    .append(f(ys[i])).append(' ').append(f(xs[i])).append(' ').append(f(ys[i]));
        }
        return p.toString();
    }

    static String f(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    static String pct1(double d) {
        return f(d).replace('.', ',');
    }

    static String money(double v) {
        return FieldDef.format(v) + " " + L.t("so'm", "сум", "UZS");
    }

    static String compact(double v) {
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
        String s = f(d);
        return (s.endsWith(".0") ? s.substring(0, s.length() - 2) : s).replace('.', ',');
    }
}