package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiDataSource.*;
import com.universal.erp.platform.ui.BiFinanceSource.Payable;
import com.universal.erp.platform.ui.BiInsights.Range;
import com.universal.erp.platform.ui.BiInsights.Slice;
import com.universal.erp.platform.ui.BiInsights.Totals;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Moliyaviy tahlil hisob-kitoblari (so'mda). Sof foyda formulasi BiInsights.totals bilan bir xil.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiFinanceInsights.java
 */
@Service
public class BiFinanceInsights {

    private final BiDataSource ds;
    private final BiFinanceSource fin;
    private final BiInsights core;

    public BiFinanceInsights(BiDataSource ds, BiFinanceSource fin, BiInsights core) {
        this.ds = ds;
        this.fin = fin;
        this.core = core;
    }

    public record Flow(String label, double in, double out, double net, double balance) {}
    public record Cat(String label, double cur, double prev) {}
    public record Ratios(double gross, double operating, double net, double costShare, double payrollShare,
                         double taxLoad, double dso) {}

    public record Report(Range range, Totals cur, Totals prev, List<Flow> flows, double cash,
                         double fixedCosts, double breakEven, double safety, boolean breakEvenKnown,
                         double fcRevenue, double fcNet, boolean fcKnown,
                         Ratios ratios, List<Cat> cats, List<Slice> taxes,
                         double recvTotal, double recvOverdue, List<Slice> aging, List<Slice> debtors,
                         double payTotal, double payOverdue, List<Slice> suppliers,
                         double netPosition, List<String> insights) {}

    public Report load(String company, Range r) {
        Range pv = r.previous();
        LocalDate monthsStart = r.to().withDayOfMonth(1).minusMonths(11);
        LocalDate earliest = pv.from().isBefore(monthsStart) ? pv.from() : monthsStart;
        List<Income> inc = ds.incomes(company, earliest, r.to());
        List<Expense> exp = ds.expenses(company, earliest, r.to());
        List<Employee> emp = ds.employees(company);

        Totals cur = core.totals(inc, exp, emp, r);
        Totals prev = core.totals(inc, exp, emp, pv);
        double cash = fin.cashBalance(company);

        // 12 oylik pul oqimi; qoldiq bugungi qoldiqdan orqaga hisoblanadi
        List<Flow> flows = new ArrayList<>();
        double[] net = new double[12], in = new double[12], out = new double[12];
        String[] lab = new String[12];
        for (int i = 0; i < 12; i++) {
            LocalDate s = monthsStart.plusMonths(i);
            LocalDate e = s.withDayOfMonth(s.lengthOfMonth());
            if (e.isAfter(r.to())) e = r.to();
            Totals t = core.totals(inc, exp, emp, new Range(s, e));
            in[i] = t.revenue();
            out[i] = t.revenue() - t.net();
            net[i] = t.net();
            lab[i] = String.format("%02d.%02d", s.getMonthValue(), s.getYear() % 100);
        }
        double[] bal = new double[12];
        double later = 0;
        for (int i = 11; i >= 0; i--) {
            bal[i] = cash - later;
            later += net[i];
        }
        for (int i = 0; i < 12; i++) flows.add(new Flow(lab[i], in[i], out[i], net[i], bal[i]));

        // zararsizlik nuqtasi
        double fixed = cur.opex() + cur.payroll() + cur.socialTax() + cur.other();
        double cm = cur.revenue() > 0 ? (cur.revenue() - cur.cogs()) / cur.revenue() : 0;
        double be = cm > 0 ? fixed / cm : 0;
        boolean beKnown = be > 0;
        double safety = beKnown && cur.revenue() > 0 ? (cur.revenue() - be) * 100 / cur.revenue() : 0;

        // prognoz: oxirgi 6 oy bo'yicha chiziqli trend
        double[] r6 = Arrays.copyOfRange(in, 6, 12), n6 = Arrays.copyOfRange(net, 6, 12);
        boolean fcKnown = Arrays.stream(r6).sum() > 0;
        double fcRev = Math.max(0, predict(r6)), fcNet = predict(n6);

        // koeffitsiyentlar
        double rev = cur.revenue();
        double op = rev - cur.cogs() - cur.opex() - cur.payroll() - cur.socialTax();
        List<Debt> debts = ds.debts(company).stream().filter(d -> d.amount() > 0).toList();
        double recv = debts.stream().mapToDouble(Debt::amount).sum();
        double dso = rev > 0 ? recv / (rev / Math.max(1, r.days())) : 0;
        Ratios ratios = new Ratios(pct(rev - cur.cogs(), rev), pct(op, rev), pct(cur.net(), rev),
                pct(rev - cur.net(), rev), pct(cur.payroll() + cur.socialTax(), rev),
                pct(cur.socialTax() + cur.profitTax(), rev), dso);

        // xarajat toifalari: joriy va o'tgan davr
        Map<String, Double> cc = byCat(exp, r), pc = byCat(exp, pv);
        List<Cat> cats = cc.keySet().stream().map(k -> new Cat(k, cc.get(k), pc.getOrDefault(k, 0.0)))
                .sorted(Comparator.comparingDouble(Cat::cur).reversed()).limit(8).toList();

        List<Slice> taxes = new ArrayList<>();
        if (cur.socialTax() > 0) taxes.add(new Slice(L.t("Ijtimoiy soliq", "Соц. налог", "Social tax"), cur.socialTax()));
        if (cur.profitTax() > 0) taxes.add(new Slice(L.t("Foyda solig'i", "Налог на прибыль", "Profit tax"), cur.profitTax()));

        // debitorlik
        double recvOver = debts.stream().filter(d -> d.dueDate() != null && d.dueDate().isBefore(r.to()))
                .mapToDouble(Debt::amount).sum();
        double[] ag = new double[4];
        for (Debt d : debts) {
            long late = d.dueDate() == null ? 0 : ChronoUnit.DAYS.between(d.dueDate(), r.to());
            ag[late <= 0 ? 0 : late <= 30 ? 1 : late <= 60 ? 2 : 3] += d.amount();
        }
        List<Slice> aging = List.of(
                new Slice(L.t("Muddati kelmagan", "Не просрочено", "Not due"), ag[0]),
                new Slice(L.t("1–30 kun kechikkan", "1–30 дн.", "1–30 days late"), ag[1]),
                new Slice(L.t("31–60 kun kechikkan", "31–60 дн.", "31–60 days late"), ag[2]),
                new Slice(L.t("60+ kun kechikkan", "60+ дн.", "60+ days late"), ag[3]));
        List<Slice> debtors = debts.stream().collect(Collectors.groupingBy(Debt::customer, Collectors.summingDouble(Debt::amount)))
                .entrySet().stream().map(e -> new Slice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(5).toList();

        // kreditorlik
        List<Payable> pays = fin.payables(company).stream().filter(p -> p.amount() > 0).toList();
        double payTotal = pays.stream().mapToDouble(Payable::amount).sum();
        double payOver = pays.stream().filter(p -> p.dueDate() != null && p.dueDate().isBefore(r.to()))
                .mapToDouble(Payable::amount).sum();
        List<Slice> sup = pays.stream().collect(Collectors.groupingBy(Payable::supplier, Collectors.summingDouble(Payable::amount)))
                .entrySet().stream().map(e -> new Slice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(5).toList();

        double netPos = cash + recv - payTotal;
        List<String> ins = insights(cur, prev, cash, recv, recvOver, payTotal, safety, beKnown, ratios, cats, net);
        return new Report(r, cur, prev, flows, cash, fixed, be, safety, beKnown, fcRev, fcNet, fcKnown, ratios, cats, taxes,
                recv, recvOver, aging, debtors, payTotal, payOver, sup, netPos, ins);
    }

    private static Map<String, Double> byCat(List<Expense> exp, Range r) {
        return exp.stream().filter(x -> r.has(x.date())).collect(Collectors.groupingBy(
                x -> x.category() == null || x.category().isBlank() ? "—" : x.category(),
                Collectors.summingDouble(Expense::amount)));
    }

    private static double pct(double a, double b) {
        return b <= 0 ? 0 : a * 100 / b;
    }

    /** Eng kichik kvadratlar: x = 0..n-1 bo'yicha x = n dagi qiymat. */
    private static double predict(double[] y) {
        int n = y.length;
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) {
            sx += i;
            sy += y[i];
            sxy += i * y[i];
            sxx += (double) i * i;
        }
        double den = n * sxx - sx * sx;
        double slope = den == 0 ? 0 : (n * sxy - sx * sy) / den;
        return (sy - slope * sx) / n + slope * n;
    }

    private List<String> insights(Totals c, Totals p, double cash, double recv, double recvOver, double pay,
                                  double safety, boolean beKnown, Ratios ra, List<Cat> cats, double[] net) {
        List<String> out = new ArrayList<>();
        if (p.net() != 0) {
            double d = (c.net() - p.net()) * 100 / Math.abs(p.net());
            out.add(L.t("Sof foyda o'tgan davrga nisbatan " + Math.abs(Math.round(d)) + "% " + (d >= 0 ? "oshdi" : "kamaydi"),
                    "Чистая прибыль " + (d >= 0 ? "выросла" : "снизилась") + " на " + Math.abs(Math.round(d)) + "%",
                    "Net profit " + (d >= 0 ? "grew " : "fell ") + Math.abs(Math.round(d)) + "% vs previous period"));
        }
        if (beKnown && c.revenue() > 0) {
            out.add(safety >= 0
                    ? L.t("Zararsizlik nuqtasidan " + Math.round(safety) + "% yuqoridasiz", "Выше точки безубыточности на " + Math.round(safety) + "%", "You are " + Math.round(safety) + "% above break-even")
                    : L.t("Daromad zararsizlik nuqtasidan past", "Выручка ниже точки безубыточности", "Revenue is below break-even"));
        }
        if (recv > 0 && recvOver > 0) {
            out.add(L.t("Mijozlar qarzining " + Math.round(recvOver * 100 / recv) + "% i muddati o'tgan",
                    Math.round(recvOver * 100 / recv) + "% долга клиентов просрочено",
                    Math.round(recvOver * 100 / recv) + "% of customer debt is overdue"));
        }
        if (pay > 0 && pay > cash) {
            out.add(L.t("Yetkazib beruvchilarga qarz pul qoldig'idan katta", "Долг поставщикам больше остатка денег",
                    "Supplier debt exceeds cash on hand"));
        }
        double avg3 = (net[9] + net[10] + net[11]) / 3;
        if (avg3 < 0 && cash > 0) {
            out.add(L.t("Hozirgi sur'atda pul " + Math.round(cash / -avg3) + " oyga yetadi",
                    "При текущем темпе денег хватит на " + Math.round(cash / -avg3) + " мес.",
                    "At the current burn rate cash lasts " + Math.round(cash / -avg3) + " months"));
        }
        for (Cat k : cats) {
            if (k.prev() > 0 && k.cur() > k.prev() * 1.3 && k.cur() - k.prev() > 0) {
                out.add(L.t("«" + k.label() + "» xarajati " + Math.round((k.cur() / k.prev() - 1) * 100) + "% oshdi",
                        "Расходы «" + k.label() + "» выросли на " + Math.round((k.cur() / k.prev() - 1) * 100) + "%",
                        "“" + k.label() + "” costs rose " + Math.round((k.cur() / k.prev() - 1) * 100) + "%"));
                break;
            }
        }
        if (ra.taxLoad() > 0) {
            out.add(L.t("Soliq yuki daromadning " + Math.round(ra.taxLoad()) + "% i", "Налоговая нагрузка — " + Math.round(ra.taxLoad()) + "% выручки",
                    "Tax burden is " + Math.round(ra.taxLoad()) + "% of revenue"));
        }
        return out;
    }
}