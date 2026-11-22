package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.AuthService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tizimga kirish. Kompaniya nomi so'ralmaydi: u email / foydalanuvchi nomi bo'yicha topiladi.
 * Maket: markazda karta — DOKONIX logotipi, sarlavha, forma, tugma (hammasi o'rtada).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/LoginView.java
 */
@Route(value = "login", layout = MainNavbarLayout.class)
@PageTitle("DOKONIX — Tizimga kirish")
@AnonymousAllowed
@CssImport("./styles/auth.css")
@CssImport("./styles/navbar.css")
public class LoginView extends Div implements BeforeEnterObserver, LocaleChangeObserver {

    private static final String COMPANY_KEY = "erp.company";

    private final AuthService authService;
    private final LoginCompanyResolver companyResolver;

    private final H1 welcomeTitle = new H1();
    private final Paragraph welcomeSubtitle = new Paragraph();

    private final TextField identifier = new TextField();
    private final PasswordField password = new PasswordField();

    private final Checkbox rememberMe = new Checkbox();
    private final Anchor forgotPassword = new Anchor("#");

    private final Div errorMessage = new Div();
    private final Button loginButton = new Button();

    private final Span registerText = new Span();
    private final RouterLink registerLink = new RouterLink(RegisterView.class);

    private String currentErrorKey;

    public LoginView(AuthService authService, LoginCompanyResolver companyResolver) {
        this.authService = authService;
        this.companyResolver = companyResolver;

        addClassName("au-page");

        Div shell = new Div(buildCard());
        shell.addClassName("au-login");
        add(shell);

        updateTexts();
    }

    // ------------------------------------------------------------ maket

    private Div buildCard() {
        welcomeTitle.addClassName("au-title");
        welcomeSubtitle.addClassName("au-subtitle");
        Div header = box("au-header", welcomeTitle, welcomeSubtitle);

        identifier.addClassName("au-field");
        identifier.setAutocomplete(Autocomplete.USERNAME);
        identifier.setPrefixComponent(fieldIcon(VaadinIcon.USER));

        password.addClassName("au-field");
        password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
        password.setPrefixComponent(fieldIcon(VaadinIcon.LOCK));

        rememberMe.addClassName("au-check");
        forgotPassword.addClassName("au-forgot");
        Div options = box("au-options", rememberMe, forgotPassword);

        errorMessage.addClassName("au-error");
        errorMessage.getElement().setAttribute("role", "alert");
        errorMessage.setVisible(false);

        loginButton.addClassName("au-submit");
        loginButton.getElement().setAttribute("theme", "primary");
        loginButton.setDisableOnClick(true);
        loginButton.addClickShortcut(Key.ENTER);
        loginButton.addClickListener(event -> authenticate());

        Div form = box("au-form", identifier, password, options, errorMessage, loginButton);

        registerLink.addClassName("au-link");
        Div registerPrompt = box("au-prompt", registerText, registerLink);

        Component brand = logo();
        Div brandRow = box("au-brand", brand);

        return box("au-card", brandRow, header, form, registerPrompt);
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

    private static Icon fieldIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.addClassName("au-input-icon");
        return icon;
    }

    private static Div box(String classNames, Component... children) {
        Div div = new Div(children);
        for (String cls : classNames.split(" ")) {
            if (!cls.isBlank()) {
                div.addClassName(cls);
            }
        }
        return div;
    }

    // ------------------------------------------------------------ matnlar

    private String tr(String uz, String ru, String en) {
        return switch (getLocale().getLanguage()) {
            case "ru" -> ru;
            case "en" -> en;
            default -> uz;
        };
    }

    private void updateTexts() {
        welcomeTitle.setText(tr("Xush kelibsiz", "Добро пожаловать", "Welcome back"));
        welcomeSubtitle.setText(tr(
                "Hisobingizga kirish uchun ma'lumotlarni kiriting.",
                "Введите данные для входа в аккаунт.",
                "Enter your details to sign in to your account."));
        identifier.setLabel(tr("Email yoki foydalanuvchi nomi", "Email или имя пользователя",
                "Email or username"));
        identifier.setPlaceholder("dilshod@nursavdo.uz");
        password.setLabel(tr("Parol", "Пароль", "Password"));
        password.setPlaceholder("••••••••");
        rememberMe.setLabel(tr("Tizimda eslab qolish", "Запомнить меня", "Remember me"));
        forgotPassword.setText(tr("Parolni unutdingizmi?", "Забыли пароль?", "Forgot password?"));
        loginButton.setText(tr("Kirish", "Войти", "Sign in"));
        registerText.setText(tr("Kompaniyangiz hali yo'qmi?", "Ещё нет компании?", "No company yet?"));
        registerLink.setText(tr("Ro'yxatdan o'tish", "Зарегистрироваться", "Sign up"));
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        updateTexts();
        if (currentErrorKey != null) {
            showError(currentErrorKey);
        }
    }

    // ------------------------------------------------------------ kirish

    private void authenticate() {
        hideError();

        String id = identifier.getValue() == null ? "" : identifier.getValue().trim();
        if (id.isEmpty() || password.isEmpty()) {
            showError("empty");
            loginButton.setEnabled(true);
            return;
        }

        try {
            Optional<String> company = companyResolver.resolveCompany(id);
            if (company.isEmpty()) {
                showError("invalid");
                return;
            }
            String companyLogin = company.get();

            authService.login(
                    companyLogin,
                    id,
                    password.getValue(),
                    VaadinServletRequest.getCurrent().getHttpServletRequest(),
                    VaadinServletResponse.getCurrent().getHttpServletResponse()
            );

            password.clear();
            CurrentCompany.set(companyLogin);
            remember(COMPANY_KEY, companyLogin.trim().toLowerCase(Locale.ROOT));
            getUI().ifPresent(ui -> ui.getPage().setLocation("/app"));

        } catch (LockedException exception) {
            showError("locked");
        } catch (DisabledException exception) {
            showError("disabled");
        } catch (AuthenticationException exception) {
            showError("invalid");
        } catch (Exception exception) {
            showError("invalid");
        } finally {
            loginButton.setEnabled(true);
        }
    }

    private void showError(String key) {
        currentErrorKey = key;

        String message = switch (key) {
            case "empty" -> tr("Barcha maydonlarni to'ldiring.", "Заполните все поля.",
                    "Please fill in all fields.");
            case "locked" -> tr("Hisobingiz vaqtincha bloklangan. Keyinroq qayta urinib ko'ring.",
                    "Аккаунт временно заблокирован. Повторите попытку позже.",
                    "Your account is temporarily locked. Please try again later.");
            case "disabled" -> tr("Hisobingiz faol emas.", "Аккаунт отключен.", "Your account is disabled.");
            default -> tr("Email yoki parol noto'g'ri.", "Неверный email или пароль.",
                    "Incorrect email or password.");
        };

        errorMessage.setText(message);
        errorMessage.setVisible(true);

        if (!"empty".equals(key)) {
            password.clear();
            password.focus();
        }
    }

    private void hideError() {
        currentErrorKey = null;
        errorMessage.setVisible(false);
    }

    private void remember(String key, String value) {
        getUI().ifPresent(ui ->
                ui.getPage().executeJs("try { localStorage.setItem($0, $1); } catch(e) {}", key, value));
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        updateTexts();
        identifier.focus();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !CurrentCompany.get().isEmpty()) {
            // kompaniya sessiyada bo'lmasa (eski kirish) — login sahifasida qolamiz, qayta kirsin
            event.forwardTo(DashboardView.class);
        }
    }
}