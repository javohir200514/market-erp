package com.universal.erp.platform.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;

/**
 * DOKONIX — bosh sahifa (do'konlar tarmog'i uchun, premium yorug' mavzu).
 * Bo'limlar: hero → ishonch qatori → oldin/keyin → imkoniyatlar → natija → jarayon → platforma → boshlash → CTA → footer.
 * Skroll animatsiyasi: [data-reveal] elementlari ko'ringanda .is-in oladi (home.css).
 * Qo'shimcha effektlar (JS): kartalarda kursor yorug'ligi (--mx/--my) va hero parallaksi (--px/--py).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/HomeView.java
 */
@Route(value = "", layout = MainNavbarLayout.class)
@PageTitle("DOKONIX")
@AnonymousAllowed
@CssImport("./styles/login.css")   // bayroqlar (.login-flag-*) shu fayldan olinadi
@CssImport("./styles/home.css")
public class HomeView extends Div {

    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private static final String[] MODULE_ICONS = {"pos", "layers", "inventory", "building", "bi"};
    private static final String[] FLOW_ICONS = {"file", "inventory", "card", "pos", "receipt", "bi"};
    private static final String[] PLATFORM_ICONS = {"key", "building", "history", "shield", "plug", "globe"};
    private static final String[] START_ICONS = {"user-plus", "layers", "send"};
    private static final String[] TRUST_ICONS = {"building", "plug", "shield"};
    private static final String[] OUTCOME_ICONS = {"history", "inventory", "pos"};

    private final String lang = MainNavbarLayout.currentLang();

    public HomeView() {
        addClassName("home-page");
        render();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        getElement().executeJs("""
                const root = $0;
                if (root.__homeFx) { root.__homeFx(); root.__homeFx = null; }
                const cleanups = [];

                // Html komponenti SVG'ning viewBox atributini kichik harfga aylantirib yuboradi — qaytarib qo'yamiz
                root.querySelectorAll('svg').forEach((s) => {
                  if (!s.getAttribute('viewBox')) { s.setAttribute('viewBox', s.dataset.vb || '0 0 24 24'); }
                  if (s.dataset.par) { s.setAttribute('preserveAspectRatio', s.dataset.par); }
                });

                // #anchor bo'yicha kelganda tegishli bo'limga o'tish
                const h = location.hash;
                if (h && h.length > 1) {
                  setTimeout(() => {
                    const el = document.getElementById(decodeURIComponent(h.substring(1)));
                    if (el) { el.scrollIntoView(); }
                  }, 60);
                }

                // Kartalarda kursor ortidan yuruvchi yorug'lik
                const onMove = (ev) => {
                  const t = ev.target;
                  const card = t && t.closest ? t.closest('.home-module, .home-outcome, .home-start-item') : null;
                  if (!card) { return; }
                  const r = card.getBoundingClientRect();
                  card.style.setProperty('--mx', (ev.clientX - r.left) + 'px');
                  card.style.setProperty('--my', (ev.clientY - r.top) + 'px');
                };
                root.addEventListener('pointermove', onMove, { passive: true });
                cleanups.push(() => root.removeEventListener('pointermove', onMove));

                const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

                // Hero parallaksi (faqat sichqoncha bor qurilmalarda)
                const vis = root.querySelector('.home-hero-visual');
                const fine = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;
                if (vis && !reduce && fine) {
                  let raf = 0;
                  const onHero = (ev) => {
                    if (raf) { return; }
                    raf = requestAnimationFrame(() => {
                      raf = 0;
                      const x = (ev.clientX / window.innerWidth - 0.5) * 2;
                      const y = (ev.clientY / window.innerHeight - 0.5) * 2;
                      vis.style.setProperty('--px', x.toFixed(3));
                      vis.style.setProperty('--py', y.toFixed(3));
                    });
                  };
                  window.addEventListener('pointermove', onHero, { passive: true });
                  cleanups.push(() => { window.removeEventListener('pointermove', onHero); if (raf) { cancelAnimationFrame(raf); } });
                }

                // Skroll paytida paydo bo'lish
                if (!reduce && 'IntersectionObserver' in window) {
                  const io = new IntersectionObserver((entries) => {
                    entries.forEach((e) => {
                      if (e.isIntersecting) { e.target.classList.add('is-in'); io.unobserve(e.target); }
                    });
                  }, { threshold: 0.12, rootMargin: '0px 0px -6% 0px' });

                  root.classList.add('is-anim');
                  requestAnimationFrame(() => {
                    root.querySelectorAll('[data-reveal]').forEach((el) => io.observe(el));
                  });
                  cleanups.push(() => { io.disconnect(); root.classList.remove('is-anim'); });
                }

                // Raqamlar 0 dan sanab chiqiladi (hero oynasida)
                root.querySelectorAll('[data-count]').forEach((el) => {
                  const to = parseFloat(el.dataset.count);
                  const dec = parseInt(el.dataset.dec || '0', 10);
                  if (reduce || !(to > 0)) { return; }
                  const t0 = performance.now() + 500;
                  let raf = 0;
                  const step = (now) => {
                    const p = Math.max(0, Math.min(1, (now - t0) / 1400));
                    const e = 1 - Math.pow(1 - p, 3);
                    el.textContent = (to * e).toFixed(dec).replace('.', ',');
                    if (p < 1) { raf = requestAnimationFrame(step); }
                  };
                  raf = requestAnimationFrame(step);
                  cleanups.push(() => cancelAnimationFrame(raf));
                });

                root.__homeFx = () => cleanups.forEach((fn) => fn());
                """, getElement());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        getElement().executeJs("if ($0.__homeFx) { $0.__homeFx(); $0.__homeFx = null; }", getElement());
        super.onDetach(event);
    }

