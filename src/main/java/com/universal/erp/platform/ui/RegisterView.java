package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.f;
import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.AuthService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ro'yxatdan o'tish (4 bosqichli). Yuqori navbar {@link MainNavbarLayout} dan keladi,
 * dizayn LoginView bilan bir xil (auth.css): qora mavzu, hammasi markazda.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RegisterView.java
 */
@Route(value = "register", layout = MainNavbarLayout.class)
@PageTitle("DOKONIX — Ro'yxatdan o'tish")
@AnonymousAllowed
@CssImport("./styles/auth.css")
@CssImport("./styles/navbar.css")
public class RegisterView extends Div {

    private static final Logger log = LoggerFactory.getLogger(RegisterView.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private final RegistrationService service;
    private final AuthService authService;

    private final TextField companyName = new TextField();
    private final TextField taxId = new TextField();
    private final TextField fullName = new TextField();
    private final TextField phone = new TextField();
    private final EmailField email = new EmailField();
    private final PasswordField password = new PasswordField();
    private final PasswordField password2 = new PasswordField();
    private final Checkbox terms = new Checkbox();

    private final TextField codeField = new TextField();

    private final Set<AppModule> selected = EnumSet.noneOf(AppModule.class);
    private final Map<AppModule, Div> cards = new EnumMap<>(AppModule.class);
    private Span counter;
    private NativeButton finishBtn;
    private NativeButton toggleAllBtn;
    private Span modulesError;

    private Main main;
    private int step = 1;
    private String verifiedEmail;

    private final CompanyBusinessTypes businessTypesStore;

    private final Set<BusinessType> selectedTypes = EnumSet.noneOf(BusinessType.class);
    private final Map<BusinessType, Div> typeCards = new EnumMap<>(BusinessType.class);
    private Span typesError;

    public RegisterView(RegistrationService service, AuthService authService,
                        CompanyBusinessTypes businessTypesStore) {
        this.service = service;
        this.authService = authService;
        this.businessTypesStore = businessTypesStore;
        addClassNames("au-page", "rg-page");

        for (HasSize field : new HasSize[] {companyName, taxId, fullName, phone, email, password, password2, codeField}) {
            field.setWidthFull();
        }

        for (Component field : new Component[] {companyName, taxId, fullName, phone, email, password, password2, codeField}) {
            field.addClassName("au-field");
        }
        terms.addClassName("au-check");

        // Brauzer avto-to'ldirishi o'chirilgan
        companyName.setAutocomplete(Autocomplete.OFF);
        taxId.setAutocomplete(Autocomplete.OFF);
        fullName.setAutocomplete(Autocomplete.OFF);
        phone.setAutocomplete(Autocomplete.OFF);
        email.setAutocomplete(Autocomplete.OFF);
        codeField.setAutocomplete(Autocomplete.OFF);
        // Parol maydonlarida "off" ni brauzerlar e'tiborsiz qoldiradi, shuning uchun new-password
        password.setAutocomplete(Autocomplete.NEW_PASSWORD);
        password2.setAutocomplete(Autocomplete.NEW_PASSWORD);

        companyName.setMaxLength(120);
        taxId.setMaxLength(9);
        taxId.setAllowedCharPattern("[0-9]");
        fullName.setMaxLength(120);
        phone.setMaxLength(20);
        phone.setAllowedCharPattern("[0-9+() \\-]");
        email.setMaxLength(160);
        password.setMaxLength(72);
        password2.setMaxLength(72);
        codeField.setMaxLength(6);
        codeField.setAllowedCharPattern("[0-9]");
        codeField.addClassName("rg-code");
        email.addClassName("rg-span-2");

        for (TextField tf : new TextField[] {companyName, taxId, fullName, phone, codeField}) {
            tf.setValueChangeMode(ValueChangeMode.EAGER);
        }
        email.setValueChangeMode(ValueChangeMode.EAGER);
        password.setValueChangeMode(ValueChangeMode.EAGER);
        password2.setValueChangeMode(ValueChangeMode.EAGER);

        companyName.addValueChangeListener(e -> companyName.setInvalid(false));
        taxId.addValueChangeListener(e -> taxId.setInvalid(false));
        fullName.addValueChangeListener(e -> fullName.setInvalid(false));
        email.addValueChangeListener(e -> email.setInvalid(false));
        password.addValueChangeListener(e -> password.setInvalid(false));
        password2.addValueChangeListener(e -> password2.setInvalid(false));
        terms.addValueChangeListener(e -> terms.setInvalid(false));
        codeField.addValueChangeListener(e -> codeField.setInvalid(false));

        companyName.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        taxId.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        fullName.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        phone.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        email.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        password.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        password2.addKeyDownListener(Key.ENTER, e -> onNextCompany());
        codeField.addKeyDownListener(Key.ENTER, e -> onVerify());

        render();
    }

    private void render() {
        boolean rerender = main != null;
        if (rerender) {
            remove(main);
        }
        applyFieldTexts();

        Div card = new Div();
        card.addClassName("rg-card");

        Div brandWrap = new Div(logo());
        brandWrap.addClassNames("au-brand", "rg-brand-wrap");

        card.add(brandWrap, stepper());
        switch (step) {
            case 2 -> card.add(stepVerify());
            case 3 -> {
                card.addClassName("is-wide");
                card.add(stepBusiness());
            }
            case 4 -> {
                card.addClassName("is-wide");
                card.add(stepModules());
            }
            default -> card.add(stepCompany());
        }

        main = new Main(card);
        main.addClassName("rg-main");
        add(main);

        // qadamdan qadamga o'tganda yangi qadam eng tepadan ko'rinsin
        if (rerender) {
            getElement().executeJs("window.scrollTo(0, 0)");
        }
    }

    /** Brend logotipi: DOKON + IX (LoginView va navbar bilan bir xil). */
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

    private void applyFieldTexts() {
        companyName.setLabel(t("wz.company"));
        taxId.setLabel(t("wz.taxid"));
        fullName.setLabel(t("wz.fullname"));
        phone.setLabel(t("wz.phone"));
        email.setLabel(t("wz.email"));
        password.setLabel(t("wz.password"));
        password.setHelperText(t("wz.pwhint"));
        password2.setLabel(t("wz.password2"));
        terms.setLabel(t("wz.terms"));
        codeField.setLabel(t("wz.code"));
    }

    private Component stepBusiness() {
        Div box = new Div();
        box.addClassName("rg-step-container");
        box.add(title(t("wz.sb.title")), sub(t("wz.sb.sub")));

        typeCards.clear();
        Div grid = new Div();
        grid.addClassName("rg-modules");
        for (BusinessType bt : BusinessType.values()) {
            Div card = typeCard(bt);
            typeCards.put(bt, card);
            grid.add(card);
        }
        box.add(grid);

        typesError = new Span(t("wz.sb.err"));
        typesError.addClassName("rg-err");
        typesError.setVisible(false);
        box.add(typesError);

        Div actions = new Div(primary(t("wz.next"), true, this::onNextBusiness));
        actions.addClassName("rg-actions");
        box.add(actions);

        refreshTypes();
        return box;
    }

    private Div typeCard(BusinessType bt) {
        Span tile = new Span(HomeIcons.svg(bt.icon()));
        tile.addClassName("rg-icon");
        Span check = new Span();
        check.addClassName("rg-mod-check");
        Div top = new Div(tile, check);
        top.addClassName("rg-mod-top");

        H3 name = new H3(bt.title());
        Paragraph desc = new Paragraph(bt.description());
        Span feats = new Span(bt.features());
        feats.addClassName("rg-mod-feats");

        Div card = new Div(top, name, desc, feats);
        card.addClassName("rg-mod");
        card.getElement().setAttribute("role", "checkbox");
        card.getElement().setAttribute("tabindex", "0");
        card.addClickListener(e -> toggleType(bt));
        card.getElement().addEventListener("keydown", e -> toggleType(bt))
                .setFilter("event.key === 'Enter' || event.key === ' '");
        return card;
    }

    private void toggleType(BusinessType bt) {
        if (!selectedTypes.remove(bt)) {
            selectedTypes.add(bt);
        }
        refreshTypes();
    }

    private void refreshTypes() {
        typeCards.forEach((bt, card) -> {
            boolean on = selectedTypes.contains(bt);
            card.setClassName("is-selected", on);
            card.getElement().setAttribute("aria-checked", String.valueOf(on));
        });
        if (typesError != null) {
            typesError.setVisible(false);
        }
    }

    private void onNextBusiness() {
        if (step != 3) {
            return;
        }
        if (selectedTypes.isEmpty()) {
            typesError.setVisible(true);
            return;
        }
        // do'kon uchun doim kerak bo'ladigan modullar oldindan belgilanadi; foydalanuvchi o'zgartira oladi
        if (selected.isEmpty()) {
            selected.add(AppModule.POS);
            selected.add(AppModule.INVENTORY);
            selected.add(AppModule.FINANCE);
        }
        step = 4;
        render();
    }

    private Component stepper() {
        String[] labels = {t("wz.step1"), t("wz.step2"), t("wz.stepB"), t("wz.step3")};
        Div steps = new Div();
        steps.addClassName("rg-steps");
        for (int i = 1; i <= 4; i++) {
            boolean done = i < step;
            Span dot = new Span(done ? "" : String.valueOf(i));
            dot.addClassName("rg-step-dot");
            Span label = new Span(labels[i - 1]);
            label.addClassName("rg-step-label");
            Div item = new Div(dot, label);
            item.addClassName("rg-step");
            item.setClassName("is-done", done);
            item.setClassName("is-current", i == step);
            if (i > 1) {
                Div line = new Div();
                line.addClassName("rg-step-line");
                line.setClassName("is-done", done || i == step);
                steps.add(line);
            }
            steps.add(item);
        }
        return steps;
    }

    private Component stepCompany() {
        Div box = new Div();
        box.addClassName("rg-step-container");
        box.add(title(t("wz.s1.title")), sub(t("wz.s1.sub")));

        Div companyGrid = new Div(companyName, taxId);
        companyGrid.addClassName("rg-grid");
        box.add(section(t("wz.sec.company"), companyGrid));

        Div adminGrid = new Div(fullName, phone, email, password, password2);
        adminGrid.addClassName("rg-grid");
        box.add(section(t("wz.sec.admin"), adminGrid));

        Div termsBox = new Div(terms);
        termsBox.addClassName("rg-terms");
        box.add(termsBox);

        Div actions = new Div(primary(t("wz.next"), true, this::onNextCompany));
        actions.addClassName("rg-actions");
        box.add(actions);
        return box;
    }

    private void onNextCompany() {
        if (step != 1 || !validateCompany()) {
            return;
        }
        String mail = normalizedEmail();
        if (mail.equals(verifiedEmail)) {
            step = 3;
            render();
            return;
        }
        // yaqinda kod yuborilgan bo'lsa (cooldown), foydalanuvchi shu kod bilan 2-qadamga o'tadi
        if (!sendCode(mail, true)) {
            return;
        }
        codeField.clear();
        codeField.setInvalid(false);
        step = 2;
        render();
    }

    private boolean validateCompany() {
        boolean ok = true;
        ok &= requireText(companyName);
        ok &= requireText(fullName);
        String nameValue = fullName.getValue().trim();
        if (!nameValue.isEmpty() && nameValue.split("\\s+").length < 2) {
            ok &= fail(fullName, t("wz.err.fullname"));
        }

        String tax = taxId.getValue().trim();
        if (!tax.isEmpty() && !tax.matches("\\d{9}")) {
            ok &= fail(taxId, t("wz.err.taxid"));
        }

        String mail = email.getValue().trim();
        if (mail.isEmpty()) {
            ok &= fail(email, t("wz.err.required"));
        } else if (!EMAIL.matcher(mail).matches()) {
            ok &= fail(email, t("wz.err.email"));
        }

        if (!isStrongPassword(password.getValue())) {
            ok &= fail(password, t("wz.pwhint"));
        }
        if (!password.getValue().equals(password2.getValue()) || password2.getValue().isEmpty()) {
            ok &= fail(password2, t("wz.err.pwmatch"));
        }
        if (!terms.getValue()) {
            ok &= fail(terms, t("wz.err.terms"));
        }
        return ok;
    }

    /** AuthService bilan bir xil qoida: 8..72 belgi, kamida bitta harf va bitta raqam. */
    private static boolean isStrongPassword(String pw) {
        if (pw.length() < 8 || pw.length() > 72) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : pw.toCharArray()) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        return letter && digit;
    }

