package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.RolePolicy;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinRequest;
import com.universal.erp.platform.security.RoleChecks;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ilova qobig'i (app shell): chap menyu (Bosh sahifa + faqat tanlangan modullar va ularning bo'limlari),
 * yuqorida kompaniya, qidiruv, til va profil. Barcha ichki sahifalar shu maket ichida ochiladi:
 *
 *   @Route(value = "app/...", layout = MainLayout.class)
 *
 * O'zgarishlar:
 *  - Profil tugmasida bosh harflar (KG) o'rniga avatar ikonkasi.
 *  - Telefonda kompaniya nomi dumaloq nuqtaga aylanib qolmaydi (shell-fix.css).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/MainLayout.java
 */
@CssImport("./styles/login.css")
@CssImport("./styles/app-shell.css")
@CssImport("./styles/shell-fix.css")
@PermitAll
public class MainLayout extends Div implements RouterLayout, AfterNavigationObserver, BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

    /** Avatar ikonkasi (odam siluet). viewBox'ni onAttach ichidagi kuzatuvchi o'rnatadi (class=home-svg). */
    private static final String AVATAR_SVG = "<svg class='home-svg sh-avatar-ico' fill='none' stroke='currentColor' "
            + "stroke-width='1.9' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true' focusable='false'>"
            + "<path d='M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z'/>"
            + "<path d='M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1'/></svg>";

    private final String companyName;
    private final Set<AppModule> enabledModules;
    private final WarehouseProfile warehouse;
    private final CatalogTerms.Terms catalog;

    private final Div contentHost = new Div();
    private final Map<String, Anchor> links = new LinkedHashMap<>();
    private final Map<AppModule, Div> groups = new LinkedHashMap<>();

    public MainLayout(TenantModules tenantModules, CompanyBusinessTypes businessTypes) {
        String c = CurrentCompany.get();
        this.companyName = c.isEmpty() ? "—" : c;
        this.enabledModules = EnumSet.noneOf(AppModule.class);
        this.warehouse = WarehouseProfile.of(businessTypes.find(c));
        this.catalog = CatalogTerms.of(businessTypes.find(c), AppTexts.lang());
        var chosen = tenantModules.enabled(c);
        for (AppModule m : chosen) {
            if (RolePolicy.canView(m)) {
                this.enabledModules.add(m);
            }
        }
        log.info("Kompaniya '{}': TANLANGAN modullar={}, ROL bo'yicha ochiq={}, rollar={}",
                c, chosen, enabledModules, RoleChecks.currentRoles());

        addClassName("sh-root");
        contentHost.addClassName("sh-content");

        Div scrim = new Div();
        scrim.addClassName("sh-scrim");
        scrim.addClickListener(e -> removeClassName("is-nav-open"));

        Div main = new Div(topbar(), contentHost);
        main.addClassName("sh-main");

        add(sidebar(), scrim, main);
    }

    /**
     * Kompaniya sessiyada yo'q bo'lsa (eski kirish), qayta kirish sahifasiga yuboramiz,
     * shunda kompaniya nomi va tanlangan modullar to'g'ri yuklanadi.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (CurrentCompany.get().isEmpty()) {
            event.forwardTo(LoginView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UiKit.installDropdowns(this);
        // Yorug' mavzu: qorong'i mavzu qolib ketgan bo'lsa, olib tashlaymiz
        event.getUI().getPage().executeJs("document.documentElement.removeAttribute('theme')");
        // Ikonkalar 24x24 koordinatada chizilgan; viewBox bo'lmasa kichraytirilganda o'ng/pastki qismi kesilib qoladi
        event.getUI().getPage().executeJs(
                "const fix = () => document.querySelectorAll('svg.home-svg:not([viewBox])')"
                        + ".forEach(s => s.setAttribute('viewBox', '0 0 24 24'));"
                        + "fix();"
                        + "if (!window.__uzSvgFix) {"
                        + "  window.__uzSvgFix = new MutationObserver(fix);"
                        + "  window.__uzSvgFix.observe(document.body, {childList: true, subtree: true});"
                        + "}");
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
    }

    // ================================================================ chap menyu

    private Component sidebar() {
        Nav nav = new Nav();
        nav.addClassName("sh-nav");
        nav.add(navLink("app", "home", t("shell.dashboard")));

        Span label = new Span(t("shell.modules"));
        label.addClassName("sh-nav-label");
        nav.add(label);
        for (AppModule m : AppModule.values()) {
            if (!enabledModules.contains(m)) {
                continue;
            }
            nav.add(moduleGroup(m));
        }

        Aside side = new Aside(logo(), nav);
        side.addClassName("sh-side");
        return side;
    }

    /** Brend logotipi: DOKON + IX (bosh sahifa va navbar bilan bir xil). */
    private Component logo() {
        Span dokon = new Span("DOKON");
        dokon.addClassName("brand-name-main");

        Span ix = new Span("IX");
        ix.addClassName("brand-name-accent");

        Anchor logo = new Anchor("app");
        logo.add(dokon, ix);
        logo.addClassName("sh-logo");
        logo.getElement().setAttribute("aria-label", "DOKONIX");
        logo.getElement().setAttribute("title", "DOKONIX");
        return logo;
    }

    /**
     * Modulning menyuda ko'rinadigan birinchi bo'limi marshruti.
     * Yig'ilgan menyuda modul ikonkasi bosilganda shu yerga o'tiladi.
     * Bo'lim bo'lmasa, modul sahifasining o'ziga qaytadi.
     */
    private String firstVisibleRoute(AppModule m) {
        for (AppSection s : AppSection.of(m)) {
            if (m == AppModule.INVENTORY && !warehouse.isRouteVisible(s.route())) {
                continue;
            }
            return s.route();
        }
        return m.route();
    }

    /**
     * Modul sarlavhasi (bosilganda ochiladi/yopiladi) + ichki bo'limlar. Bir vaqtda bittasi ochiq.
     * POS ham endi oddiy bo'lim sifatida {@link AppSection#of(AppModule)} orqali keladi.
     */
    private Component moduleGroup(AppModule m) {
        NativeButton head = new NativeButton();
        head.addClassName("sh-nav-link");
        Span headText = new Span(m.title());
        headText.addClassName("sh-nav-text");
        head.add(HomeIcons.svg(m.icon()), headText);
        Span chev = new Span();
        chev.addClassName("sh-chev");
        head.add(chev);
        head.getElement().setAttribute("aria-label", m.title());
        head.getElement().setAttribute("title", m.title());

        Div inner = new Div();
        inner.addClassName("sh-sub-inner");
        // "Umumiy ko'rinish" havolasi olib tashlandi: faqat modul bo'limlari qoldi.
        for (AppSection s : AppSection.of(m)) {
            if (m == AppModule.INVENTORY && !warehouse.isRouteVisible(s.route())) {
                continue;
            }
            String label = m == AppModule.INVENTORY && s.route().endsWith("/products") ? catalog.title() : s.title();
            inner.add(subLink(s.route(), label));
        }
        Div sub = new Div(inner);
        sub.addClassName("sh-sub");

        Div group = new Div(head, sub);
        group.addClassName("sh-group");
        groups.put(m, group);

        head.addClickListener(e -> {
            if (hasClassName("is-collapsed")) {
                // yig'ilgan (faqat ikonka) holatda ichki ro'yxat ko'rinmaydi — birinchi bo'limga o'tamiz
                UI.getCurrent().navigate(firstVisibleRoute(m));
                return;
            }
            boolean open = !group.hasClassName("is-open");
            groups.values().forEach(g -> g.removeClassName("is-open"));
            group.setClassName("is-open", open);
        });
        return group;
    }

    private Anchor subLink(String route, String text) {
        Anchor a = new Anchor(route, text);
        a.addClassName("sh-sublink");
        links.put(route, a);
        return a;
    }

    private Anchor navLink(String route, String icon, String text) {
        Anchor a = new Anchor(route);
        Span label = new Span(text);
        label.addClassName("sh-nav-text");
        a.add(HomeIcons.svg(icon), label);
        a.getElement().setAttribute("title", text);
        a.addClassName("sh-nav-link");
        links.put(route, a);
        return a;
    }

    // ================================================================ yuqori panel

    private Component topbar() {
        NativeButton menuBtn = new NativeButton();
        menuBtn.add(HomeIcons.svg("menu"));
        menuBtn.addClassNames("sh-icon-btn", "sh-menu-btn");
        menuBtn.getElement().setAttribute("aria-label", t("shell.menu"));
        // telefonda — menyu paneli ochiladi/yopiladi; kompyuterda — chap menyu yig'iladi/ochiladi
        menuBtn.addClickListener(e -> UI.getCurrent().getPage()
                .executeJs("return window.matchMedia('(max-width: 1000px)').matches;")
                .then(Boolean.class, mobile -> {
                    if (Boolean.TRUE.equals(mobile)) {
                        setClassName("is-nav-open", !hasClassName("is-nav-open"));
                    } else {
                        setClassName("is-collapsed", !hasClassName("is-collapsed"));
                    }
                }));

        Input input = new Input();
        input.setType("search");
        input.setPlaceholder(t("shell.search"));
        input.getElement().setAttribute("aria-label", t("shell.search"));
        Div search = new Div(HomeIcons.svg("search"), input);
        search.addClassName("sh-search");

        Div spacer = new Div();
        spacer.addClassName("sh-spacer");

        Header top = new Header();
        top.addClassName("sh-top");
        top.add(menuBtn, companyDropdown(), search, spacer,
                UiKit.langDropdown(() -> UI.getCurrent().getPage().reload()), profileDropdown());
        return top;
    }

    /** TODO: bir nechta kompaniya bo'lsa, ro'yxatni servisdan oling va tanlanganda kontekstni almashtiring. */
    private Component companyDropdown() {
        Span dot = new Span();
        dot.addClassName("sh-company-dot");
        Span name = new Span(companyName);
        name.addClassName("sh-company-name");
        name.getElement().setAttribute("title", companyName);
        Span content = new Span(dot, name, UiKit.caret());
        content.addClassName("ui-btn-content");

        Span label = new Span(t("shell.company"));
        label.addClassName("ui-drop-label");

        Span itemDot = new Span();
        itemDot.addClassName("sh-company-dot");
        Div item = new Div(itemDot, new Span(companyName));
        item.addClassNames("ui-drop-item", "is-active");

        Div drop = UiKit.dropdown(content, t("shell.company"), "sh-company-btn", label, item);
        drop.addClassName("sh-company");
        return drop;
    }

    private Component profileDropdown() {
        Principal p = VaadinRequest.getCurrent() == null ? null : VaadinRequest.getCurrent().getUserPrincipal();
        String name = p != null ? p.getName() : t("shell.user");

        // Bosh harflar (KG) o'rniga avatar ikonkasi
        Span avatar = new Span();
        avatar.addClassName("sh-avatar");
        avatar.getElement().appendChild(new Html(AVATAR_SVG).getElement());
        avatar.getElement().setAttribute("aria-hidden", "true");
        Span shown = new Span(name);
        shown.addClassName("sh-profile-name");
        Span content = new Span(avatar, shown, UiKit.caret());
        content.addClassName("ui-btn-content");

        Span headName = new Span(name);
        headName.addClassName("ui-drop-head-name");
        Div head = new Div(headName);
        head.addClassName("ui-drop-head");

        NativeButton logout = new NativeButton(t("shell.logout"));
        logout.addClassNames("ui-drop-item", "is-danger");
        logout.addClickListener(e -> logout());

        Div drop;
        if (RolePolicy.isAdmin()) {
            NativeButton users = new NativeButton(t("u.menu"));
            users.addClassName("ui-drop-item");
            users.addClickListener(e -> UI.getCurrent().navigate("settings/users"));
            NativeButton settings = new NativeButton(t("set.menu"));
            settings.addClassName("ui-drop-item");
            settings.addClickListener(e -> UI.getCurrent().navigate("settings/modules"));
            drop = UiKit.dropdown(content, t("shell.profile"), "sh-profile-btn", head, users, settings, logout);
        } else {
            drop = UiKit.dropdown(content, t("shell.profile"), "sh-profile-btn", head, logout);
        }
        drop.addClassName("sh-profile");
        return drop;
    }

    /** Sessiyani yopib, bosh sahifaga qaytaradi. Spring Security'ning o'z logout oqimi bo'lsa, shunga almashtiring. */
    private void logout() {
        UI ui = UI.getCurrent();
        ui.getPage().setLocation("/");
        ui.getSession().getSession().invalidate();
        ui.getSession().close();
    }

    // ================================================================ router

    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentHost.getElement().removeAllChildren();
        if (content != null) {
            contentHost.getElement().appendChild(content.getElement());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        links.forEach((route, a) -> a.setClassName("is-active", route.equals(path)));
        groups.forEach((m, g) -> {
            boolean current = path.equals(m.route()) || path.startsWith(m.route() + "/");
            g.setClassName("is-open", current);
            g.setClassName("is-current", current);
        });
        removeClassName("is-nav-open");
    }
}