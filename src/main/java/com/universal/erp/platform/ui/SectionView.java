package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Route(value = "app/:module/:section", layout = MainLayout.class)
@PermitAll
public class SectionView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private final TenantModules tenantModules;
    private final RecordStore store;

    private AppSection section;
    private boolean canWrite;
    private String company = "";
    private String query = "";
    private String filterValue = "";
    private String sortKey = "";
    private boolean sortAsc = true;
    private Div listHost;

    public SectionView(TenantModules tenantModules, RecordStore store) {
        this.tenantModules = tenantModules;
        this.store = store;
    }

    private static String tx(String key, String fallback) {
        String v = t(key);
        return (v == null || v.isBlank() || v.equals(key)) ? fallback : v;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        company = CurrentCompany.get();
        Optional<AppModule> m = AppModule.byKey(event.getRouteParameters().get("module").orElse(""));
        if (m.isEmpty() || !tenantModules.enabled(company).contains(m.get()) || !RolePolicy.canView(m.get())) {
            event.forwardTo(DashboardView.class);
            return;
        }
        Optional<AppSection> s = AppSection.find(m.get(), event.getRouteParameters().get("section").orElse(""));
        if (s.isEmpty()) {
            event.forwardTo(ModuleView.class, new RouteParameters("key", m.get().key()));
            return;
        }
        section = s.get();
        canWrite = RolePolicy.canWrite(m.get());
        query = "";
        filterValue = "";
        sortKey = "";
        sortAsc = true;
        build();
    }

    private void build() {
        removeAll();
        addClassName("sh-page");
        AppModule module = section.module();

        Anchor up = new Anchor(module.route(), module.title());
        Span sep = new Span();
        sep.addClassName("sh-chev-r");
        Span here = new Span(section.title());
        Div crumbs = new Div(up, sep, here);
        crumbs.addClassName("sh-crumbs");

        H1 title = new H1(section.title());
        title.addClassName("sh-page-title");
        Paragraph sub = new Paragraph(section.description());
        sub.addClassName("sh-page-sub");
        Div titleBox = new Div(title, sub);
        titleBox.addClassName("sh-title-box");

        Div header = new Div(titleBox);
        header.addClassName("sh-header");
        if (canWrite && !section.isReport()) {
            header.add(createButton());
        }

        boolean empty = !section.isReport() && store.list(company, section.key()).isEmpty();

        // SectionTabs olib tashlandi: bo'limlar chap menyuda bor.
        add(crumbs, header, guide(section.isReport() || empty));

        if (section.isReport()) {
            add(new ReportPanel(store, company).build(section));
            return;
        }

        Input search = new Input();
        search.setType("search");
        search.setPlaceholder(t("rec.search"));
        search.getElement().setAttribute("aria-label", t("rec.search"));
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> {
            query = e.getValue() == null ? "" : e.getValue().trim().toLowerCase(Locale.ROOT);
            renderList();
        });
        Div searchBox = new Div(search);
        searchBox.addClassName("sh-search-box");

        Div toolbar = new Div(searchBox);
        toolbar.addClassName("sh-toolbar");

        listHost = new Div();
        listHost.addClassName("sh-list-host");

        Div panel = new Div(toolbar, listHost);
        panel.addClassName("sh-panel");
        add(panel);
        renderList();
    }

    private NativeButton createButton() {
        NativeButton create = new NativeButton();
        Span plus = new Span();
        plus.addClassName("sh-plus-ico");
        plus.getElement().setAttribute("aria-hidden", "true");
        create.add(plus, new Span(t("shell.sec.new")));
        create.addClassName("sh-btn");
        create.addClickListener(e -> openForm(null));
        return create;
    }

    private Component guide(boolean open) {
        String[] g = SectionGuide.of(section);

        Span icon = new Span();
        icon.addClassName("sh-guide-i");
        icon.getElement().setAttribute("aria-hidden", "true");
        Span label = new Span(t("guide.title"));
        label.addClassName("sh-guide-title");
        Span chev = new Span();
        chev.addClassName("sh-chev");
        chev.getElement().setAttribute("aria-hidden", "true");
        NativeButton toggle = new NativeButton();
        toggle.add(icon, label, chev);
        toggle.addClassName("sh-guide-toggle");

        Div body = new Div(line(t("guide.what"), g[0]));
        if (g.length > 1 && !g[1].isBlank()) {
            body.add(line(t("guide.enter"), g[1]));
        }
        if (g.length > 2 && !g[2].isBlank()) {
            body.add(line(t("guide.use"), g[2]));
        }
        body.addClassName("sh-guide-body");

        Div box = new Div(toggle, body);
        box.addClassName("sh-guide");
        box.setClassName("is-open", open);
        toggle.addClickListener(e -> box.setClassName("is-open", !box.hasClassName("is-open")));
        return box;
    }

    private static Component line(String label, String text) {
        Span l = new Span(label);
        l.addClassName("sh-guide-label");
        Span v = new Span(text);
        v.addClassName("sh-guide-text");
        Div d = new Div(l, v);
        d.addClassName("sh-guide-line");
        return d;
    }

    /** Birinchi ustun kengroq, qolganlari teng. Kassir/raqam sinib ketmasligi uchun. */
    private String gridTemplate(int colCount) {
        String cols;
        if (colCount <= 1) {
            cols = "minmax(0, 1fr)";
        } else {
            cols = "minmax(170px, 1.6fr) repeat(" + (colCount - 1) + ", minmax(0, 1fr))";
        }
        return cols + (canWrite ? " 96px" : "");
    }

    private void renderList() {
        listHost.removeAll();
        List<RecordStore.Rec> all = store.list(company, section.key());

        if (all.isEmpty()) {
            listHost.add(emptyState(t("rec.empty.t"), t("rec.empty.d"), canWrite));
            return;
        }

        FieldDef filterField = section.fields().stream()
                .filter(fd -> fd.type() == FieldDef.Type.SELECT)
                .findFirst()
                .orElse(null);

        List<RecordStore.Rec> rows = all.stream()
                .filter(this::matches)
                .filter(r -> filterField == null || filterValue.isEmpty()
                        || filterValue.equalsIgnoreCase(r.get(filterField.key())))
                .sorted(comparator())
                .toList();

        listHost.add(stats(rows, all.size()));
        if (filterField != null) {
            listHost.add(chips(filterField, all));
        }

        if (rows.isEmpty()) {
            listHost.add(emptyState(t("rec.nomatch"), "", false));
            return;
        }

        List<FieldDef> cols = section.fields().stream()
                .filter(fd -> fd.type() != FieldDef.Type.LONG)
                .toList();
        String grid = gridTemplate(cols.size());

        Div head = new Div();
        head.addClassNames("sh-row", "sh-row-head");
        head.getStyle().set("grid-template-columns", grid);
        for (FieldDef fd : cols) {
            String arrow = fd.key().equals(sortKey) ? (sortAsc ? "  ▲" : "  ▼") : "";
            Span h = new Span(fd.label() + arrow);
            h.addClassName("is-sortable");
            if (fd.numeric()) {
                h.addClassName("is-num");
            }
            h.addClickListener(e -> {
                if (fd.key().equals(sortKey)) {
                    sortAsc = !sortAsc;
                } else {
                    sortKey = fd.key();
                    sortAsc = true;
                }
                renderList();
            });
            head.add(h);
        }
        if (canWrite) {
            head.add(new Span());
        }
        listHost.add(head);

        for (RecordStore.Rec r : rows) {
            listHost.add(row(r, cols, grid));
        }
    }

    private Comparator<RecordStore.Rec> comparator() {
        if (sortKey.isEmpty()) {
            return (a, b) -> 0;
        }
        FieldDef fd = section.fields().stream()
                .filter(x -> x.key().equals(sortKey))
                .findFirst()
                .orElse(null);
        if (fd == null) {
            return (a, b) -> 0;
        }
        Comparator<RecordStore.Rec> c = fd.numeric()
                ? Comparator.comparingDouble(r -> FieldDef.parse(r.get(fd.key())))
                : Comparator.comparing(r -> fd.display(r.get(fd.key())).toLowerCase(Locale.ROOT));
        return sortAsc ? c : c.reversed();
    }

    private boolean matches(RecordStore.Rec r) {
        if (query.isEmpty()) {
            return true;
        }
        for (FieldDef fd : section.fields()) {
            String shown = fd.display(r.get(fd.key())).toLowerCase(Locale.ROOT);
            if (shown.contains(query) || r.get(fd.key()).toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private Component stats(List<RecordStore.Rec> rows, int totalCount) {
        Div box = new Div();
        box.addClassName("sh-stats");
        box.add(statCard(tx("rec.stat.count", "Yozuvlar"),
                String.valueOf(rows.size()), rows.size() == totalCount ? "" : "/ " + totalCount));
        String key = section.sumKey();
        if (key != null) {
            double total = rows.stream().mapToDouble(r -> FieldDef.parse(r.get(key))).sum();
            box.add(statCard(tx("rec.stat.total", "Jami"), FieldDef.format(total), t("rec.cur")));
            double avg = rows.isEmpty() ? 0 : total / rows.size();
            box.add(statCard(tx("rec.stat.avg", "O'rtacha"), FieldDef.format(avg), t("rec.cur")));
        }
        return box;
    }

    private static Component statCard(String label, String value, String unit) {
        Span l = new Span(label);
        l.addClassName("sh-stat-label");
        Span v = new Span(value);
        v.addClassName("sh-stat-value");
        Span u = new Span(unit);
        u.addClassName("sh-stat-unit");
        Div valueLine = new Div(v, u);
        valueLine.addClassName("sh-stat-line");
        Div card = new Div(l, valueLine);
        card.addClassName("sh-stat");
        return card;
    }

    private Component chips(FieldDef fd, List<RecordStore.Rec> all) {
        Div bar = new Div();
        bar.addClassName("sh-chips");
        bar.add(chip(tx("rec.all", "Hammasi"), "", all.size()));
        for (FieldDef.Opt o : fd.options()) {
            long n = all.stream().filter(r -> o.key().equalsIgnoreCase(r.get(fd.key()))).count();
            if (n > 0) {
                bar.add(chip(o.label(), o.key(), n));
            }
        }
        return bar;
    }

    private Component chip(String label, String value, long count) {
        NativeButton b = new NativeButton(label + "  " + count);
        b.addClassName("sh-chip");
        b.setClassName("is-active", filterValue.equalsIgnoreCase(value));
        b.addClickListener(e -> {
            filterValue = value;
            renderList();
        });
        return b;
    }

    private Component row(RecordStore.Rec r, List<FieldDef> cols, String grid) {
        Div row = new Div();
        row.addClassName("sh-row");
        row.getStyle().set("grid-template-columns", grid);

        for (FieldDef fd : cols) {
            String raw = r.get(fd.key());
            String shown = fd.display(raw);
            Span value = new Span(shown);
            Div cell = new Div();
            cell.addClassName("sh-cell");
            cell.getElement().setAttribute("data-label", fd.label());

            if (fd == cols.get(0)) {
                String letter = shown.isBlank() ? "•" : shown.substring(0, 1).toUpperCase(Locale.ROOT);
                Span av = new Span(letter);
                av.addClassName("sh-avatar");
                cell.add(av);
                cell.addClassName("is-main");
                // raqam (S-202611001) bir qatorda turishi uchun
                value.getStyle().set("white-space", "nowrap");
            }
            cell.add(value);

            if (fd.type() == FieldDef.Type.SELECT && !raw.isBlank()) {
                value.addClassNames("sh-badge", "sh-badge-" + raw.toLowerCase(Locale.ROOT));
            }
            if (fd.numeric()) {
                cell.addClassName("is-num");
            }
            row.add(cell);
        }

        if (canWrite) {
            NativeButton edit = iconButton(t("rec.edit"), false);
            edit.addClickListener(e -> openForm(r));
            NativeButton del = iconButton(t("rec.delete"), true);
            del.addClickListener(e -> confirmDelete(r, cols.get(0)));
            Div actions = new Div(edit, del);
            actions.addClassName("sh-row-actions");
            row.add(actions);
        }
        return row;
    }

    private static NativeButton iconButton(String label, boolean danger) {
        NativeButton b = new NativeButton(label);
        b.addClassName("sh-icon-action");
        if (danger) {
            b.addClassName("is-danger");
        }
        b.getElement().setAttribute("aria-label", label);
        b.getElement().setAttribute("title", label);
        return b;
    }

    private Component emptyState(String title, String text, boolean withButton) {
        Div ico = new Div();
        ico.addClassName("sh-empty-ico");
        ico.getElement().setAttribute("aria-hidden", "true");
        H3 h = new H3(title);
        Div box = new Div(ico, h);
        box.addClassName("sh-empty-box");
        if (!text.isBlank()) {
            box.add(new Paragraph(text));
        }
        if (withButton) {
            box.add(createButton());
        }
        return box;
    }

    private void openForm(RecordStore.Rec existing) {
        if (!canWrite) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(section.title() + " — " + t(existing == null ? "rec.add" : "rec.edit"));
        dialog.setWidth("620px");
        dialog.setMaxWidth("calc(100vw - 24px)");
        dialog.setCloseOnOutsideClick(false);
        dialog.addClassName("sh-dialog");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("480px", 2));

        Map<FieldDef, HasValue<?, ?>> inputs = new LinkedHashMap<>();
        for (FieldDef fd : section.fields()) {
            Component c = createInput(fd, existing == null ? "" : existing.get(fd.key()));
            inputs.put(fd, (HasValue<?, ?>) c);
            form.add(c);
            if (fd.type() == FieldDef.Type.LONG) {
                form.setColspan(c, 2);
            }
        }
        dialog.add(form);

        Button cancel = new Button(t("rec.cancel"), e -> dialog.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(t("rec.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            Map<String, String> data = collect(inputs);
            if (data == null) {
                return;
            }
            try {
                store.save(company, section.key(), existing == null ? null : existing.id(), data);
            } catch (RuntimeException ex) {
                Notification.show(t("rec.err.save"), 4000, Notification.Position.TOP_CENTER);
                return;
            }
            dialog.close();
            Notification.show(t("rec.saved"), 2000, Notification.Position.BOTTOM_START);
            renderList();
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private Component createInput(FieldDef fd, String value) {
        switch (fd.type()) {
            case LONG -> {
                TextArea a = new TextArea(fd.label());
                a.setMaxLength(1000);
                a.setValue(value);
                a.setMinHeight("90px");
                return a;
            }
            case NUMBER, MONEY -> {
                NumberField n = new NumberField(fd.label());
                n.setStepButtonsVisible(false);
                n.setMin(0);
                if (fd.type() == FieldDef.Type.MONEY) {
                    n.setSuffixComponent(new Span(t("rec.cur")));
                }
                if (!value.isBlank()) {
                    n.setValue(FieldDef.parse(value));
                }
                n.setRequiredIndicatorVisible(fd.required());
                return n;
            }
            case DATE -> {
                DatePicker d = new DatePicker(fd.label());
                try {
                    if (!value.isBlank()) {
                        d.setValue(LocalDate.parse(value));
                    }
                } catch (RuntimeException ignored) {
                }
                d.setRequiredIndicatorVisible(fd.required());
                return d;
            }
            case SELECT -> {
                Select<FieldDef.Opt> s = new Select<>();
                s.setLabel(fd.label());
                s.setItems(fd.options());
                s.setItemLabelGenerator(FieldDef.Opt::label);
                if (!value.isBlank()) {
                    s.setValue(fd.option(value));
                } else if (!fd.required() && !fd.options().isEmpty()) {
                    s.setValue(fd.options().get(0));
                }
                s.setRequiredIndicatorVisible(fd.required());
                return s;
            }
            default -> {
                TextField tf = new TextField(fd.label());
                tf.setMaxLength(200);
                tf.setValue(value);
                tf.setRequiredIndicatorVisible(fd.required());
                return tf;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, String> collect(Map<FieldDef, HasValue<?, ?>> inputs) {
        Map<String, String> data = new LinkedHashMap<>();
        boolean ok = true;
        for (Map.Entry<FieldDef, HasValue<?, ?>> en : inputs.entrySet()) {
            FieldDef fd = en.getKey();
            Object v = en.getValue().getValue();
            String text = "";
            if (v instanceof Double d) {
                text = BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            } else if (v instanceof LocalDate d) {
                text = d.toString();
            } else if (v instanceof FieldDef.Opt o) {
                text = o.key();
            } else if (v instanceof String s) {
                text = s.trim();
            }
            if (fd.required() && text.isBlank()) {
                ok = false;
                if (en.getValue() instanceof HasValidation hv) {
                    hv.setInvalid(true);
                    hv.setErrorMessage(t("rec.required"));
                }
            }
            data.put(fd.key(), text);
        }
        return ok ? data : null;
    }

    private void confirmDelete(RecordStore.Rec r, FieldDef mainField) {
        if (!canWrite) {
            return;
        }
        Dialog d = new Dialog();
        d.setHeaderTitle(t("rec.confirm.t"));
        d.setWidth("420px");
        d.setMaxWidth("calc(100vw - 24px)");
        Span name = new Span(mainField.display(r.get(mainField.key())));
        name.addClassName("sh-del-name");
        d.add(name, new Paragraph(t("rec.confirm.d")));
        Button cancel = new Button(t("rec.cancel"), e -> d.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button del = new Button(t("rec.delete"));
        del.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        del.addClickListener(e -> {
            try {
                store.delete(company, section.key(), r.id());
            } catch (RuntimeException ex) {
                Notification.show(t("rec.err.save"), 4000, Notification.Position.TOP_CENTER);
                return;
            }
            d.close();
            Notification.show(t("rec.deleted"), 2000, Notification.Position.BOTTOM_START);
            renderList();
        });
        d.getFooter().add(cancel, del);
        d.open();
    }

    @Override
    public String getPageTitle() {
        return section == null ? "DOKONIX" : "DOKONIX — " + section.title();
    }
}