    private boolean requireText(TextField f) {
        return f.getValue().trim().isEmpty() ? fail(f, t("wz.err.required")) : true;
    }

    private static boolean fail(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
        return false;
    }

    private String normalizedEmail() {
        return email.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private Component stepVerify() {
        Div box = new Div();
        box.addClassName("rg-step-container");
        box.add(title(t("wz.s2.title")), sub(f("wz.s2.sub", normalizedEmail())));

        Div codeWrap = new Div(codeField);
        codeWrap.addClassName("rg-code-wrap");
        box.add(codeWrap);

        Paragraph hint = new Paragraph(t("wz.s2.hint"));
        hint.addClassName("rg-hint");
        box.add(hint);

        NativeButton resend = link(t("wz.resend"), () -> {
            if (sendCode(normalizedEmail(), false)) {
                Notification.show(t("wz.resent"), 3000, Notification.Position.TOP_CENTER);
            }
        });
        NativeButton change = link(t("wz.changeemail"), () -> {
            step = 1;
            render();
        });

        Div actions = new Div(primary(t("wz.verify"), true, this::onVerify), resend, change);
        actions.addClassName("rg-actions");
        box.add(actions);
        return box;
    }

    /**
     * Kodni emailga yuboradi. Kutilgan xatolarni foydalanuvchiga tushunarli ko'rsatadi.
     *
     * @param proceedOnCooldown true bo'lsa, "kod yaqinda yuborilgan" holati xato hisoblanmaydi
     * @return davom etish mumkinmi
     */
    private boolean sendCode(String mail, boolean proceedOnCooldown) {
        try {
            service.sendCode(mail, AppTexts.lang());
            return true;
        } catch (VerificationException ex) {
            switch (ex.reason()) {
                case COOLDOWN -> {
                    if (proceedOnCooldown) {
                        return true;
                    }
                    error(f("wz.err.cooldown", ex.retryAfterSeconds()));
                }
                case TOO_MANY -> error(t("wz.err.limit"));
                case EMAIL_TAKEN -> {
                    fail(email, t("wz.err.taken"));
                    error(t("wz.err.taken"));
                }
                default -> error(t("wz.err.send"));
            }
        } catch (RuntimeException ex) {
            error(t("wz.err.send"));
        }
        return false;
    }

    private void onVerify() {
        if (step != 2) {
            return;
        }
        String code = codeField.getValue().trim();
        if (code.length() != 6 || !service.verifyCode(normalizedEmail(), code)) {
            fail(codeField, t("wz.err.code"));
            return;
        }
        verifiedEmail = normalizedEmail();
        codeField.clear();
        step = 3;
        render();
    }

    private Component stepModules() {
        Div box = new Div();
        box.addClassName("rg-step-container");
        box.add(title(t("wz.s3.title")), sub(t("wz.s3.sub")));

        Span badge = new Span(t("wz.verified"));
        badge.addClassName("rg-verified");

        counter = new Span();
        counter.addClassName("rg-counter");
        toggleAllBtn = link("", this::toggleAll);
        Div left = new Div(counter, badge);
        left.addClassName("rg-toolbar-left");
        Div toolbar = new Div(left, toggleAllBtn);
        toolbar.addClassName("rg-toolbar");
        box.add(toolbar);

        cards.clear();
        Div grid = new Div();
        grid.addClassName("rg-modules");
        for (AppModule m : AppModule.values()) {
            Div card = moduleCard(m);
            cards.put(m, card);
            grid.add(card);
        }
        box.add(grid);

        modulesError = new Span(t("wz.err.modules"));
        modulesError.addClassName("rg-err");
        modulesError.setVisible(false);
        box.add(modulesError);

        finishBtn = primary(t("wz.finish"), false, this::onFinish);
        Div actions = new Div(finishBtn);
        actions.addClassName("rg-actions");
        box.add(actions);

        refreshModules();
        return box;
    }

    private Div moduleCard(AppModule m) {
        Span tile = new Span(HomeIcons.svg(m.icon()));
        tile.addClassName("rg-icon");
        Span check = new Span();
        check.addClassName("rg-mod-check");
        Div top = new Div(tile, check);
        top.addClassName("rg-mod-top");

        H3 name = new H3(m.title());
        Paragraph desc = new Paragraph(m.description());
        Span feats = new Span(m.features());
        feats.addClassName("rg-mod-feats");

        Div card = new Div(top, name, desc, feats);
        card.addClassName("rg-mod");
        card.getElement().setAttribute("role", "checkbox");
        card.getElement().setAttribute("tabindex", "0");
        card.addClickListener(e -> toggle(m));
        card.getElement().addEventListener("keydown", e -> toggle(m))
                .setFilter("event.key === 'Enter' || event.key === ' '");
        return card;
    }

    private void toggle(AppModule m) {
        if (!selected.remove(m)) {
            selected.add(m);
        }
        refreshModules();
    }

    private void toggleAll() {
        if (selected.size() == AppModule.values().length) {
            selected.clear();
        } else {
            selected.addAll(EnumSet.allOf(AppModule.class));
        }
        refreshModules();
    }

    private void refreshModules() {
        cards.forEach((m, card) -> {
            boolean on = selected.contains(m);
            card.setClassName("is-selected", on);
            card.getElement().setAttribute("aria-checked", String.valueOf(on));
        });
        counter.setText(f("wz.selected", selected.size()));
        finishBtn.setEnabled(!selected.isEmpty());
        toggleAllBtn.setText(selected.size() == AppModule.values().length ? t("wz.clear") : t("wz.selectall"));
        modulesError.setVisible(false);
    }

    private void onFinish() {
        if (selected.isEmpty()) {
            modulesError.setVisible(true);
            return;
        }
        RegistrationRequest request = new RegistrationRequest(
                companyName.getValue().trim(),
                taxId.getValue().trim(),
                fullName.getValue().trim(),
                normalizedEmail(),
                phone.getValue().trim(),
                password.getValue(),
                AppTexts.lang(),
                EnumSet.copyOf(selected));

        // ikki marta bosilib, ikkita kompaniya yaratilib qolmasligi uchun
        finishBtn.setEnabled(false);
        String companyLogin;
        try {
            companyLogin = service.register(request);
        } catch (RuntimeException ex) {
            log.error("Registration failed: email={}", request.email(), ex);

            finishBtn.setEnabled(true);
            error(t("wz.err.finish"));
            return;
        }

        if (companyLogin != null && !companyLogin.isBlank()) {
            try {
                businessTypesStore.save(companyLogin, selectedTypes);
            } catch (RuntimeException ex) {
                // kompaniya yaratildi; turlar keyinroq sozlamalardan qayta saqlanadi
                log.warn("Do'kon turlarini saqlab bo'lmadi (kompaniya={}): {}", companyLogin, ex.toString());
            }
        }
        String plainPassword = password.getValue();
        password.clear();
        password2.clear();

        if (companyLogin != null && !companyLogin.isBlank() && signIn(companyLogin, request.email(), plainPassword)) {
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs("try { localStorage.setItem($0, $1); } catch(e) {}",
                        "erp.company", companyLogin);
                ui.getPage().setLocation("/app");
            });
            return;
        }

