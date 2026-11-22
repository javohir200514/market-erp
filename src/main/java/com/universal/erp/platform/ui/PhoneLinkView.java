package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@PageTitle("Telefonni ulash")
@Route(value = "app/inventory/phone-link", layout = MainLayout.class)
@CssImport("./styles/phone-link.css")
@PermitAll
public class PhoneLinkView extends Div implements BeforeEnterObserver {

    private final String company = CurrentCompany.get();
    private final PhoneScannerClient phone;
    private final Span state = new Span();
    private final Span addr = new Span();
    private Runnable statusUnsub;

    public PhoneLinkView(PhoneScannerClient phone) {
        this.phone = phone;
        addClassName("pl-root");
        phone.setCompany(company);

        H2 title = new H2(label("Telefonni ulash", "Подключить телефон", "Connect phone"));
        title.addClassName("pl-title");

        Paragraph hint = new Paragraph(label(
                "1) Telefonda hotspotni yoqing. 2) Kompyuterni shu Wi-Fi ga ulang. 3) Telefonda POS Skaner ilovasini oching. Qolganini dastur o'zi qiladi.",
                "1) Включите точку доступа на телефоне. 2) Подключите компьютер к этой Wi-Fi. 3) Откройте приложение POS Skaner. Остальное произойдёт автоматически.",
                "1) Turn on the phone hotspot. 2) Connect this computer to it. 3) Open the POS Skaner app. Everything else is automatic."));
        hint.addClassName("pl-hint");

        state.addClassName("pl-state");
        addr.addClassName("pl-addr");

        NativeButton retry = new NativeButton(label("Qayta qidirish", "Искать снова", "Search again"));
        retry.addClassName("pl-opt");
        retry.addClickListener(e -> {
            state.setText(label("Qidirilmoqda...", "Поиск...", "Searching..."));
            phone.reconnect();
        });

        Div box = new Div(state, addr, retry);
        box.addClassName("pl-body");
        add(title, hint, box);
        paint();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (company.isEmpty() || !RolePolicy.canView(AppModule.INVENTORY)) {
            event.forwardTo("app");
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UI ui = event.getUI();
        paint();
        statusUnsub = phone.addStatusListener(c -> ui.access(this::paint));
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (statusUnsub != null) {
            statusUnsub.run();
            statusUnsub = null;
        }
        super.onDetach(event);
    }

    private void paint() {
        boolean on = phone.isConnected();
        state.setText(on
                ? label("Telefon ulangan ✔", "Телефон подключён ✔", "Phone connected ✔")
                : label("Telefon qidirilmoqda...", "Поиск телефона...", "Looking for phone..."));
        state.setClassName("is-ok", on);
        addr.setText(on ? phone.phoneHost() : "");
    }

    private static String label(String uz, String ru, String en) {
        return new String[] {uz, ru, en}[AppTexts.index()];
    }
}