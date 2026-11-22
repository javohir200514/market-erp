package com.universal.erp.platform.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Ochiq bosh sahifa (landing): loyiha haqida ma'lumot.
 * Navbar — {@link PublicNavbar}, stil — styles/navbar.css va styles/landing.css.
 */
@Route("home")
@PageTitle("Uz-mark ERP")
@AnonymousAllowed
@CssImport("./styles/navbar.css")
@CssImport("./styles/landing.css")
public class LandingView extends Div implements LocaleChangeObserver {

    /** Til o'zgarganda matnlarni yangilaydigan funksiyalar ro'yxati. */
    private final List<Runnable> refreshers = new ArrayList<>();

    public LandingView() {
        addClassName("landing-page");

        add(
                new PublicNavbar(PublicNavbar.Page.HOME),
                buildHero(),
                buildModules(),
                buildWhy(),
                buildCta(),
                buildFooter()
        );
        refreshTexts();
    }

    // ================================================================ hero

    private Div buildHero() {
        Div ringA = ring("landing-ring-a");
        Div ringB = ring("landing-ring-b");

        Span eyebrow = new Span("Uz-mark ERP");
        eyebrow.addClassName("landing-eyebrow");

        H1 title = i18n(new H1(),
                "Bitta platforma. Har bir biznes uchun.",
                "Одна платформа. Для любого бизнеса.",
                "One platform. Every business.");
        title.addClassName("landing-title");

        Paragraph lead = i18n(new Paragraph(),
                "Moliya, savdo, ombor, kadrlar va tahlil — barchasi bitta tizimda. "
                        + "Faqat kerakli modullarni yoqing va biznesingiz bilan birga o'sing.",
                "Финансы, продажи, склад, кадры и аналитика — в одной системе. "
                        + "Подключайте только нужные модули и растите вместе с бизнесом.",
                "Finance, sales, inventory, HR and analytics in one system. "
                        + "Turn on only the modules you need and grow with your business.");
        lead.addClassName("landing-lead");

        RouterLink start = i18n(new RouterLink(RegisterView.class),
                "Bepul boshlash", "Начать бесплатно", "Get started free");
        start.addClassNames("landing-btn", "landing-btn-primary");

        RouterLink signIn = i18n(new RouterLink(LoginView.class),
                "Tizimga kirish", "Войти в систему", "Sign in");
        signIn.addClassNames("landing-btn", "landing-btn-ghost");

        Div actions = new Div(start, signIn);
        actions.addClassName("landing-actions");

        Paragraph note = i18n(new Paragraph(),
                "14 kunlik sinov muddati bilan.",
                "С 14-дневным пробным периодом.",
                "Includes a 14-day trial.");
        note.addClassName("landing-note");

        Div chips = new Div(
                chip("Modulli arxitektura", "Модульная архитектура", "Modular architecture"),
                chip("Ko'p kompaniyali (multi-tenant)", "Мультикомпанийность (multi-tenant)", "Multi-company (multi-tenant)"),
                chip("O'zbek · Русский · English", "Узбекский · Русский · English", "Uzbek · Russian · English")
        );
        chips.addClassName("landing-chips");

        Div inner = new Div(eyebrow, title, lead, actions, note, chips);
        inner.addClassName("landing-hero-inner");

        Div hero = new Div(ringA, ringB, inner);
        hero.addClassName("landing-hero");
        return hero;
    }

    // ================================================================ modullar

