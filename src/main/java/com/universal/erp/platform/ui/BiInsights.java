package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.BiDataSource.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * BI hisob-kitoblari. Hammasi so'mda.
 *
 * Sof foyda = Daromad − Tannarx − Xarajatlar − Ish haqi − Ijtimoiy soliq (12%)
 *             − Boshqa to'lovlar − Foyda solig'i.
 * Foyda solig'i: Moliyada TAX yozuvlari bo'lsa shular olinadi, bo'lmasa
 * soliq oldi foydaning 15% i taxminan hisoblanadi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiInsights.java
 */
@Service
public class BiInsights {

    public static final double SOCIAL_TAX_RATE = 0.12;
    public static final double PROFIT_TAX_RATE = 0.15;

    private final BiDataSource ds;

    public BiInsights(BiDataSource ds) {
        this.ds = ds;
    }

    // ------------------------------------------------------------ modellar

    public record Range(LocalDate from, LocalDate to) {
        public long days() { return ChronoUnit.DAYS.between(from, to) + 1; }
        public Range previous() {
            LocalDate pt = from.minusDays(1);
            return new Range(pt.minusDays(days() - 1), pt);
        }
        public Range yearAgo() { return new Range(from.minusYears(1), to.minusYears(1)); }
        public boolean has(LocalDate d) { return d != null && !d.isBefore(from) && !d.isAfter(to); }
    }

    public record Totals(double revenue, double cogs, double opex, double payroll, double socialTax,
                         double other, double pretax, double profitTax, boolean taxEstimated,
                         double net, double margin) {}

    public record Slice(String label, double value) {}
    public record Month(String label, double revenue, double cost, double net) {}

    public record Report(Range range, Totals cur, Totals prev, Totals year, List<Month> months,
                         List<Slice> expenseMix,
                         List<Slice> funnel, double conversion, double avgDeal, List<Slice> topCustomers,
                         int openDeals, double openDealsSum, int customersTotal, int customersNew,
                         double stockValue, int stockCount, List<Slice> stockByCategory,
                         List<Slice> fast, List<Slice> dead, List<Slice> abc, List<Slice> abcTop,
                         int employees, List<Slice> departments, double payrollShare,
                         double debtTotal, double overdueTotal, int debtCustomers,
                         List<Slice> debtors, List<Slice> aging,
                         List<String> insights) {}

    // ------------------------------------------------------------ hisob

    public Report load(String company, Range r) {
        Range pv = r.previous();
        Range py = r.yearAgo();
        LocalDate monthsStart = r.to().withDayOfMonth(1).minusMonths(11);
        LocalDate earliest = Collections.min(List.of(pv.from(), py.from(), monthsStart));

        List<Income> inc = ds.incomes(company, earliest, r.to());
        List<Expense> exp = ds.expenses(company, earliest, r.to());
        List<Employee> emp = ds.employees(company);

        Totals cur = totals(inc, exp, emp, r);
        Totals prev = totals(inc, exp, emp, pv);
        Totals year = totals(inc, exp, emp, py);

        List<Month> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate s = monthsStart.plusMonths(i);
            LocalDate e = s.withDayOfMonth(s.lengthOfMonth());
            if (e.isAfter(r.to())) e = r.to();
            Totals t = totals(inc, exp, emp, new Range(s, e));
            months.add(new Month(String.format("%02d.%02d", s.getMonthValue(), s.getYear() % 100),
                    t.revenue(), t.revenue() - t.net(), t.net()));
        }

        // xarajatlar tarkibi
        Map<String, Double> mix = new LinkedHashMap<>();
        for (Expense x : exp) {
            if (r.has(x.date())) mix.merge(x.category(), x.amount(), Double::sum);
        }
        if (cur.payroll() > 0) mix.merge(L.t("Ish haqi", "Зарплата", "Payroll"), cur.payroll(), Double::sum);
        if (cur.socialTax() > 0) mix.merge(L.t("Ijtimoiy soliq", "Соц. налог", "Social tax"), cur.socialTax(), Double::sum);
        if (cur.profitTax() > 0 && cur.taxEstimated()) {
            mix.merge(L.t("Foyda solig'i", "Налог на прибыль", "Profit tax"), cur.profitTax(), Double::sum);
        }
        List<Slice> expenseMix = top(slices(mix), 6, L.t("Boshqa", "Прочее", "Other"));

