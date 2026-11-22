package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiInsights.Range;
import com.universal.erp.platform.ui.BiInsights.Slice;
import com.universal.erp.platform.ui.BiSalesSource.Line;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Savdo tahlili hisob-kitoblari (so'mda).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiSalesInsights.java
 */
@Service
public class BiSalesInsights {

    private final BiSalesSource src;

    public BiSalesInsights(BiSalesSource src) {
        this.src = src;
    }

    public record Totals(double revenue, int receipts, double avgReceipt, double qty, double gross,
                         double margin, double returns, int customers) {}

    public record Point(String label, double value, double prev) {}

    public record Cashier(String name, double revenue, int receipts) {}

    public record Report(Range range, Totals cur, Totals prev, Totals year, List<Point> trend, boolean hasPrev,
                         double[] weekday, double[] hours, int hourFrom,
                         List<Slice> byRevenue, List<Slice> byProfit, List<Slice> categories, List<Slice> payments,
                         List<Cashier> cashiers, List<Slice> customers, List<Slice> lowMargin, List<Slice> returned,
                         List<String> insights) {}

    public Report load(String company, Range r) {
        Range pv = r.previous(), py = r.yearAgo();
        LocalDate earliest = pv.from().isBefore(py.from()) ? pv.from() : py.from();
        List<Line> all = src.lines(company, earliest, r.to());
        List<Line> cur = in(all, r), prev = in(all, pv), year = in(all, py);
        Totals tc = totals(cur), tp = totals(prev), ty = totals(year);

        // dinamika: 92 kungacha kunlik, undan uzoq bo'lsa oylik
        List<Point> trend = new ArrayList<>();
        boolean daily = r.days() <= 92;
        if (daily) {
            Map<LocalDate, Double> a = byDay(cur), b = byDay(prev);
            for (int i = 0; i < r.days(); i++) {
                LocalDate d = r.from().plusDays(i);
                trend.add(new Point(String.format("%02d.%02d", d.getDayOfMonth(), d.getMonthValue()),
                        a.getOrDefault(d, 0.0), b.getOrDefault(pv.from().plusDays(i), 0.0)));
            }
        } else {
            Map<String, Double> m = new TreeMap<>(cur.stream().collect(Collectors.groupingBy(
                    l -> String.format("%d-%02d", l.time().getYear(), l.time().getMonthValue()),
                    Collectors.summingDouble(Line::revenue))));
            m.forEach((k, v) -> trend.add(new Point(k.substring(5) + "." + k.substring(2, 4), v, 0)));
        }

        double[] wd = new double[7], hr = new double[24];
        for (Line l : cur) {
            wd[l.time().getDayOfWeek().getValue() - 1] += l.revenue();
            hr[l.time().getHour()] += l.revenue();
        }
        int hFrom = 0, hTo = 23;
        while (hFrom < 23 && hr[hFrom] == 0) hFrom++;
        while (hTo > hFrom && hr[hTo] == 0) hTo--;
        double[] hours = Arrays.copyOfRange(hr, hFrom, hTo + 1);

        Map<String, double[]> prod = new HashMap<>(); // [revenue, cost, qty]
        for (Line l : cur) {
            double[] a = prod.computeIfAbsent(l.product(), k -> new double[3]);
            a[0] += l.revenue();
            a[1] += l.cost();
            a[2] += l.qty();
        }
        List<Slice> byRev = prod.entrySet().stream().filter(e -> e.getValue()[0] > 0)
                .map(e -> new Slice(e.getKey(), e.getValue()[0]))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(6).toList();
        List<Slice> byProfit = prod.entrySet().stream().filter(e -> e.getValue()[0] - e.getValue()[1] > 0)
                .map(e -> new Slice(e.getKey(), e.getValue()[0] - e.getValue()[1]))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(6).toList();
        List<Slice> lowMargin = prod.entrySet().stream().filter(e -> e.getValue()[0] > 0
                        && (e.getValue()[0] - e.getValue()[1]) * 100 / e.getValue()[0] < 10)
                .sorted((x, y) -> Double.compare(y.getValue()[0], x.getValue()[0])).limit(5)
                .map(e -> new Slice(e.getKey(), (e.getValue()[0] - e.getValue()[1]) * 100 / e.getValue()[0])).toList();
        List<Slice> returned = cur.stream().filter(l -> l.revenue() < 0)
                .collect(Collectors.groupingBy(Line::product, Collectors.summingDouble(l -> -l.revenue())))
                .entrySet().stream().map(e -> new Slice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(5).toList();

        List<Slice> cats = top(group(cur, Line::category), 6);
        List<Slice> pays = group(cur, Line::payment);
        List<Cashier> cashiers = cur.stream().filter(l -> l.cashier() != null && !l.cashier().isBlank())
                .collect(Collectors.groupingBy(Line::cashier)).entrySet().stream()
                .map(e -> new Cashier(e.getKey(), e.getValue().stream().mapToDouble(Line::revenue).sum(),
                        (int) e.getValue().stream().filter(l -> l.revenue() > 0).map(Line::receipt).distinct().count()))
                .sorted(Comparator.comparingDouble(Cashier::revenue).reversed()).limit(6).toList();
        List<Slice> customers = cur.stream().filter(l -> l.customer() != null && !l.customer().isBlank())
                .collect(Collectors.groupingBy(Line::customer, Collectors.summingDouble(Line::revenue)))
                .entrySet().stream().filter(e -> e.getValue() > 0).map(e -> new Slice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(5).toList();

        List<String> ins = insights(tc, tp, wd, hours, hFrom, byRev, lowMargin, customers);
        return new Report(r, tc, tp, ty, trend, daily && prev.size() > 0, wd, hours, hFrom, byRev, byProfit, cats, pays,
                cashiers, customers, lowMargin, returned, ins);
    }

    private static List<Line> in(List<Line> all, Range r) {
        return all.stream().filter(l -> l.time() != null && r.has(l.time().toLocalDate())).toList();
    }

    private static Map<LocalDate, Double> byDay(List<Line> ls) {
        return ls.stream().collect(Collectors.groupingBy(l -> l.time().toLocalDate(), Collectors.summingDouble(Line::revenue)));
    }

    private static Totals totals(List<Line> ls) {
        double rev = ls.stream().mapToDouble(Line::revenue).sum();
        double cost = ls.stream().mapToDouble(Line::cost).sum();
        int receipts = (int) ls.stream().filter(l -> l.revenue() > 0).map(Line::receipt).distinct().count();
        double ret = ls.stream().filter(l -> l.revenue() < 0).mapToDouble(l -> -l.revenue()).sum();
        int cust = (int) ls.stream().map(Line::customer).filter(c -> c != null && !c.isBlank()).distinct().count();
        return new Totals(rev, receipts, receipts == 0 ? 0 : rev / receipts, ls.stream().mapToDouble(Line::qty).sum(),
                rev - cost, rev <= 0 ? 0 : (rev - cost) * 100 / rev, ret, cust);
    }

    private static List<Slice> group(List<Line> ls, Function<Line, String> key) {
        return ls.stream().collect(Collectors.groupingBy(l -> {
                    String k = key.apply(l);
                    return k == null || k.isBlank() ? "—" : k;
                }, Collectors.summingDouble(Line::revenue))).entrySet().stream()
                .filter(e -> e.getValue() > 0).map(e -> new Slice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).toList();
    }

    private static List<Slice> top(List<Slice> s, int n) {
        if (s.size() <= n) return s;
        List<Slice> r = new ArrayList<>(s.subList(0, n - 1));
        r.add(new Slice(L.t("Boshqa", "Прочее", "Other"), s.subList(n - 1, s.size()).stream().mapToDouble(Slice::value).sum()));
        return r;
    }

    public static String dayName(int idx) {
        return DayOfWeek.of(idx + 1).getDisplayName(TextStyle.SHORT, Locale.forLanguageTag(
                L.t("uz", "ru", "en")));
    }

    private List<String> insights(Totals c, Totals p, double[] wd, double[] hours, int hFrom, List<Slice> byRev,
                                  List<Slice> low, List<Slice> customers) {
        List<String> out = new ArrayList<>();
        if (p.revenue() > 0) {
            double d = (c.revenue() - p.revenue()) * 100 / p.revenue();
            out.add(L.t("Savdo o'tgan davrga nisbatan " + Math.abs(Math.round(d)) + "% " + (d >= 0 ? "oshdi" : "kamaydi"),
                    "Продажи " + (d >= 0 ? "выросли" : "снизились") + " на " + Math.abs(Math.round(d)) + "% к прошлому периоду",
                    "Sales " + (d >= 0 ? "grew " : "fell ") + Math.abs(Math.round(d)) + "% vs previous period"));
        }
        if (c.revenue() > 0) {
            int best = 0;
            for (int i = 1; i < 7; i++) if (wd[i] > wd[best]) best = i;
            out.add(L.t("Eng kuchli kun: " + dayName(best), "Самый сильный день: " + dayName(best), "Best day: " + dayName(best)));
            int pk = 0;
            for (int i = 1; i < hours.length; i++) if (hours[i] > hours[pk]) pk = i;
            String h = String.format("%02d:00–%02d:00", hFrom + pk, hFrom + pk + 1);
            out.add(L.t("Eng gavjum soat: " + h, "Пиковый час: " + h, "Peak hour: " + h));
        }
        if (!byRev.isEmpty() && c.revenue() > 0) {
            long sh = Math.round(byRev.get(0).value() * 100 / c.revenue());
            out.add(L.t(byRev.get(0).label() + " daromadning " + sh + "% ini beradi",
                    byRev.get(0).label() + " даёт " + sh + "% выручки", byRev.get(0).label() + " brings " + sh + "% of revenue"));
        }
        if (c.revenue() > 0 && c.returns() * 100 / (c.revenue() + c.returns()) > 5) {
            out.add(L.t("Qaytarishlar savdoning 5% idan oshdi", "Возвраты превысили 5% продаж", "Returns exceed 5% of sales"));
        }
        if (!low.isEmpty()) {
            out.add(L.t(low.size() + " ta tovarning marjasi 10% dan past", low.size() + " поз. с маржой ниже 10%",
                    low.size() + " items have margin below 10%"));
        }
        if (!customers.isEmpty() && c.revenue() > 0 && customers.get(0).value() * 100 / c.revenue() > 30) {
            out.add(L.t("Savdoning 30% dan ko'pi bitta mijozga bog'liq", "Более 30% продаж зависит от одного клиента",
                    "Over 30% of sales depend on one customer"));
        }
        return out;
    }
}