    private Div buildModules() {
        Div grid = new Div(
                card(VaadinIcon.WALLET,
                        "Moliya va buxgalteriya", "Финансы и бухгалтерия", "Finance & accounting",
                        "Ikki yoqlama yozuv, bosh kitob, kassa va bank, debitor va kreditor qarzlar, moliyaviy hisobotlar.",
                        "Двойная запись, главная книга, касса и банк, дебиторская и кредиторская задолженность, финансовая отчётность.",
                        "Double-entry ledger, cash and bank, receivables and payables, financial statements."),
                card(VaadinIcon.HANDSHAKE,
                        "CRM va savdo", "CRM и продажи", "CRM & sales",
                        "Mijozlar, takliflar, buyurtmalar, narx ro'yxatlari va sotuv voronkasi.",
                        "Клиенты, коммерческие предложения, заказы, прайс-листы и воронка продаж.",
                        "Customers, quotations, orders, price lists and the sales pipeline."),
                card(VaadinIcon.CUBES,
                        "Ombor va inventar", "Склад и запасы", "Inventory & warehouse",
                        "Bir nechta ombor, qoldiqlar, ko'chirishlar, partiya va seriya raqamlari, zaxiralash.",
                        "Несколько складов, остатки, перемещения, партии и серийные номера, резервирование.",
                        "Multiple warehouses, stock levels, transfers, batches and serial numbers, reservations."),
                card(VaadinIcon.USERS,
                        "Kadrlar va ish haqi", "Кадры и зарплата", "HR & payroll",
                        "Xodimlar, davomat, ta'tillar, ish haqi hisobi va samaradorlik ko'rsatkichlari.",
                        "Сотрудники, посещаемость, отпуска, расчёт зарплаты и показатели эффективности.",
                        "Employees, attendance, leave, payroll and performance indicators."),
                card(VaadinIcon.PIE_CHART,
                        "Tahlil va dashboardlar", "Аналитика и дашборды", "Analytics & dashboards",
                        "Vizual dashboardlar, KPI ko'rsatkichlari, o'z hisobotlaringizni yaratish va eksport.",
                        "Визуальные дашборды, KPI, создание собственных отчётов и экспорт.",
                        "Visual dashboards, KPIs, custom reports and export."),
                soonCard(VaadinIcon.PUZZLE_PIECE,
                        "Yangi modullar — tez orada", "Новые модули — скоро", "More modules — coming soon",
                        "Xaridlar, ishlab chiqarish, logistika va loyihalar boshqaruvi.",
                        "Закупки, производство, логистика и управление проектами.",
                        "Purchasing, production, logistics and project management.")
        );
        grid.addClassName("landing-grid");

        return section(
                "Modullar", "Модули", "Modules",
                "Biznesingiz uchun kerakli barcha yo'nalishlar", "Все направления, нужные вашему бизнесу", "Everything your business needs",
                grid);
    }

    // ================================================================ nima uchun

    private Div buildWhy() {
        Div grid = new Div(
                card(VaadinIcon.SHIELD,
                        "Xavfsizlik", "Безопасность", "Security",
                        "Har bir kompaniya ma'lumotlari alohida himoyalangan, ruxsatlar rollar bo'yicha boshqariladi.",
                        "Данные каждой компании изолированы, доступ управляется ролями.",
                        "Each company's data is isolated and access is controlled by roles."),
                card(VaadinIcon.PUZZLE_PIECE,
                        "Modullilik", "Модульность", "Modularity",
                        "Faqat kerakli modullarni yoqing — ortiqcha murakkablik yo'q.",
                        "Подключайте только нужные модули — без лишней сложности.",
                        "Enable only the modules you need — no extra complexity."),
                card(VaadinIcon.GLOBE,
                        "Uch tilda", "На трёх языках", "Three languages",
                        "O'zbek, rus va ingliz tillarida to'liq interfeys.",
                        "Полноценный интерфейс на узбекском, русском и английском.",
                        "Full interface in Uzbek, Russian and English."),
                card(VaadinIcon.CLUSTER,
                        "Yagona jarayon", "Единый процесс", "One connected flow",
                        "Savdo, ombor va moliya o'zaro bog'langan — ma'lumot bir marta kiritiladi.",
                        "Продажи, склад и финансы связаны между собой — данные вводятся один раз.",
                        "Sales, inventory and finance are connected — enter data once.")
        );
        grid.addClassNames("landing-grid", "landing-grid-compact");

        return section(
                "Nima uchun Uz-mark?", "Почему Uz-mark?", "Why Uz-mark?",
                "Kichik biznesdan yirik korxonagacha", "От малого бизнеса до крупных компаний", "From small businesses to large enterprises",
                grid);
    }