        // CRM
        List<Deal> deals = ds.deals(company);
        Map<String, Long> st = deals.stream().filter(d -> r.has(d.date()))
                .collect(Collectors.groupingBy(d -> stageOf(d.stage()), Collectors.counting()));
        long won = st.getOrDefault("won", 0L), lost = st.getOrDefault("lost", 0L);
        List<Slice> funnel = List.of(
                new Slice(L.t("Lid", "Лид", "Lead"), st.getOrDefault("lead", 0L) + st.getOrDefault("negotiation", 0L) + won + lost),
                new Slice(L.t("Muzokara", "Переговоры", "Negotiation"), st.getOrDefault("negotiation", 0L) + won + lost),
                new Slice(L.t("Yutildi", "Выиграно", "Won"), won),
                new Slice(L.t("Yo'qotildi", "Потеряно", "Lost"), lost));
        double conversion = won + lost == 0 ? 0 : won * 100.0 / (won + lost);
        List<Deal> wonDeals = deals.stream().filter(d -> r.has(d.date()) && "won".equals(stageOf(d.stage()))).toList();
        double avgDeal = wonDeals.isEmpty() ? 0 : sum(wonDeals, Deal::amount) / wonDeals.size();
        List<Slice> topCustomers = slices(wonDeals.stream().collect(
                        Collectors.groupingBy(Deal::customer, Collectors.summingDouble(Deal::amount)))).stream()
                .limit(5).toList();
        List<Deal> open = deals.stream().filter(d -> {
            String s = stageOf(d.stage());
            return !s.equals("won") && !s.equals("lost");
        }).toList();
        List<Customer> customers = ds.customers(company);
        int newCustomers = (int) customers.stream().filter(c -> r.has(c.created())).count();

        // Ombor
        List<Stock> stock = ds.stock(company);
        double stockValue = stock.stream().mapToDouble(s -> s.qty() * s.unitCost()).sum();
        List<Slice> stockByCat = top(slices(stock.stream().collect(Collectors.groupingBy(
                s -> s.category() == null || s.category().isBlank() ? "—" : s.category(),
                Collectors.summingDouble(s -> s.qty() * s.unitCost())))), 6, L.t("Boshqa", "Прочее", "Other"));

