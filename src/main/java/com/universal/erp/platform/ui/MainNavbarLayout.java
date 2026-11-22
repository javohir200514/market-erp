package com.universal.erp.platform.ui;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;

/**
 * Ochiq sahifalar (Home, Login, Register) uchun umumiy navbar.
 *
 * <pre>
 * &#64;Route(value = "register", layout = MainNavbarLayout.class)
 * </pre>
 *
 * Havolalar RouterLink: Vaadin router orqali sahifa almashadi (sahifa qayta yuklanmaydi).
 * Til VaadinSession'da saqlanadi, sahifalar orasida o'tganda ham o'zgarmaydi.
 */
@AnonymousAllowed   // layout ham ochiq bo'lishi shart, aks holda Vaadin uni ishlatadigan ochiq view'larni (Register, Home) rad etadi
@CssImport("./styles/navbar.css")
public class MainNavbarLayout extends Div implements RouterLayout, AfterNavigationObserver {

    private static final String LANG_ATTR = "main.lang";
    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private final String lang = currentLang();
    private final RouterLink loginBtn = new RouterLink(LoginView.class);
    private final RouterLink registerBtn = new RouterLink(RegisterView.class);

    public MainNavbarLayout() {
        addClassName("site-layout");
        add(navbar());
    }

    // ---------------------------------------------------------------- til

    public static String currentLang() {
        VaadinSession session = VaadinSession.getCurrent();
        Object value = session == null ? null : session.getAttribute(LANG_ATTR);
        if (value instanceof String s) {
            return s;
        }
        String fromLocale = session == null || session.getLocale() == null
                ? "" : session.getLocale().getLanguage();
        return switch (fromLocale) {
            case "ru", "en" -> fromLocale;
            default -> "uz";
        };
    }

    public static void changeLang(String code) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(LANG_ATTR, code);
            session.setLocale(Locale.forLanguageTag(code));
        }
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.setLocale(Locale.forLanguageTag(code));
            ui.getPage().reload();
        }
    }

    public static String fullName(String code) {
        return switch (code) {
            case "ru" -> "Русский";
            case "en" -> "English";
            default -> "O'zbekcha";
        };
    }

    private String t(String key) {
        return HomeTexts.get(key, lang);
    }

    // ---------------------------------------------------------------- navbar

    private Component navbar() {
        Nav links = new Nav(
                sectionLink(t("nav.features"), "/#features"),
                sectionLink(t("nav.process"), "/#process"),
                sectionLink(t("nav.platform"), "/#platform"),
                sectionLink(t("nav.start"), "/#start"));
        links.addClassName("site-nav-links");

        Div inner = new Div(logo(), links, langs(), auth());
        inner.addClassName("site-nav-inner");

        Header nav = new Header(inner);
        nav.addClassName("site-nav");
        return nav;
    }

    /** Bosh sahifadagi bo'limga havola; boshqa sahifadan bosilsa ham Home ochilib, bo'limga o'tadi. */
    private static Anchor sectionLink(String text, String href) {
        Anchor a = new Anchor(href, text);
        a.addClassName("site-nav-link");
        return a;
    }


    private Component logo() {
        Span dokon = new Span("DOKON");
        dokon.addClassName("brand-name-main");

        Span ix = new Span("IX");
        ix.addClassName("brand-name-accent");

        RouterLink logo = new RouterLink(HomeView.class);
        logo.add(dokon, ix);
        logo.addClassName("site-nav-logo");

        logo.getElement().setAttribute("aria-label", "DOKONIX");
        logo.getElement().setAttribute("title", "DOKONIX");

        return logo;
    }


    /** Til tanlagich: bitta tugma (joriy til), bosilganda UZ / RU / EN ro'yxati ochiladi. */
    private Component langs() {
        Span currentFlag = new Span();
        currentFlag.addClassNames("site-flag", "site-flag-" + lang);
        Span currentCode = new Span(lang.toUpperCase(Locale.ROOT));
        currentCode.addClassName("site-nav-lang-code");
        Span caret = new Span();
        caret.addClassName("site-nav-caret");

        NativeButton current = new NativeButton();
        current.add(currentFlag, currentCode, caret);
        current.addClassName("site-nav-lang-current");
        current.getElement().setAttribute("aria-haspopup", "true");
        current.getElement().setAttribute("aria-expanded", "false");
        current.getElement().setAttribute("aria-label", t("lang.label") + ": " + fullName(lang));
        current.getElement().setAttribute("title", fullName(lang));

        Div menu = new Div();
        menu.addClassName("site-nav-lang-menu");
        for (String code : new String[] {"uz", "ru", "en"}) {
            Span flag = new Span();
            flag.addClassNames("site-flag", "site-flag-" + code);
            NativeButton option = new NativeButton();
            option.add(flag, new Span(fullName(code)));
            option.addClassName("site-nav-lang-option");
            boolean active = code.equals(lang);
            if (active) {
                option.addClassName("is-active");
            }
            option.getElement().setAttribute("aria-pressed", String.valueOf(active));
            option.addClickListener(e -> changeLang(code));
            menu.add(option);
        }

        Div wrap = new Div(current, menu);
        wrap.addClassName("site-nav-langdrop");
        return wrap;
    }

    /** Menyuni ochish/yopish brauzer tomonida: tugma bosilsa ochiladi, tashqarida bosilsa yoki Esc bosilsa yopiladi. */
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        getElement().executeJs("""
                if (window.__siteLangInit) { return; }
                window.__siteLangInit = true;
                const closeAll = (except) => {
                  document.querySelectorAll('.site-nav-langdrop.is-open').forEach((d) => {
                    if (d !== except) {
                      d.classList.remove('is-open');
                      const b = d.querySelector('.site-nav-lang-current');
                      if (b) { b.setAttribute('aria-expanded', 'false'); }
                    }
                  });
                };
                document.addEventListener('click', (ev) => {
                  const cur = ev.target.closest ? ev.target.closest('.site-nav-lang-current') : null;
                  const drop = cur ? cur.closest('.site-nav-langdrop') : null;
                  closeAll(drop);
                  if (drop) {
                    drop.classList.toggle('is-open');
                    cur.setAttribute('aria-expanded', String(drop.classList.contains('is-open')));
                  }
                });
                document.addEventListener('keydown', (ev) => {
                  if (ev.key === 'Escape') { closeAll(null); }
                });
                """);
    }

    private Component auth() {
        loginBtn.setText(t("nav.login"));
        loginBtn.addClassNames("site-nav-btn", "site-nav-btn-ghost");
        registerBtn.setText(t("nav.register"));
        registerBtn.addClassNames("site-nav-btn", "site-nav-btn-primary");
        Div auth = new Div(loginBtn, registerBtn);
        auth.addClassName("site-nav-auth");
        return auth;
    }

    /** Joriy sahifa tugmasi aria-current="page" oladi (CSS uni ajratib ko'rsatadi). */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        mark(loginBtn, LOGIN.equals(path));
        mark(registerBtn, REGISTER.equals(path));
    }

    private static void mark(RouterLink a, boolean current) {
        if (current) {
            a.getElement().setAttribute("aria-current", "page");
        } else {
            a.getElement().removeAttribute("aria-current");
        }
    }
}