    private String t(String key) {
        return HomeTexts.get(key, lang);
    }

    /** Kichik, faqat shu sahifaga tegishli yozuvlar (HomeTexts ga yangi kalit qo'shmaslik uchun). */
    private String tr(String uz, String ru, String en) {
        return "ru".equals(lang) ? ru : "en".equals(lang) ? en : uz;
    }

    /** Marketing matnlari. */
    private String c(String key) {
        return HomeCopy.get(key, lang);
    }

    private void render() {
        removeAll();
        add(hero(), trustBand(), shift(), modules(), outcomes(), flow(), platform(), start(), cta(), footer());
    }

    // ================================================================ yordamchilar

    private <T extends Component> T reveal(T component, String kind, int delayMs) {
        component.getElement().setAttribute("data-reveal", kind);
        if (delayMs > 0) {
            component.getElement().getStyle().set("--d", delayMs + "ms");
        }
        return component;
    }

    private Anchor navLink(String text, String href) {
        return new Anchor(href, text);
    }

    private Component langs() {
        Div wrap = new Div();
        for (String code : new String[] {"uz", "ru", "en"}) {
            wrap.add(langButton(code, MainNavbarLayout.fullName(code), "home-lang-btn"));
        }
        wrap.addClassName("home-langs");
        wrap.getElement().setAttribute("role", "group");
        wrap.getElement().setAttribute("aria-label", t("lang.label"));
        return wrap;
    }

    private NativeButton langButton(String code, String label, String cssClass) {
        Span flag = new Span();
        flag.addClassNames("login-flag", "login-flag-" + code);
        Span text = new Span(label);
        text.addClassName("home-lang-label");
        NativeButton b = new NativeButton();
        b.add(flag, text);
        b.addClassName(cssClass);
        boolean active = code.equals(lang);
        if (active) {
            b.addClassName("active-lang");
        }
        b.getElement().setAttribute("aria-pressed", String.valueOf(active));
        b.getElement().setAttribute("aria-label", MainNavbarLayout.fullName(code));
        b.getElement().setAttribute("title", MainNavbarLayout.fullName(code));
        b.addClickListener(e -> MainNavbarLayout.changeLang(code));
        return b;
    }

    private Anchor link(String text, String href, String classes) {
        Anchor a = new Anchor(href, text);
        a.addClassNames(classes.split(" "));
        return a;
    }

    private Anchor iconLink(String text, String href, String classes) {
        Anchor a = new Anchor(href);
        a.add(new Span(text), HomeIcons.svg("arrow"));
        a.addClassNames(classes.split(" "));
        return a;
    }

    private Component iconTile(String icon, String extraClass) {
        Span tile = new Span(HomeIcons.svg(icon));
        tile.addClassName("home-icon");
        if (extraClass != null) {
            tile.addClassName(extraClass);
        }
        return tile;
    }