    // ================================================================ chaqiriq bloki va footer

    private Div buildCta() {
        H2 title = i18n(new H2(), "Bugun boshlang", "Начните сегодня", "Start today");
        title.addClassName("landing-cta-title");

        Paragraph text = i18n(new Paragraph(),
                "Kompaniyangizni ro'yxatdan o'tkazing — 14 kunlik sinov muddati bilan.",
                "Зарегистрируйте компанию — с 14-дневным пробным периодом.",
                "Register your company — with a 14-day trial.");
        text.addClassName("landing-cta-text");

        RouterLink register = i18n(new RouterLink(RegisterView.class),
                "Ro'yxatdan o'tish", "Зарегистрироваться", "Create account");
        register.addClassNames("landing-btn", "landing-btn-primary");

        Div cta = new Div(title, text, register);
        cta.addClassName("landing-cta");
        return cta;
    }

    private Div buildFooter() {
        Span text = new Span();
        refreshers.add(() -> text.setText("© " + Year.now().getValue() + " Uz-mark ERP. "
                + t("Barcha huquqlar himoyalangan.", "Все права защищены.", "All rights reserved.")));

        Div footer = new Div(text);
        footer.addClassName("landing-footer");
        return footer;
    }

    // ================================================================ yordamchilar

    private Div section(String tuz, String tru, String ten,
                        String suz, String sru, String sen,
                        Div content) {
        H2 title = i18n(new H2(), tuz, tru, ten);
        title.addClassName("landing-section-title");
        Paragraph subtitle = i18n(new Paragraph(), suz, sru, sen);
        subtitle.addClassName("landing-section-subtitle");

        Div head = new Div(title, subtitle);
        head.addClassName("landing-section-head");

        Div section = new Div(head, content);
        section.addClassName("landing-section");
        return section;
    }

    private Div card(VaadinIcon vaadinIcon,
                     String tuz, String tru, String ten,
                     String duz, String dru, String den) {
        Icon icon = vaadinIcon.create();
        icon.addClassName("landing-card-icon-svg");
        Div iconBox = new Div(icon);
        iconBox.addClassName("landing-card-icon");

        H3 title = i18n(new H3(), tuz, tru, ten);
        title.addClassName("landing-card-title");

        Paragraph text = i18n(new Paragraph(), duz, dru, den);
        text.addClassName("landing-card-text");

        Div card = new Div(iconBox, title, text);
        card.addClassName("landing-card");
        return card;
    }

    private Div soonCard(VaadinIcon icon,
                         String tuz, String tru, String ten,
                         String duz, String dru, String den) {
        Div card = card(icon, tuz, tru, ten, duz, dru, den);
        card.addClassName("landing-card-soon");
        return card;
    }

    private Span chip(String uz, String ru, String en) {
        Span chip = i18n(new Span(), uz, ru, en);
        chip.addClassName("landing-chip");
        return chip;
    }

    private static Div ring(String modifier) {
        Div ring = new Div();
        ring.addClassNames("landing-ring", modifier);
        ring.getElement().setAttribute("aria-hidden", "true");
        return ring;
    }

    /** Komponentga uch tilli matn biriktiradi; til o'zgarganda avtomatik yangilanadi. */
    private <T extends Component & HasText> T i18n(T component, String uz, String ru, String en) {
        refreshers.add(() -> component.setText(t(uz, ru, en)));
        return component;
    }

    private String t(String uz, String ru, String en) {
        return switch (getLocale().getLanguage()) {
            case "ru" -> ru;
            case "en" -> en;
            default -> uz;
        };
    }

    private void refreshTexts() {
        refreshers.forEach(Runnable::run);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        refreshTexts();
    }
}