package com.universal.erp.platform.ui;

import com.universal.erp.platform.localization.ErpI18NProvider;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


@CssImport("./styles/navbar.css")
public class PublicNavbar extends Div implements LocaleChangeObserver {

    /** Qaysi sahifada turganimiz — tegishli tugma "joriy" deb belgilanadi. */
    public enum Page {HOME, LOGIN, REGISTER}

    private static final String LANG_KEY = "erp.lang";
    private static final String LANG_RESTORED_KEY = "erp.lang.restored";
    private static final Locale[] LOCALES = {ErpI18NProvider.UZ, ErpI18NProvider.RU, ErpI18NProvider.EN};

    private final Map<String, NativeButton> languageButtons = new LinkedHashMap<>();

    private final RouterLink loginLink = new RouterLink(LoginView.class);
    private final RouterLink registerLink = new RouterLink(RegisterView.class);
    private final Anchor openAppLink = new Anchor("/");

    public PublicNavbar(Page active) {
        addClassName("site-nav");
        getElement().setAttribute("role", "navigation");

        add(buildLogo(), buildLanguages(), buildAuthActions(active));
        updateTexts();
    }

    // ================================================================ qismlar

    private RouterLink buildLogo() {
        Span mark = new Span("U");
        mark.addClassName("site-nav-logo-mark");
        Span name = new Span("Uz-mark");
        name.addClassName("site-nav-logo-text");

        RouterLink logo = new RouterLink(LandingView.class);
        logo.addClassName("site-nav-logo");
        logo.getElement().setAttribute("aria-label", "Uz-mark");
        logo.add(mark, name);
        return logo;
    }

    private Div buildLanguages() {
        Div languages = new Div();
        languages.addClassName("site-nav-langs");

        for (Locale locale : LOCALES) {
            String lang = locale.getLanguage();
            String label = switch (lang) {
                case "uz" -> "O'zbekcha";
                case "ru" -> "Русский";
                default -> "English";
            };

            Span flag = new Span();
            flag.addClassNames("site-flag", "site-flag-" + (lang.equals("uz") || lang.equals("ru") ? lang : "en"));

            NativeButton button = new NativeButton();
            button.addClassName("site-nav-lang");
            button.getElement().setAttribute("type", "button");
            button.add(flag, new Span(label));
            button.addClickListener(e -> changeLanguage(locale));

            languageButtons.put(lang, button);
            languages.add(button);
        }
        return languages;
    }

    private Div buildAuthActions(Page active) {
        Div actions = new Div();
        actions.addClassName("site-nav-auth");

        if (isAuthenticated()) {
            openAppLink.addClassNames("site-nav-btn", "site-nav-btn-primary");
            actions.add(openAppLink);
            return actions;
        }

        loginLink.addClassNames("site-nav-btn", "site-nav-btn-ghost");
        registerLink.addClassNames("site-nav-btn", "site-nav-btn-primary");

        if (active == Page.LOGIN) {
            loginLink.getElement().setAttribute("aria-current", "page");
        } else if (active == Page.REGISTER) {
            registerLink.getElement().setAttribute("aria-current", "page");
        }

        actions.add(loginLink, registerLink);
        return actions;
    }

    // ================================================================ matnlar va til

    private String t(String uz, String ru, String en) {
        return switch (getLocale().getLanguage()) {
            case "ru" -> ru;
            case "en" -> en;
            default -> uz;
        };
    }

    private void updateTexts() {
        loginLink.setText(t("Kirish", "Войти", "Sign in"));
        registerLink.setText(t("Ro'yxatdan o'tish", "Регистрация", "Sign up"));
        openAppLink.setText(t("Ilovaga o'tish", "Открыть приложение", "Open app"));

        String lang = getLocale().getLanguage();
        languageButtons.forEach((code, button) -> {
            if (code.equals(lang)) {
                button.addClassName("is-active");
                button.getElement().setAttribute("aria-pressed", "true");
            } else {
                button.removeClassName("is-active");
                button.getElement().setAttribute("aria-pressed", "false");
            }
        });
    }

    private void changeLanguage(Locale locale) {
        UI ui = UI.getCurrent();
        ui.setLocale(locale);
        ui.getPage().executeJs("try { localStorage.setItem($0, $1); } catch (e) {}",
                LANG_KEY, locale.getLanguage());
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        updateTexts();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        updateTexts();
        restoreSavedLanguage(event.getUI());
    }

    /** Sahifa to'liq qayta yuklanganda, oxirgi tanlangan tilni brauzer xotirasidan tiklaydi (bir marta). */
    private void restoreSavedLanguage(UI ui) {
        if (ComponentUtil.getData(ui, LANG_RESTORED_KEY) != null) {
            return;
        }
        ComponentUtil.setData(ui, LANG_RESTORED_KEY, Boolean.TRUE);

        ui.getPage()
                .executeJs("try { return localStorage.getItem($0); } catch (e) { return null; }", LANG_KEY)
                .then(String.class, saved -> {
                    if (saved == null || saved.equals(ui.getLocale().getLanguage())) {
                        return;
                    }
                    for (Locale locale : LOCALES) {
                        if (locale.getLanguage().equals(saved)) {
                            ui.setLocale(locale);
                            return;
                        }
                    }
                });
    }

    private static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}