    private Span eyebrow(String text) {
        Span e = new Span(text);
        e.addClassName("home-eyebrow");
        return e;
    }

    private Section section(String id, String eyebrow, String title, String sub) {
        Section s = new Section();
        s.addClassName("home-section");
        s.setId(id);
        H2 h = new H2(title);
        h.addClassName("home-section-title");
        Div left = new Div(eyebrow(eyebrow), h);
        left.addClassName("home-section-headline");
        Paragraph p = new Paragraph(sub);
        p.addClassName("home-section-sub");
        Div head = new Div(left, p);
        head.addClassName("home-section-head");
        reveal(head, "up", 0);
        s.add(head);
        return s;
    }

    /** Ro'yxat bandi: kichik ko'k romb belgisi (ptichka emas). */
    private ListItem checkItem(String text) {
        Span mark = new Span();
        mark.addClassName("home-mark");
        mark.getElement().setAttribute("aria-hidden", "true");
        return new ListItem(mark, new Span(text));
    }

    // ================================================================ hero

    private Component hero() {
        H1 title = new H1(c("hero.headline"));
        title.addClassName("home-hero-title");
        Paragraph sub = new Paragraph(t("hero.sub"));
        sub.addClassName("home-hero-sub");

        Div buttons = new Div(
                iconLink(t("hero.cta1"), REGISTER, "home-btn home-btn-solid home-btn-lg"),
                link(t("hero.cta2"), LOGIN, "home-btn home-btn-ghost home-btn-lg"));
        buttons.addClassName("home-hero-actions");

        Span liveDot = new Span();
        liveDot.addClassName("home-live-dot");
        liveDot.getElement().setAttribute("aria-hidden", "true");
        Div note = new Div(liveDot, new Span(c("hero.note")));
        note.addClassName("home-hero-note");

        Div copy = new Div(title, sub, buttons, note);
        copy.addClassName("home-hero-copy");

        Div visual = new Div(
                console(),
                phone(),
                chip("pos", c("chip.1.t"), c("chip.1.d"), "is-a"),
                chip("inventory", c("chip.2.t"), c("chip.2.d"), "is-b"));
        visual.addClassName("home-hero-visual");

        Div grid = new Div(copy, visual);
        grid.addClassName("home-hero");

        Section wrap = new Section(grid);
        wrap.addClassName("home-hero-wrap");
        return wrap;
    }

    private static final String CHART_LINE = "M0 96 C28 92 48 74 78 78 C108 82 128 54 164 50 "
            + "C200 46 216 68 252 60 C288 52 304 26 338 24 C364 22 382 32 400 14";

    private static Div box(String cls, Component... children) {
        Div d = new Div(children);
        if (!cls.isEmpty()) {
            d.addClassNames(cls.split(" "));
        }
        return d;
    }

    private static Span txt(String cls, String text) {
        Span sp = new Span(text);
        if (!cls.isEmpty()) {
            sp.addClassNames(cls.split(" "));
        }
        return sp;
    }

