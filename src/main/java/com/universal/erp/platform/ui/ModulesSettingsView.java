package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kompaniya modullarini boshqarish: /settings/modules. Yorug' kartalar, tanlangan modullar chap menyuda chiqadi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/ModulesSettingsView.java
 */
@Route(value = "settings/modules", layout = MainLayout.class)
@CssImport("./styles/modules-settings.css")
@PermitAll
public class ModulesSettingsView extends Div implements HasDynamicTitle, BeforeEnterObserver {

    private final TenantModules tenantModules;
    private final String company = CurrentCompany.get();

    private final Set<AppModule> selected = EnumSet.noneOf(AppModule.class);
    private final Map<AppModule, Div> cards = new EnumMap<>(AppModule.class);
    private final Span counter = new Span();
    private final NativeButton toggleAll = new NativeButton();
    private final NativeButton save = new NativeButton();

    public ModulesSettingsView(TenantModules tenantModules) {
        this.tenantModules = tenantModules;
        addClassName("ms-page");
        selected.addAll(tenantModules.enabled(company));

        H1 title = new H1(AppTexts.t("set.title"));
        title.addClassName("ms-title");
        Paragraph sub = new Paragraph(AppTexts.t("set.sub"));
        sub.addClassName("ms-sub");
        Div heading = new Div(title, sub);
        heading.addClassName("ms-heading");

        counter.addClassName("ms-counter");
        toggleAll.addClassName("ms-link");
        toggleAll.addClickListener(e -> {
            if (selected.size() == AppModule.values().length) {
                selected.clear();
            } else {
                selected.addAll(EnumSet.allOf(AppModule.class));
            }
            refresh();
        });
        Div toolbar = new Div(counter, toggleAll);
        toolbar.addClassName("ms-toolbar");

        Div grid = new Div();
        grid.addClassName("ms-grid");
        for (AppModule m : AppModule.values()) {
            Div card = card(m);
            cards.put(m, card);
            grid.add(card);
        }

        save.setText(AppTexts.t("set.save"));
        save.addClassName("ms-save");
        save.addClickListener(e -> onSave());
        Div actions = new Div(save);
        actions.addClassName("ms-actions");

        add(heading, toolbar, grid, actions);
        refresh();
    }

    private Div card(AppModule m) {
        Span tile = new Span(HomeIcons.svg(m.icon()));
        tile.addClassName("ms-tile");
        Span check = new Span();
        check.addClassName("ms-check");
        Div top = new Div(tile, check);
        top.addClassName("ms-card-top");

        H3 name = new H3(m.title());
        Paragraph desc = new Paragraph(m.description());
        Span feats = new Span(m.features());
        feats.addClassName("ms-feats");

        Div card = new Div(top, name, desc, feats);
        card.addClassName("ms-card");
        card.getElement().setAttribute("role", "checkbox");
        card.getElement().setAttribute("tabindex", "0");
        card.getElement().setAttribute("aria-label", m.title());
        card.addClickListener(e -> toggle(m));
        card.getElement().addEventListener("keydown", e -> toggle(m))
                .setFilter("event.key === 'Enter' || event.key === ' '");
        return card;
    }

    private void toggle(AppModule m) {
        if (!selected.remove(m)) {
            selected.add(m);
        }
        refresh();
    }

    private void refresh() {
        cards.forEach((m, card) -> {
            boolean on = selected.contains(m);
            card.setClassName("is-selected", on);
            card.getElement().setAttribute("aria-checked", String.valueOf(on));
        });
        counter.setText(L.t("Tanlandi: ", "Выбрано: ", "Selected: ") + selected.size()
                + " / " + AppModule.values().length);
        toggleAll.setText(selected.size() == AppModule.values().length
                ? L.t("Tanlovni bekor qilish", "Снять выбор", "Clear all")
                : L.t("Hammasini tanlash", "Выбрать все", "Select all"));
    }

    private void onSave() {
        if (selected.isEmpty()) {
            Notification.show(AppTexts.t("set.min"), 3000, Notification.Position.TOP_CENTER);
            return;
        }
        Set<AppModule> chosen = EnumSet.copyOf(selected);
        tenantModules.save(company, chosen);

        // Saqlanganini tekshiramiz: bazadan qayta o'qiymiz
        Set<AppModule> saved = tenantModules.enabled(company);
        List<String> missing = new ArrayList<>();
        for (AppModule m : chosen) {
            if (!saved.contains(m)) {
                missing.add(m.name());
            }
        }
        if (!missing.isEmpty()) {
            Notification n = Notification.show(
                    L.t("Saqlanmadi: ", "Не сохранено: ", "Not saved: ") + String.join(", ", missing),
                    6000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Notification.show(AppTexts.t("set.saved"), 2000, Notification.Position.BOTTOM_START);
        // menyu qayta qurilishi uchun sahifani to'liq yangilaymiz
        UI.getCurrent().getPage().setLocation("/app");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!RolePolicy.isAdmin()) {
            event.forwardTo(DashboardView.class);
        }
    }

    @Override
    public String getPageTitle() {
        return "Uz-mark ERP — " + AppTexts.t("set.title");
    }
}