        // Avtomatik kirib bo'lmasa — login sahifasiga
        String message = companyLogin == null || companyLogin.isBlank()
                ? t("wz.done")
                : f("wz.done.slug", companyLogin);
        Notification.show(message, 12000, Notification.Position.TOP_CENTER);
        getUI().ifPresent(ui -> ui.navigate("login"));
    }

    private boolean signIn(String companyLogin, String mail, String plainPassword) {
        try {
            authService.login(
                    companyLogin,
                    mail,
                    plainPassword,
                    VaadinServletRequest.getCurrent().getHttpServletRequest(),
                    VaadinServletResponse.getCurrent().getHttpServletResponse());
            return true;
        } catch (RuntimeException ex) {
            log.warn("Ro'yxatdan o'tgach avtomatik kirib bo'lmadi (kompaniya={}, email={}): {}",
                    companyLogin, mail, ex.toString());
            return false;
        }
    }

    private static H1 title(String text) {
        H1 h = new H1(text);
        h.addClassName("rg-title");
        return h;
    }

    private static Paragraph sub(String text) {
        Paragraph p = new Paragraph(text);
        p.addClassName("rg-sub");
        return p;
    }

    private static Div section(String title, Component content) {
        Span head = new Span(title);
        head.addClassName("rg-section-title");
        Div s = new Div(head, content);
        s.addClassName("rg-section");
        return s;
    }

    /** Asosiy tugma: yozuv (va ixtiyoriy strelka) tugma ichida o'rtada. */
    private static NativeButton primary(String text, boolean arrow, Runnable action) {
        NativeButton b = new NativeButton();
        b.add(new Span(text));
        if (arrow) {
            b.add(HomeIcons.svg("arrow"));
        }
        b.addClassNames("rg-btn", "rg-btn-primary");
        b.addClickListener(e -> action.run());
        return b;
    }

    private static NativeButton link(String text, Runnable action) {
        NativeButton b = new NativeButton(text);
        b.addClassName("rg-link");
        b.addClickListener(e -> action.run());
        return b;
    }

    private static void error(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}