    /** Dastur oynasi: DOKONIX boshqaruv paneli (namunaviy ma'lumot bilan). */
    private Component console() {
        String[][] items = {
                {"layers", tr("Bosh sahifa", "Главная", "Overview")},
                {"receipt", tr("Moliya", "Финансы", "Finance")},
                {"user", "CRM"},
                {"inventory", tr("Ombor", "Склад", "Stock")},
                {"shield", tr("Kadrlar", "Кадры", "HR")}};
        Div side = box("home-app-side", txt("home-app-logo", "DOKONIX"));
        for (int i = 0; i < items.length; i++) {
            Div item = box("home-app-nav", HomeIcons.svg(items[i][0]), txt("", items[i][1]));
            if (i == 0) {
                item.addClassName("is-active");
            }
            side.add(item);
        }

        Div top = box("home-app-top",
                txt("home-app-search", tr("Qidirish...", "Поиск...", "Search...")),
                txt("home-app-branch", tr("Chilonzor", "Чиланзар", "Chilanzar")),
                txt("home-app-avatar", "DK"));
        Div hello = box("home-app-hello",
                txt("home-app-h", tr("Xayrli kun, Dokon", "Добрый день, Магазин", "Good afternoon, Store")),
                txt("home-app-date", tr("Bugungi ko'rsatkichlar", "Показатели за сегодня", "Today's overview")));

        String[][] kpi = {
                {tr("Tushum · 7 kun", "Выручка · 7 дн.", "Revenue · 7d"), "31,4 " + t("mock.mln"), "▲ 12%", "up"},
                {tr("Cheklar", "Чеки", "Receipts"), "128", "▲ 8%", "up"},
                {tr("Kam qolgan", "Мало осталось", "Low stock"), "6", tr("tovar", "товаров", "items"), "warn"}};
        Div kpis = box("home-app-kpis");
        for (String[] k : kpi) {
            kpis.add(box("home-app-kpi", txt("home-app-kpi-l", k[0]), txt("home-app-kpi-v", k[1]),
                    txt("home-app-kpi-d is-" + k[3], k[2])));
        }

        Div chart = new Div();
        chart.addClassName("home-win-chart");
        chart.getElement().appendChild(new Html("<svg class='home-win-svg' data-vb='0 0 400 120' data-par='none' "
                + "aria-hidden='true'><path class='home-win-area' d='" + CHART_LINE + " L400 120 L0 120 Z'/>"
                + "<path class='home-win-line' d='" + CHART_LINE + "'/></svg>").getElement());
        chart.add(txt("home-win-pulse", ""));
        Div panel = box("home-app-panel",
                box("home-app-panel-head",
                        txt("home-app-panel-t", tr("Tushum dinamikasi", "Динамика выручки", "Revenue trend")),
                        box("home-app-seg", txt("is-on", tr("7 kun", "7 дн.", "7d")),
                                txt("", tr("30 kun", "30 дн.", "30d")))),
                chart);

        Div main = box("home-app-main", top, hello, kpis, panel);
        Div win = box("home-app", side, main);
        win.getElement().setAttribute("aria-hidden", "true");
        return win;
    }

    /** Telefon: kassa ekrani — skaner, savat va to'lov. */
    private Component phone() {
        Div scan = box("home-phone-scan", txt("home-phone-beam", ""));
        Div screen = box("home-phone-screen",
                box("home-phone-head", txt("home-phone-title", tr("Kassa", "Касса", "Register")),
                        txt("home-live-dot", "")),
                scan,
                box("home-phone-line", txt("", t("mock.p1")), txt("", "8 000")),
                box("home-phone-line", txt("", t("mock.p2")), txt("", "12 000")),
                box("home-phone-line", txt("", t("mock.p3")), txt("", "11 000")),
                box("home-phone-total", txt("", t("mock.total")), txt("", "31 000")),
                txt("home-phone-pay", tr("To'lash", "Оплатить", "Pay")));
        Div phone = box("home-phone", txt("home-phone-notch", ""), screen);
        phone.getElement().setAttribute("aria-hidden", "true");
        return phone;
    }

    private Component chip(String icon, String title, String desc, String position) {
        Span tile = new Span(HomeIcons.svg(icon));
        tile.addClassName("home-chip-icon");
        Span name = new Span(title);
        name.addClassName("home-chip-title");
        Span text = new Span(desc);
        text.addClassName("home-chip-desc");
        Div body = new Div(name, text);
        body.addClassName("home-chip-body");
        Div chip = new Div(tile, body);
        chip.addClassNames("home-chip", position);
        chip.getElement().setAttribute("aria-hidden", "true");
        return chip;
    }

    private Component trustBand() {
        Div band = new Div();
        band.addClassName("home-trust");
        for (int i = 1; i <= 3; i++) {
            Span text = new Span(t("trust." + i));
            Div item = new Div(iconTile(TRUST_ICONS[i - 1], "home-icon-sm"), text);
            item.addClassName("home-trust-item");
            band.add(item);
        }
        reveal(band, "up", 0);
        Section s = new Section(band);
        s.addClassName("home-trust-section");
        return s;
    }

    // ================================================================ oldin / keyin

