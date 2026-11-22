package com.universal.erp.platform.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;

/** Modul ichidagi bo'limlar uchun gorizontal (telefonda surib ko'riladigan) tablar. */
final class SectionTabs {

    private SectionTabs() {
    }

    /** @param activeSlug faol bo'lim kaliti; modul sahifasida null. */
    static Div build(AppModule module, String activeSlug) {
        Div tabs = new Div();
        tabs.addClassName("sh-tabs");
        Anchor all = new Anchor(module.route(), module.title());
        all.addClassName("sh-tab");
        all.setClassName("is-active", activeSlug == null);
        tabs.add(all);
        for (AppSection s : AppSection.of(module)) {
            Anchor a = new Anchor(s.route(), s.title());
            a.addClassName("sh-tab");
            a.setClassName("is-active", s.slug().equals(activeSlug));
            tabs.add(a);
        }
        return tabs;
    }
}