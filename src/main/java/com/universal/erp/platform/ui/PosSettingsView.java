package com.universal.erp.platform.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * POS sozlamalari: ovozli yordamchi va Global Product API.
 * Stil: src/main/frontend/styles/pos-settings.css  (yoki frontend/styles/pos-settings.css)
 */
@PageTitle("POS sozlamalari")
@Route(value = "app/pos/settings", layout = MainLayout.class)
@CssImport("./styles/pos-settings.css")
@PermitAll
public class PosSettingsView extends Div implements BeforeEnterObserver {

    private final String company = CurrentCompany.get();
    private final GlobalProductService global;

    private final Span deviceVoice = new Span();
    private final Span apiState = new Span();

    public PosSettingsView(PosSettings settings, GlobalProductService global) {
        this.global = global;
        addClassName("ps-page");

        PosSettings.Values v = settings.get(company);

        // ------------------------------------------------ sarlavha
        Div header = new Div(
                title(L.t("POS sozlamalari", "Настройки POS", "POS settings")),
                sub(L.t("Kassa ovozi va tovar ma'lumotlar bazasi ulanishini shu yerda sozlang",
                        "Настройте голос кассы и подключение к базе товаров",
                        "Configure the register voice and the product database connection")));
        header.addClassName("ps-header");

        // ------------------------------------------------ ovozli yordamchi
        Switch voice = new Switch(v.voice());

        Span langBadge = new Span(L.t("Til: faqat o'zbekcha (uz-UZ)", "Язык: только узбекский (uz-UZ)",
                "Language: Uzbek only (uz-UZ)"));
        langBadge.addClassName("ps-badge");

        NumberField volume = number(0, 100, 10);
        volume.setValue(v.volume() * 100.0);
        NumberField rate = number(0.5, 2, 0.1);
        rate.setValue(v.rate());

        Div grid = new Div(
                fieldBlock(L.t("Ovoz balandligi (0–100 %)", "Громкость (0–100 %)", "Volume (0–100 %)"), volume, null),
                fieldBlock(L.t("Tezlik (0.5–2)", "Скорость (0.5–2)", "Speed (0.5–2)"), rate, null));
        grid.addClassName("ps-grid");

        TextField tts = new TextField();
        tts.setWidthFull();
        tts.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tts.addClassName("ps-input");
        tts.setPlaceholder("https://tts.example.com/say?lang=uz&text={text}");
        tts.setValue(v.ttsUrl() == null ? "" : v.ttsUrl());
        Div ttsBlock = fieldBlock(
                L.t("Zaxira o'zbekcha TTS manzili (ixtiyoriy)", "Резервный узбекский TTS (необязательно)",
                        "Fallback Uzbek TTS URL (optional)"),
                tts,
                L.t("GET so'rovga audio qaytaradigan manzil; matn o'rniga {text} yozing. Qurilmada o'zbekcha ovoz bo'lmasagina ishlatiladi.",
                        "URL, возвращающий аудио по GET; вместо текста {text}. Используется, только если на устройстве нет узбекского голоса.",
                        "URL returning audio for GET; use {text} placeholder. Used only if the device has no Uzbek voice."));

        setStatus(deviceVoice, "—", null);
        NativeButton checkVoice = ghost(L.t("Qurilmada ovozni tekshirish", "Проверить голос на устройстве",
                "Check device voice"));
        checkVoice.addClickListener(e -> UI.getCurrent().getPage().executeJs(
                "return !!(window.speechSynthesis&&speechSynthesis.getVoices().find(v=>(v.lang||'').toLowerCase()"
                        + ".replace('_','-').startsWith('uz')))")
                .then(Boolean.class, ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        setStatus(deviceVoice, L.t("O'zbekcha ovoz bor", "Узбекский голос найден",
                                "Uzbek voice found"), true);
                    } else {
                        setStatus(deviceVoice, L.t("O'zbekcha ovoz topilmadi (qayta bosing yoki zaxira TTS kiriting)",
                                "Узбекский голос не найден (нажмите ещё раз или укажите резервный TTS)",
                                "No Uzbek voice found (retry or set fallback TTS)"), false);
                    }
                }));

        NativeButton test = ghost(L.t("Ovozni sinash", "Проверить звук", "Test voice"));
        test.addClickListener(e -> PosVoice.sayWith(UI.getCurrent(), "Salom, bu ovoz sinovi",
                (volume.getValue() == null ? 100 : volume.getValue()) / 100.0,
                rate.getValue() == null ? 1 : rate.getValue(), tts.getValue()));

        Div voiceActions = new Div(checkVoice, test, deviceVoice);
        voiceActions.addClassName("ps-actions");

        Div voiceCard = card("🔊",
                L.t("Ovozli yordamchi", "Голосовой помощник", "Voice assistant"),
                L.t("Savdo vaqtida mahsulot va summalarni ovoz bilan aytib beradi",
                        "Озвучивает товары и суммы во время продажи",
                        "Speaks products and totals during a sale"),
                voice, langBadge, grid, ttsBlock, voiceActions);

        // ------------------------------------------------ global API
        Switch globalOn = new Switch(v.global());

        NativeButton checkApi = ghost(L.t("API ulanishini tekshirish", "Проверить API", "Check API connection"));
        checkApi.addClickListener(e -> {
            boolean ok = global.check();
            paintApi();
            Notification.show(ok ? "API OK" : "API: " + global.lastError(), 3000, Notification.Position.BOTTOM_END);
        });
        paintApi();

        Div apiActions = new Div(checkApi, apiState);
        apiActions.addClassName("ps-actions");

        Div apiCard = card("🌐",
                "Global Product API",
                L.t("Open Food Facts bazasidan shtrix-kod bo'yicha tovar ma'lumotini oladi",
                        "Получает данные о товаре по штрихкоду из Open Food Facts",
                        "Looks up product data by barcode from Open Food Facts"),
                globalOn, apiActions);

        // ------------------------------------------------ saqlash paneli
        NativeButton save = new NativeButton(L.t("Saqlash", "Сохранить", "Save"));
        save.addClassNames("ps-btn", "ps-btn-primary");
        save.addClickListener(e -> {
            settings.save(company, new PosSettings.Values(voice.getValue(),
                    (volume.getValue() == null ? 100 : volume.getValue()) / 100.0,
                    rate.getValue() == null ? 1 : rate.getValue(),
                    tts.getValue(), globalOn.getValue()));
            Notification.show(L.t("Saqlandi", "Сохранено", "Saved"), 2000, Notification.Position.BOTTOM_END);
        });
        Div footer = new Div(save);
        footer.addClassName("ps-footer");

        add(header, voiceCard, apiCard, footer);
    }

    // ================================================================ yordamchi komponentlar

    private static H1 title(String text) {
        H1 h = new H1(text);
        h.addClassName("ps-title");
        return h;
    }

    private static Paragraph sub(String text) {
        Paragraph p = new Paragraph(text);
        p.addClassName("ps-sub");
        return p;
    }

    /** Karta: ikonka + sarlavha + tavsif + o'ng tomonda switch, pastda kontent. */
    private static Div card(String icon, String name, String description, Switch toggle, Component... body) {
        Span ic = new Span(icon);
        ic.addClassName("ps-card-icon");
        Span n = new Span(name);
        n.addClassName("ps-card-name");
        Span d = new Span(description);
        d.addClassName("ps-card-desc");
        Div text = new Div(n, d);
        text.addClassName("ps-card-text");
        Div head = new Div(ic, text, toggle);
        head.addClassName("ps-card-head");

        Div content = new Div(body);
        content.addClassName("ps-card-body");

        Div card = new Div(head, content);
        card.addClassName("ps-card");
        return card;
    }

    /** Sarlavha (o'zimizning label) + maydon + ixtiyoriy yordamchi matn. */
    private static Div fieldBlock(String label, Component field, String helper) {
        Span l = new Span(label);
        l.addClassName("ps-label");
        Div block = new Div(l, field);
        block.addClassName("ps-field");
        if (helper != null) {
            Span h = new Span(helper);
            h.addClassName("ps-helper");
            block.add(h);
        }
        return block;
    }

    private static NumberField number(double min, double max, double step) {
        NumberField f = new NumberField();
        f.setMin(min);
        f.setMax(max);
        f.setStep(step);
        f.setStepButtonsVisible(true);
        f.setWidthFull();
        f.addClassName("ps-input");
        return f;
    }

    private static NativeButton ghost(String text) {
        NativeButton b = new NativeButton(text);
        b.addClassNames("ps-btn", "ps-btn-ghost");
        return b;
    }

    /** ok: true — yashil, false — qizil, null — neytral. */
    private static void setStatus(Span span, String text, Boolean ok) {
        span.setText(text);
        span.removeClassNames("ps-status", "is-ok", "is-bad");
        span.addClassName("ps-status");
        if (ok != null) {
            span.addClassName(ok ? "is-ok" : "is-bad");
        }
    }

    private void paintApi() {
        Boolean ok = global.lastOk();
        if (ok == null) {
            setStatus(apiState, L.t("API holati: tekshirilmagan", "Статус API: не проверено",
                    "API status: not checked"), null);
            return;
        }
        String when = new SimpleDateFormat("HH:mm:ss").format(new Date(global.lastCheck()));
        if (ok) {
            setStatus(apiState, L.t("API holati: ulangan", "Статус API: подключено", "API status: connected")
                    + " · " + when, true);
        } else {
            String err = global.lastError() == null ? "" : " " + global.lastError();
            setStatus(apiState, L.t("API holati: ulanmagan", "Статус API: нет связи", "API status: not connected")
                    + err + " · " + when, false);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (company.isEmpty() || !RolePolicy.isAdmin()) {
            event.forwardTo("app");
        }
    }

    // ================================================================ yoqish/o'chirish tugmasi

    /** Chiroyli toggle (switch). Qiymat: getValue()/setValue(). */
    private static final class Switch extends NativeButton {
        private boolean value;

        Switch(boolean initial) {
            addClassName("ps-switch");
            getElement().setAttribute("type", "button");
            getElement().setAttribute("role", "switch");
            Span knob = new Span();
            knob.addClassName("ps-switch-knob");
            add(knob);
            setValue(initial);
            addClickListener(e -> setValue(!value));
        }

        boolean getValue() {
            return value;
        }

        void setValue(boolean v) {
            this.value = v;
            setClassName("is-on", v);
            getElement().setAttribute("aria-checked", String.valueOf(v));
        }
    }
}