    private Component shift() {
        Section s = section("why", c("eb.why"), c("shift.title"), c("shift.sub"));

        UnorderedList before = new UnorderedList();
        UnorderedList after = new UnorderedList();
        before.addClassName("home-shift-list");
        after.addClassName("home-shift-list");
        for (int i = 1; i <= 5; i++) {
            Span x = new Span("—");
            x.addClassName("home-x");
            before.add(new ListItem(x, new Span(c("b" + i))));
            after.add(checkItem(c("a" + i)));
        }

        Span beforeLabel = new Span(c("shift.before"));
        beforeLabel.addClassName("home-shift-label");
        Div colBefore = new Div(beforeLabel, before);
        colBefore.addClassNames("home-shift-col", "is-before");

        Span afterLabel = new Span(c("shift.after"));
        afterLabel.addClassName("home-shift-label");
        Div colAfter = new Div(afterLabel, after);
        colAfter.addClassNames("home-shift-col", "is-after");

        Div wrap = new Div(reveal(colBefore, "left", 0), reveal(colAfter, "up", 140));
        wrap.addClassName("home-shift");
        s.add(wrap);
        return s;
    }

    // ================================================================ imkoniyatlar

    private Component modules() {
        Section s = section("features", c("eb.features"), t("mod.title"), t("mod.sub"));
        Div grid = new Div();
        grid.addClassName("home-modules");
        for (int i = 1; i <= 5; i++) {
            UnorderedList list = new UnorderedList();
            list.addClassName("home-checklist");
            for (int b = 1; b <= 4; b++) {
                list.add(checkItem(t("m" + i + ".b" + b)));
            }
            Span num = new Span(String.format(Locale.ROOT, "%02d", i));
            num.addClassName("home-module-num");
            Div top = new Div(num, iconTile(MODULE_ICONS[i - 1], null));
            top.addClassName("home-module-top");

            Div body = new Div(new H3(t("m" + i + ".t")), new Paragraph(t("m" + i + ".d")));
            body.addClassName("home-module-body");

            Div row = new Div(top, body, list);
            row.addClassName("home-module");
            grid.add(reveal(row, "up", ((i - 1) % 2) * 90));
        }
        s.add(grid);
        return s;
    }

    // ================================================================ natija

    private Component outcomes() {
        Section s = section("outcome", c("eb.outcome"), c("out.title"), c("out.sub"));
        Div grid = new Div();
        grid.addClassName("home-outcomes");
        for (int i = 1; i <= 3; i++) {
            Span idx = new Span(String.format(Locale.ROOT, "0%d", i));
            idx.addClassName("home-outcome-idx");
            Div top = new Div(idx, iconTile(OUTCOME_ICONS[i - 1], "home-icon-sm"));
            top.addClassName("home-outcome-top");
            Div item = new Div(top, new H3(c("out." + i + ".t")), new Paragraph(c("out." + i + ".d")));
            item.addClassName("home-outcome");
            grid.add(reveal(item, "up", (i - 1) * 110));
        }
        s.add(grid);
        return s;
    }

    // ================================================================ savdo jarayoni

    private Component flow() {
        Section s = section("process", c("eb.process"), t("flow.title"), t("flow.sub"));
        Div steps = new Div();
        steps.addClassName("home-flow");

        Div line = new Div();
        line.addClassName("home-flow-line");
        steps.add(line);

        for (int i = 1; i <= 6; i++) {
            Span num = new Span(String.valueOf(i));
            num.addClassName("home-step-num");
            Div step = new Div(num, iconTile(FLOW_ICONS[i - 1], "home-icon-sm"),
                    new H3(t("f" + i + ".t")), new Paragraph(t("f" + i + ".d")));
            step.addClassName("home-step");
            steps.add(reveal(step, "up", (i - 1) * 90));
        }
        s.add(reveal(steps, "line", 0));
        return s;
    }

    // ================================================================ platforma

    private Component platform() {
        Section s = section("platform", c("eb.platform"), t("plat.title"), t("plat.sub"));
        Div grid = new Div();
        grid.addClassName("home-platform");
        for (int i = 1; i <= 6; i++) {
            Div text = new Div(new H3(t("p" + i + ".t")), new Paragraph(t("p" + i + ".d")));
            text.addClassName("home-platform-text");
            Div item = new Div(iconTile(PLATFORM_ICONS[i - 1], "home-icon-sm"), text);
            item.addClassName("home-platform-item");
            grid.add(reveal(item, "up", ((i - 1) % 3) * 90));
        }
        s.add(grid);
        return s;
    }

