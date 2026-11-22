package com.universal.erp.platform.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import java.util.Locale;

/** Ilova qobig'i va ro'yxatdan o'tish sahifasi uchun umumiy UI bo'laklari (logo, dropdown, til tanlagich). */
public final class UiKit {

    private UiKit() {
    }

    public static Component logo() {
        Span mark = new Span("U");
        mark.addClassName("ui-logo-mark");
        Span text = new Span("Uz-mark");
        text.addClassName("ui-logo-text");
        Div logo = new Div(mark, text);
        logo.addClassName("ui-logo");
        return logo;
    }

    public static Span caret() {
        Span c = new Span();
        c.addClassName("ui-caret");
        return c;
    }

    public static Span flag(String code) {
        Span f = new Span();
        f.addClassNames("login-flag", "login-flag-" + code);
        return f;
    }

    /**
     * Umumiy dropdown: tugma + menyu. Ochish/yopish brauzer tomonida (installDropdowns) hal qilinadi.
     */
    public static Div dropdown(Component buttonContent, String ariaLabel, String buttonClass, Component... menuItems) {
        NativeButton btn = new NativeButton();
        btn.add(buttonContent);
        btn.addClassName("ui-drop-btn");
        if (buttonClass != null && !buttonClass.isBlank()) {
            btn.addClassName(buttonClass);
        }
        btn.getElement().setAttribute("aria-haspopup", "true");
        btn.getElement().setAttribute("aria-expanded", "false");
        btn.getElement().setAttribute("aria-label", ariaLabel);

        Div menu = new Div(menuItems);
        menu.addClassName("ui-drop-menu");

        Div wrap = new Div(btn, menu);
        wrap.addClassName("ui-drop");
        return wrap;
    }

    /** Ixcham til tanlagich: bitta tugma (bayroq + kod), bosilganda UZ / RU / EN ro'yxati ochiladi. */
    public static Component langDropdown(Runnable onChange) {
        String current = AppTexts.lang();

        Span code = new Span(current.toUpperCase(Locale.ROOT));
        code.addClassName("ui-lang-label");
        Span content = new Span(flag(current), code, caret());
        content.addClassName("ui-btn-content");

        Span label = new Span(AppTexts.t("shell.lang"));
        label.addClassName("ui-drop-label");

        Div drop = dropdown(content, AppTexts.t("shell.lang") + ": " + AppTexts.fullName(current), "ui-lang-btn");
        Div menu = (Div) drop.getComponentAt(1);
        menu.add(label);
        for (String c : AppTexts.codes()) {
            NativeButton item = new NativeButton();
            item.add(flag(c), new Span(AppTexts.fullName(c)));
            item.addClassName("ui-drop-item");
            if (c.equals(current)) {
                item.addClassName("is-active");
            }
            item.addClickListener(e -> {
                AppTexts.setLang(c);
                onChange.run();
            });
            menu.add(item);
        }
        return drop;
    }

    /**
     * Dropdown'larni brauzer tomonida boshqaradi (sahifada bir marta o'rnatiladi):
     * tugma bosilsa ochiladi/yopiladi, tashqarida yoki elementga bosilsa yopiladi, Esc ham yopadi.
     */
    public static void installDropdowns(Component owner) {
        owner.getElement().executeJs("""
                if (window.__uiDrops) { return; }
                window.__uiDrops = true;
                document.addEventListener('click', function (ev) {
                  var t = ev.target;
                  var btn = t.closest ? t.closest('.ui-drop-btn') : null;
                  document.querySelectorAll('.ui-drop.is-open').forEach(function (d) {
                    if (!btn || !d.contains(btn)) { d.classList.remove('is-open'); }
                  });
                  if (btn) {
                    var drop = btn.closest('.ui-drop');
                    drop.classList.toggle('is-open');
                    btn.setAttribute('aria-expanded', String(drop.classList.contains('is-open')));
                    return;
                  }
                  var item = t.closest ? t.closest('.ui-drop-item') : null;
                  if (item) {
                    var d2 = item.closest('.ui-drop');
                    if (d2) { d2.classList.remove('is-open'); }
                  }
                });
                document.addEventListener('keydown', function (ev) {
                  if (ev.key === 'Escape') {
                    document.querySelectorAll('.ui-drop.is-open').forEach(function (d) { d.classList.remove('is-open'); });
                  }
                });
                """);
    }
}