package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.RoleCatalog;
import com.universal.erp.platform.security.RolePolicy;
import com.universal.erp.platform.security.UserAdminService;
import com.universal.erp.platform.security.UserAdminService.Credentials;
import com.universal.erp.platform.security.UserAdminService.EmailTakenException;
import com.universal.erp.platform.security.UserAdminService.Member;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Xodimlar va rollar: /settings/users. Administrator yangi xodim yaratadi — kirish nomi va parolni
 * tizim o'zi yaratadi (parol faqat bir marta ko'rsatiladi). Rol berish, parolni yangilash, bloklash.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/UsersView.java
 */
@Route(value = "settings/users", layout = MainLayout.class)
@PermitAll
public class UsersView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String GRID =
            "minmax(0, 1.3fr) minmax(0, 1.1fr) minmax(0, 1fr) 120px minmax(0, 1fr) 250px";

    private final UserAdminService service;
    private final String company = CurrentCompany.get();
    private Div listHost;

    public UsersView(UserAdminService service) {
        this.service = service;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!RolePolicy.isAdmin()) {
            event.forwardTo(DashboardView.class);
            return;
        }
        build();
    }

    // ================================================================ sahifa

    private void build() {
        removeAll();
        addClassName("sh-page");

        H1 title = new H1(t("u.title"));
        title.addClassName("sh-page-title");
        Paragraph sub = new Paragraph(t("u.sub"));
        sub.addClassName("sh-page-sub");

        NativeButton create = new NativeButton();
        create.add(new Span("+"), new Span(t("u.add")));
        create.addClassName("sh-btn");
        create.addClickListener(e -> openCreate());
        Div toolbar = new Div(create);
        toolbar.addClassName("sh-toolbar");

        listHost = new Div();
        Div panel = new Div(toolbar, listHost);
        panel.addClassName("sh-panel");

        add(title, sub, rolesInfo(), panel);
        renderList();
    }

    /** Rollar qisqacha: kim nimaga kira oladi. */
    private Component rolesInfo() {
        Div grid = new Div();
        grid.addClassName("sh-role-grid");
        for (RoleCatalog.Role r : RoleCatalog.all()) {
            Span name = new Span(r.name());
            name.addClassName("sh-role-name");
            Span desc = new Span(r.description());
            desc.addClassName("sh-role-desc");
            Div d = new Div(name, desc);
            d.addClassName("sh-role-card");
            grid.add(d);
        }
        Span label = new Span(t("u.roles"));
        label.addClassName("sh-block-label");
        Div box = new Div(label, grid);
        box.addClassName("sh-role-box");
        return box;
    }

    private void renderList() {
        listHost.removeAll();
        List<Member> members;
        try {
            members = service.list(company);
        } catch (RuntimeException ex) {
            listHost.add(message(t("u.err.fail")));
            return;
        }

        Div head = new Div();
        head.addClassNames("sh-row", "sh-row-head");
        head.getStyle().set("grid-template-columns", GRID);
        for (String k : new String[] {"u.col.name", "u.col.login", "u.col.role", "u.col.status", "u.col.last"}) {
            head.add(new Span(t(k)));
        }
        head.add(new Span());
        listHost.add(head);

        for (Member m : members) {
            listHost.add(row(m));
        }
    }

    private Component row(Member m) {
        boolean self = UserAdminService.isSelf(m.email(), m.id());

        Div row = new Div();
        row.addClassName("sh-row");
        row.getStyle().set("grid-template-columns", GRID);

        Span nameText = new Span(m.fullName());
        if (self) {
            Span you = new Span(t("u.you"));
            you.addClassNames("sh-badge", "sh-badge-lead");
            you.getStyle().set("margin-left", "8px");
            nameText.add(you);
        }
        row.add(cell(t("u.col.name"), nameText, true));
        row.add(cell(t("u.col.login"), new Span(m.login()), false));

        String roles = m.roles().isEmpty() ? "—"
                : String.join(", ", m.roles().stream().map(RoleCatalog::label).toList());
        row.add(cell(t("u.col.role"), new Span(roles), false));

        Span status = new Span(t(m.blocked() ? "u.blocked" : "u.active"));
        status.addClassNames("sh-badge", m.blocked() ? "sh-badge-rejected" : "sh-badge-active");
        row.add(cell(t("u.col.status"), status, false));

        row.add(cell(t("u.col.last"), new Span(formatWhen(m.lastLogin())), false));

        Div actions = new Div();
        actions.addClassName("sh-row-actions");
        if (!self) {
            NativeButton role = mini(t("u.changerole"));
            role.addClickListener(e -> openChangeRole(m));
            NativeButton reset = mini(t("u.reset"));
            reset.addClickListener(e -> doReset(m));
            NativeButton block = mini(t(m.blocked() ? "u.unblock" : "u.block"));
            if (!m.blocked()) {
                block.addClassName("is-danger");
            }
            block.addClickListener(e -> doBlock(m, !m.blocked()));
            actions.add(role, reset, block);
        }
        row.add(actions);
        return row;
    }

    private static Div cell(String label, Component content, boolean main) {
        Div c = new Div(content);
        c.addClassName("sh-cell");
        c.getElement().setAttribute("data-label", label);
        if (main) {
            c.addClassName("is-main");
        }
        return c;
    }

    private static NativeButton mini(String text) {
        NativeButton b = new NativeButton(text);
        b.addClassName("sh-mini");
        return b;
    }

    private static String formatWhen(OffsetDateTime t) {
        return t == null ? AppTexts.t("u.never") : t.atZoneSameInstant(ZoneId.of("Asia/Tashkent")).format(WHEN);
    }

    private static Component message(String text) {
        Div d = new Div(new Span(text));
        d.addClassName("sh-empty-box");
        return d;
    }

    // ================================================================ yangi xodim

    private void openCreate() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(t("u.add"));
        dialog.setWidth("520px");
        dialog.setMaxWidth("calc(100vw - 24px)");
        dialog.setCloseOnOutsideClick(false);

        TextField name = new TextField(t("u.fullname"));
        name.setWidthFull();
        name.setMaxLength(200);
        name.setRequiredIndicatorVisible(true);

        Select<RoleCatalog.Role> role = new Select<>();
        role.setLabel(t("u.role"));
        role.setWidthFull();
        role.setItems(RoleCatalog.all());
        role.setItemLabelGenerator(RoleCatalog.Role::name);
        role.setValue(RoleCatalog.get("VIEWER"));
        role.setRequiredIndicatorVisible(true);
        Paragraph roleHint = new Paragraph(role.getValue().description());
        roleHint.addClassName("sh-form-hint");
        role.addValueChangeListener(e -> roleHint.setText(e.getValue() == null ? "" : e.getValue().description()));

        TextField email = new TextField(t("u.email"));
        email.setWidthFull();
        email.setMaxLength(255);
        email.setHelperText(t("u.email.hint"));

        Div form = new Div(name, role, roleHint, email);
        form.addClassName("sh-form-col");
        dialog.add(form);

        Button cancel = new Button(t("rec.cancel"), e -> dialog.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(t("u.create"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            name.setInvalid(false);
            email.setInvalid(false);
            String full = name.getValue() == null ? "" : name.getValue().trim();
            if (full.split("\\s+").length < 2) {
                name.setInvalid(true);
                name.setErrorMessage(t("u.err.name"));
                return;
            }
            if (role.getValue() == null) {
                return;
            }
            String code = role.getValue().code();
            try {
                Credentials c = service.create(company, full, email.getValue(), code,
                        RoleCatalog.storedName(code), RoleCatalog.storedDescription(code), AppTexts.lang());
                dialog.close();
                renderList();
                showCredentials(t("u.cred.title"), c.username(), c.password());
            } catch (EmailTakenException ex) {
                email.setInvalid(true);
                email.setErrorMessage(t("u.err.taken"));
            } catch (IllegalArgumentException ex) {
                if ("Invalid email".equals(ex.getMessage())) {
                    email.setInvalid(true);
                    email.setErrorMessage(t("wz.err.email"));
                } else {
                    name.setInvalid(true);
                    name.setErrorMessage(t("u.err.name"));
                }
            } catch (RuntimeException ex) {
                Notification.show(t("u.err.fail"), 4000, Notification.Position.TOP_CENTER);
            }
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** Kirish ma'lumotlari oynasi: parol faqat shu yerda ko'rinadi. */
    private void showCredentials(String header, String username, String password) {
        Dialog d = new Dialog();
        d.setHeaderTitle(header);
        d.setWidth("480px");
        d.setMaxWidth("calc(100vw - 24px)");
        d.setCloseOnOutsideClick(false);

        Div box = new Div(
                credLine(t("u.company"), company),
                credLine(t("u.username"), username == null ? "" : username),
                credLine(t("u.password"), password));
        box.addClassName("sh-cred");

        Paragraph note = new Paragraph(t("u.cred.note"));
        note.addClassName("sh-form-hint");
        d.add(box, note);

        String text = t("u.company") + ": " + company
                + (username == null ? "" : "\n" + t("u.username") + ": " + username)
                + "\n" + t("u.password") + ": " + password;

        Button copy = new Button(t("u.copy"));
        copy.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs(
                    "navigator.clipboard && navigator.clipboard.writeText($0).catch(() => {})", text);
            Notification.show(t("u.copied"), 1500, Notification.Position.BOTTOM_START);
        });
        Button close = new Button(t("u.close"), e -> d.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(copy, close);
        d.open();
    }

    private static Component credLine(String label, String value) {
        Span l = new Span(label);
        l.addClassName("sh-cred-label");
        Span v = new Span(value);
        v.addClassName("sh-cred-value");
        Div d = new Div(l, v);
        d.addClassName("sh-cred-line");
        return d;
    }

    // ================================================================ boshqarish

    private void openChangeRole(Member m) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(m.fullName());
        dialog.setWidth("460px");
        dialog.setMaxWidth("calc(100vw - 24px)");

        Select<RoleCatalog.Role> role = new Select<>();
        role.setLabel(t("u.role"));
        role.setWidthFull();
        role.setItems(RoleCatalog.all());
        role.setItemLabelGenerator(RoleCatalog.Role::name);
        if (!m.roles().isEmpty()) {
            role.setValue(RoleCatalog.get(m.roles().get(0)));
        }
        Paragraph hint = new Paragraph(role.getValue() == null ? "" : role.getValue().description());
        hint.addClassName("sh-form-hint");
        role.addValueChangeListener(e -> hint.setText(e.getValue() == null ? "" : e.getValue().description()));
        dialog.add(new Div(role, hint));

        Button cancel = new Button(t("rec.cancel"), e -> dialog.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(t("rec.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            if (role.getValue() == null) {
                return;
            }
            String code = role.getValue().code();
            try {
                service.changeRole(company, m.id(), code, RoleCatalog.storedName(code),
                        RoleCatalog.storedDescription(code));
                dialog.close();
                Notification.show(t("rec.saved"), 2000, Notification.Position.BOTTOM_START);
                renderList();
            } catch (RuntimeException ex) {
                Notification.show(t("u.err.fail"), 4000, Notification.Position.TOP_CENTER);
            }
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void doReset(Member m) {
        try {
            String password = service.resetPassword(company, m.id());
            showCredentials(t("u.reset.title"), m.login(), password);
        } catch (RuntimeException ex) {
            Notification.show(t("u.err.fail"), 4000, Notification.Position.TOP_CENTER);
        }
    }

    private void doBlock(Member m, boolean blocked) {
        try {
            service.setBlocked(company, m.id(), blocked);
            Notification.show(t("rec.saved"), 1500, Notification.Position.BOTTOM_START);
            renderList();
        } catch (RuntimeException ex) {
            Notification.show(t("u.err.fail"), 4000, Notification.Position.TOP_CENTER);
        }
    }

    @Override
    public String getPageTitle() {
        return "Uz-mark ERP — " + t("u.title");
    }

    @SuppressWarnings("unused")
    private static UUID unused() {
        return null;
    }
}