    // ================================================================ boshlash

    private Component start() {
        Section s = section("start", c("eb.start"), t("start.title"), t("start.sub"));
        Div grid = new Div();
        grid.addClassName("home-start");
        for (int i = 1; i <= 3; i++) {
            Span big = new Span(String.format(Locale.ROOT, "0%d", i));
            big.addClassName("home-start-num");
            Div top = new Div(big, iconTile(START_ICONS[i - 1], "home-icon-sm"));
            top.addClassName("home-start-top");
            Div item = new Div(top, new H3(t("s" + i + ".t")), new Paragraph(t("s" + i + ".d")));
            item.addClassName("home-start-item");
            grid.add(reveal(item, "up", (i - 1) * 110));
        }
        s.add(grid);
        return s;
    }

    // ================================================================ CTA

    private Component cta() {
        H2 h = new H2(c("cta2.title"));
        h.addClassName("home-cta-title");
        Paragraph p = new Paragraph(c("cta2.sub"));
        p.addClassName("home-cta-sub");
        Div actions = new Div(
                iconLink(t("nav.register"), REGISTER, "home-btn home-btn-solid home-btn-lg"),
                link(t("nav.login"), LOGIN, "home-btn home-btn-ghost home-btn-lg"));
        actions.addClassName("home-hero-actions");
        Span note = new Span(c("cta2.note"));
        note.addClassName("home-cta-note");

        Div inner = new Div(h, p, actions, note);
        inner.addClassName("home-cta-inner");
        Div box = new Div(inner);
        box.addClassName("home-cta");
        Section s = new Section(reveal(box, "scale", 0));
        s.addClassName("home-section");
        return s;
    }

    // ================================================================ footer

    // ================================================================ pastki navigatsiya (telefon)

    /** Telefonda ilova kabi pastda turadigan panel; kompyuterda CSS bilan yashiriladi. */
    /** Brend logotipi: DOKON + IX (navbar bilan bir xil). */
    private Component footerLogo() {
        Span dokon = new Span("DOKON");
        dokon.addClassName("home-brand-main");

        Span ix = new Span("IX");
        ix.addClassName("home-brand-accent");

        RouterLink logo = new RouterLink(HomeView.class);
        logo.add(dokon, ix);
        logo.addClassName("home-logo");
        logo.getElement().setAttribute("aria-label", "DOKONIX");
        logo.getElement().setAttribute("title", "DOKONIX");
        return logo;
    }

    private Component footer() {
        Footer f = new Footer();
        f.addClassName("home-footer");

        Paragraph tagline = new Paragraph(c("foot.tag"));
        tagline.addClassName("home-footer-tagline");
        Div brand = new Div(footerLogo(), tagline, langs());
        brand.addClassName("home-footer-brand");

        Div products = footerColumn(t("foot.product"),
                navLink(t("m1.t"), "#features"),
                navLink(t("m2.t"), "#features"),
                navLink(t("m3.t"), "#features"),
                navLink(t("m4.t"), "#features"),
                navLink(t("m5.t"), "#features"));
        Div platformCol = footerColumn(t("foot.platform"),
                navLink(t("p1.t"), "#platform"),
                navLink(t("p2.t"), "#platform"),
                navLink(t("p3.t"), "#platform"),
                navLink(t("p5.t"), "#platform"),
                navLink(t("nav.process"), "#process"));
        Div account = footerColumn(t("foot.account"),
                navLink(t("nav.login"), LOGIN),
                navLink(t("nav.register"), REGISTER),
                navLink(t("nav.start"), "#start"));

        Div grid = new Div(brand, products, platformCol, account);
        grid.addClassName("home-footer-grid");

        Div bottom = new Div(new Span("© 2026 DOKONIX. " + t("foot.rights")));
        bottom.addClassName("home-footer-bottom");

        Div inner = new Div(grid, bottom);
        inner.addClassName("home-footer-inner");
        f.add(inner);
        return f;
    }

    private Div footerColumn(String title, Anchor... links) {
        Span head = new Span(title);
        head.addClassName("home-footer-title");
        Div col = new Div(head);
        col.addClassName("home-footer-col");
        for (Anchor a : links) {
            a.addClassName("home-footer-link");
            col.add(a);
        }
        return col;
    }
}