        List<Sale> sales = ds.sales(company, r.to().minusDays(Math.max(r.days(), 60)), r.to());
        List<Sale> salesInRange = sales.stream().filter(s -> r.has(s.date())).toList();
        List<Slice> fast = slices(salesInRange.stream().collect(
                Collectors.groupingBy(Sale::product, Collectors.summingDouble(Sale::qty)))).stream().limit(5).toList();
        Set<String> sold60 = sales.stream().filter(s -> !s.date().isBefore(r.to().minusDays(60)))
                .map(Sale::product).collect(Collectors.toSet());
        List<Slice> dead = stock.stream().filter(s -> s.qty() > 0 && !sold60.contains(s.name()))
                .map(s -> new Slice(s.name(), s.qty() * s.unitCost()))
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).limit(5).toList();

        // ABC (daromad bo'yicha)
        List<Slice> byRev = slices(salesInRange.stream().collect(
                Collectors.groupingBy(Sale::product, Collectors.summingDouble(Sale::revenue))));
        double totRev = byRev.stream().mapToDouble(Slice::value).sum();
        int a = 0, b = 0, c = 0;
        double cum = 0;
        List<Slice> abcTop = new ArrayList<>();
        for (Slice s : byRev) {
            cum += s.value();
            double share = totRev <= 0 ? 1 : cum / totRev;
            if (share <= 0.8 || a == 0) { a++; if (abcTop.size() < 5) abcTop.add(s); }
            else if (share <= 0.95) b++;
            else c++;
        }
        List<Slice> abc = List.of(new Slice("A", a), new Slice("B", b), new Slice("C", c));

        // Kadrlar
        List<Slice> depts = slices(emp.stream().collect(Collectors.groupingBy(
                e -> e.department() == null || e.department().isBlank() ? "—" : e.department(), Collectors.counting()))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue())));
        double payrollShare = cur.revenue() <= 0 ? 0 : (cur.payroll() + cur.socialTax()) * 100 / cur.revenue();

        // Qarzlar
        List<Debt> debts = ds.debts(company).stream().filter(d -> d.amount() > 0).toList();
        double debtTotal = sum(debts, Debt::amount);
        double overdue = debts.stream().filter(d -> d.dueDate() != null && d.dueDate().isBefore(r.to()))
                .mapToDouble(Debt::amount).sum();
        Map<String, Double> byCust = debts.stream().collect(Collectors.groupingBy(Debt::customer, Collectors.summingDouble(Debt::amount)));
        double[] ag = new double[4];
        for (Debt d : debts) {
            long late = d.dueDate() == null ? 0 : ChronoUnit.DAYS.between(d.dueDate(), r.to());
            ag[late <= 0 ? 0 : late <= 30 ? 1 : late <= 60 ? 2 : 3] += d.amount();
        }
        List<Slice> aging = List.of(
                new Slice(L.t("Muddati kelmagan", "Не просрочено", "Not due"), ag[0]),
                new Slice(L.t("1–30 kun kechikkan", "1–30 дн. просрочки", "1–30 days late"), ag[1]),
                new Slice(L.t("31–60 kun kechikkan", "31–60 дн. просрочки", "31–60 days late"), ag[2]),
                new Slice(L.t("60+ kun kechikkan", "60+ дн. просрочки", "60+ days late"), ag[3]));

        List<String> ins = insights(cur, prev, r, dead, debtTotal, overdue, conversion, payrollShare);

        return new Report(r, cur, prev, year, months, expenseMix, funnel, conversion, avgDeal, topCustomers,
                open.size(), sum(open, Deal::amount), customers.size(), newCustomers,
                stockValue, stock.size(), stockByCat, fast, dead, abc, abcTop,
                emp.size(), depts, payrollShare, debtTotal, overdue, byCust.size(),
                top(slices(byCust), 6, L.t("Boshqa", "Прочее", "Other")), aging, ins);
    }

    Totals totals(List<Income> inc, List<Expense> exp, List<Employee> emp, Range r) {
        double revenue = inc.stream().filter(i -> r.has(i.date())).mapToDouble(Income::amount).sum();
        double cogs = kind(exp, r, Kind.COGS), opex = kind(exp, r, Kind.OPEX);
        double other = kind(exp, r, Kind.OTHER), taxPaid = kind(exp, r, Kind.TAX);
        double payroll = emp.stream().mapToDouble(Employee::monthlySalary).sum() * (r.days() / 30.4);
        double social = payroll * SOCIAL_TAX_RATE;
        double pretax = revenue - cogs - opex - payroll - social - other;
        boolean est = taxPaid <= 0;
        double profitTax = est ? Math.max(0, pretax) * PROFIT_TAX_RATE : taxPaid;
        double net = pretax - profitTax;
        return new Totals(revenue, cogs, opex, payroll, social, other, pretax, profitTax, est, net,
                revenue <= 0 ? 0 : net * 100 / revenue);
    }

    private static double kind(List<Expense> exp, Range r, Kind k) {
        return exp.stream().filter(x -> x.kind() == k && r.has(x.date())).mapToDouble(Expense::amount).sum();
    }

    private List<String> insights(Totals cur, Totals prev, Range r, List<Slice> dead, double debt,
                                  double overdue, double conv, double payrollShare) {
        List<String> out = new ArrayList<>();
        if (prev.revenue() > 0) {
            double p = (cur.revenue() - prev.revenue()) * 100 / prev.revenue();
            out.add(L.t("Daromad o'tgan davrga nisbatan " + Math.abs(Math.round(p)) + "% " + (p >= 0 ? "oshdi" : "kamaydi"),
                    "Выручка " + (p >= 0 ? "выросла" : "снизилась") + " на " + Math.abs(Math.round(p)) + "% к прошлому периоду",
                    "Revenue " + (p >= 0 ? "grew " : "fell ") + Math.abs(Math.round(p)) + "% vs previous period"));
        }
        if (cur.revenue() > 0 && cur.net() < 0) {
            out.add(L.t("Davr zarar bilan yopildi: sof foyda manfiy", "Период убыточен: чистая прибыль отрицательна",
                    "The period is at a loss: net profit is negative"));
        }
        if (!dead.isEmpty()) {
            out.add(L.t(dead.size() + " ta tovar 60 kundan beri sotilmagan (" + FieldDef.format(
                            dead.stream().mapToDouble(Slice::value).sum()) + " so'm band)",
                    dead.size() + " поз. не продаются 60 дней",
                    dead.size() + " items have not sold in 60 days"));
        }
        if (debt > 0 && overdue > 0) {
            out.add(L.t("Qarzning " + Math.round(overdue * 100 / debt) + "% i muddati o'tgan",
                    Math.round(overdue * 100 / debt) + "% долга просрочено",
                    Math.round(overdue * 100 / debt) + "% of debt is overdue"));
        }
        if (payrollShare > 40) {
            out.add(L.t("Ish haqi va soliqlari daromadning " + Math.round(payrollShare) + "% ini yeydi",
                    "ФОТ с налогами — " + Math.round(payrollShare) + "% выручки",
                    "Payroll with taxes takes " + Math.round(payrollShare) + "% of revenue"));
        }
        if (conv > 0) {
            out.add(L.t("Bitimlar konversiyasi " + Math.round(conv) + "%", "Конверсия сделок " + Math.round(conv) + "%",
                    "Deal conversion is " + Math.round(conv) + "%"));
        }
        return out;
    }

    // ------------------------------------------------------------ yordamchilar

    static String stageOf(String s) {
        String x = s == null ? "" : s.toLowerCase(Locale.ROOT);
        if (x.contains("yut") || x.contains("won") || x.contains("выигр") || x.contains("успеш")) return "won";
        if (x.contains("yo'q") || x.contains("yoq") || x.contains("lost") || x.contains("проигр") || x.contains("потер")) return "lost";
        if (x.contains("muzok") || x.contains("negot") || x.contains("перег")) return "negotiation";
        return "lead";
    }

    private static <T> double sum(Collection<T> c, ToDoubleFunction<T> f) {
        return c.stream().mapToDouble(f).sum();
    }

    private static <K extends Number> List<Slice> slices(Map<String, K> m) {
        return m.entrySet().stream().map(e -> new Slice(e.getKey(), e.getValue().doubleValue()))
                .filter(s -> s.value() > 0)
                .sorted(Comparator.comparingDouble(Slice::value).reversed()).toList();
    }

    private static List<Slice> top(List<Slice> s, int n, String otherLabel) {
        if (s.size() <= n) return s;
        List<Slice> r = new ArrayList<>(s.subList(0, n - 1));
        r.add(new Slice(otherLabel, s.subList(n - 1, s.size()).stream().mapToDouble(Slice::value).sum()));
        return r;
    }
}