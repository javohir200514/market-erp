package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Ombor bo'limlari (Qoldiq, Kirim, Chiqim, Inventarizatsiya) uchun umumiy asos: sarlavha, qidiruv,
 * jadval, bo'sh holat va forma yordamchilari. Uslub products.css (pl-/pf-) + inventory-ops.css (ol-).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/OpsBase.java
 */
abstract class OpsBase extends Div implements BeforeEnterObserver, HasDynamicTitle {

    protected record Col(String label, Function<RecordStore.Rec, Component> cell, boolean num, int weight) {
    }

    protected final RecordStore store;
    protected final StockLedger ledger;
    protected final TenantModules tenantModules;
    protected final String company = CurrentCompany.get();
    protected final boolean canWrite = RolePolicy.canWrite(AppModule.INVENTORY);
    protected final WarehouseProfile profile;
    protected final AppSection section;

    private final Div list = new Div();
    private final Span count = new Span();
    private final Input search = new Input();
    protected final Div modalHost = new Div();
    private final Div titleHost = new Div();

    protected OpsBase(String slug, RecordStore store, StockLedger ledger, TenantModules tenantModules,
                      WarehouseProfiles profiles) {
        this.store = store;
        this.ledger = ledger;
        this.tenantModules = tenantModules;
        this.profile = profiles.forCompany(company);
        this.section = AppSection.get(AppModule.INVENTORY, slug);
        addClassName("pl-page");
    }

    // ---- subklass ta'riflaydi
    protected abstract List<Col> columns();

    protected boolean addEnabled() {
        return false;
    }

    protected String addLabel() {
        return "";
    }

    protected void openForm() {
    }

    protected Component rowActions(RecordStore.Rec r) {
        return null;
    }

    protected boolean hasActions() {
        return false;
    }

    /** Konstruktor oxirida chaqiriladi. */
    protected final void init() {
        H2 title = new H2(section.title());
        title.addClassName("pl-title");
        count.addClassName("pl-sub");
        Div titles = new Div(title, count);
        titles.addClassName("pl-titles");

        Div head = new Div(titles);
        head.addClassName("pl-head");
        if (addEnabled() && canWrite) {
            NativeButton add = new NativeButton("+  " + addLabel());
            add.addClassNames("pl-btn", "pl-btn-primary");
            add.addClickListener(e -> openForm());
            head.add(add);
        }

        search.setType("search");
        search.setPlaceholder(L.t("Qidirish", "Поиск", "Search"));
        search.addClassName("pl-search-input");
        search.getElement().setAttribute("autocomplete", "off");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> render());
        Div searchBox = new Div(HomeIcons.svg("search"), search);
        searchBox.addClassName("pl-search");
        Div toolbar = new Div(searchBox);
        toolbar.addClassName("pl-toolbar");

        add(head, toolbar, list, modalHost);
        render();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean enabled = !company.isEmpty() && tenantModules.enabled(company).contains(AppModule.INVENTORY);
        if (!enabled || !RolePolicy.canView(AppModule.INVENTORY)) {
            event.forwardTo("app");
        }
    }

    @Override
    public String getPageTitle() {
        return "Uz-mark ERP — " + section.title();
    }

    // ================================================================ jadval

    protected final void render() {
        list.removeAll();
        String q = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        List<RecordStore.Rec> all = new ArrayList<>(store.list(company, section.key()));
        if (section.field("date") != null) {
            all.sort(Comparator.comparing((RecordStore.Rec r) -> r.get("date")).reversed());
        } else {
            all.sort(Comparator.comparing((RecordStore.Rec r) -> r.get("product").toLowerCase(Locale.ROOT)));
        }

        List<RecordStore.Rec> rows = new ArrayList<>();
        for (RecordStore.Rec r : all) {
            if (q.isEmpty() || r.data().values().stream()
                    .anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(q))) {
                rows.add(r);
            }
        }
        count.setText(L.t("Jami: ", "Всего: ", "Total: ") + rows.size());

        if (rows.isEmpty()) {
            list.add(emptyState(all.isEmpty()));
            return;
        }

        List<Col> cols = columns();
        StringBuilder grid = new StringBuilder();
        for (Col c : cols) {
            grid.append("minmax(0,").append(c.weight()).append("fr) ");
        }
        if (hasActions() && canWrite) {
            grid.append("52px");
        }
        String template = grid.toString().trim();

        Div wrap = new Div();
        wrap.addClassName("ol-wrap");
        Div head = new Div();
        head.addClassNames("ol-row", "ol-head");
        head.getStyle().set("grid-template-columns", template);
        for (Col c : cols) {
            Span h = new Span(c.label());
            if (c.num()) {
                h.addClassName("is-num");
            }
            head.add(h);
        }
        if (hasActions() && canWrite) {
            head.add(new Span());
        }
        wrap.add(head);

        for (RecordStore.Rec r : rows) {
            Div row = new Div();
            row.addClassName("ol-row");
            row.getStyle().set("grid-template-columns", template);
            boolean first = true;
            for (Col c : cols) {
                Div cell = new Div(c.cell().apply(r));
                cell.addClassName("ol-cell");
                if (c.num()) {
                    cell.addClassName("is-num");
                }
                if (first) {
                    cell.addClassName("is-main");
                    first = false;
                }
                row.add(cell);
            }
            if (hasActions() && canWrite) {
                Component a = rowActions(r);
                row.add(a == null ? new Span() : a);
            }
            wrap.add(row);
        }
        list.add(wrap);
    }

    private Component emptyState(boolean noData) {
        Span icon = new Span(HomeIcons.svg("inventory"));
        icon.addClassName("pl-empty-icon");
        Span title = new Span(noData
                ? L.t("Hali yozuv yo'q", "Записей пока нет", "No records yet")
                : L.t("Hech narsa topilmadi", "Ничего не найдено", "Nothing found"));
        title.addClassName("pl-empty-title");
        Span text = new Span(noData
                ? L.t("Birinchi yozuv shu yerda ko'rinadi.", "Первая запись появится здесь.",
                        "Your first record will show up here.")
                : L.t("Qidiruvni o'zgartirib ko'ring.", "Измените поиск.", "Try changing the search."));
        text.addClassName("pl-empty-text");
        Div box = new Div(icon, title, text);
        box.addClassName("pl-empty");
        if (noData && addEnabled() && canWrite) {
            NativeButton add = new NativeButton("+  " + addLabel());
            add.addClassNames("pl-btn", "pl-btn-primary");
            add.addClickListener(e -> openForm());
            box.add(add);
        }
        return box;
    }

    // ================================================================ mahsulotlar

    private List<RecordStore.Rec> products() {
        return store.list(company, "inventory/products");
    }

    /** Ro'yxatdagi mahsulotning asl nomi (katta-kichik harfga e'tiborsiz) yoki null. */
    protected final String canonicalProduct(String typed) {
        String t = typed == null ? "" : typed.trim();
        for (RecordStore.Rec r : products()) {
            if (r.get("name").trim().equalsIgnoreCase(t)) {
                return r.get("name").trim();
            }
        }
        return null;
    }

    /** Mahsulot nomi -> o'lchov birligi yorlig'i. */
    protected final Map<String, String> productUnits() {
        Map<String, String> m = new LinkedHashMap<>();
        for (RecordStore.Rec r : products()) {
            String u = r.get("unit");
            if (!u.isBlank()) {
                String label = u;
                for (WarehouseProfile.Unit x : WarehouseProfile.Unit.values()) {
                    if (unitKey(x).equals(u)) {
                        label = x.label(AppTexts.lang());
                    }
                }
                m.put(r.get("name").trim().toLowerCase(Locale.ROOT), label);
            }
        }
        return m;
    }

    private static String unitKey(WarehouseProfile.Unit u) {
        return switch (u) {
            case PIECE -> "pcs";
            case PAIR -> "pair";
            case PACK -> "pack";
            case BOX -> "box";
            case BAG -> "bag";
            case KG -> "kg";
            case GRAM -> "g";
            case LITER -> "l";
            case ML -> "ml";
            case METER -> "m";
        };
    }

    /** Qidiruvli mahsulot maydoni (brauzer datalist). Ro'yxatdan boshqa nom saqlashda rad etiladi. */
    protected final Component productPicker(Input input, String listId) {
        input.getElement().setAttribute("list", listId);
        input.getElement().setAttribute("autocomplete", "off");
        StringBuilder opts = new StringBuilder("<datalist id='" + listId + "'>");
        for (RecordStore.Rec r : products()) {
            opts.append("<option value=\"").append(escape(r.get("name"))).append("\"></option>");
        }
        opts.append("</datalist>");
        Div box = new Div(input, new Html(opts.toString()));
        box.addClassName("ol-picker");
        return box;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    protected static Span text(String s) {
        return new Span(s == null ? "" : s);
    }

    protected static String today() {
        return LocalDate.now().toString();
    }

    // ================================================================ forma yordamchilari

    protected final void showDialog(String title, Div body, Div foot) {
        modalHost.removeAll();
        Span t = new Span(title);
        t.addClassName("pf-dialog-title");
        NativeButton close = new NativeButton("✕");
        close.addClassName("pf-close");
        close.getElement().setAttribute("aria-label", L.t("Yopish", "Закрыть", "Close"));
        close.addClickListener(e -> modalHost.removeAll());
        Div head = new Div(t, close);
        head.addClassName("pf-dialog-head");

        Div dialog = new Div(head, body, foot);
        dialog.addClassName("pf-dialog");
        dialog.getElement().setAttribute("role", "dialog");
        dialog.getElement().setAttribute("aria-modal", "true");
        Div overlay = new Div(dialog);
        overlay.addClassName("pf-overlay");
        modalHost.add(overlay);
    }

    protected static Div formBody(Component... parts) {
        Div body = new Div();
        body.addClassName("pf-dialog-body");
        Div sec = new Div();
        sec.addClassNames("pf-section");
        for (Component c : parts) {
            sec.add(c);
        }
        body.add(sec);
        return body;
    }

    protected static Div grid(Component... fields) {
        Div g = new Div(fields);
        g.addClassName("pf-grid");
        return g;
    }

    protected static Div field(String label, boolean required, Component control) {
        Span l = new Span(label);
        l.addClassName("pf-label");
        if (required) {
            Span star = new Span(" *");
            star.addClassName("pf-star");
            l.add(star);
        }
        Div f = new Div(l, control);
        f.addClassName("pf-field");
        return f;
    }

    protected static Input input(String type, String placeholder, String value) {
        Input in = new Input();
        in.setType(type);
        in.setPlaceholder(placeholder == null ? "" : placeholder);
        in.setValue(value == null ? "" : value);
        in.addClassName("pf-input");
        if ("number".equals(type)) {
            in.getElement().setAttribute("step", "any");
            in.getElement().setAttribute("min", "0");
            in.getElement().setAttribute("inputmode", "decimal");
        }
        return in;
    }

    protected static NativeButton button(String text, String cls) {
        NativeButton b = new NativeButton(text);
        b.addClassNames("pf-btn", cls);
        return b;
    }

    protected static Div errorBox() {
        Div err = new Div();
        err.addClassName("pf-error");
        err.setVisible(false);
        return err;
    }

    protected static void fail(Div err, String message) {
        err.setText(message);
        err.setVisible(true);
    }

    protected final Div footer(NativeButton save) {
        NativeButton cancel = button(L.t("Bekor qilish", "Отмена", "Cancel"), "pf-btn-ghost");
        cancel.addClickListener(e -> modalHost.removeAll());
        Div foot = new Div(cancel, save);
        foot.addClassName("pf-dialog-foot");
        return foot;
    }

    protected final Div chips(Map<String, String> options, String[] sel) {
        Div box = new Div();
        box.addClassName("pf-chips");
        List<NativeButton> all = new ArrayList<>();
        for (Map.Entry<String, String> en : options.entrySet()) {
            NativeButton b = new NativeButton(en.getValue());
            b.addClassName("pf-chip");
            b.setClassName("is-active", en.getKey().equals(sel[0]));
            b.addClickListener(e -> {
                sel[0] = en.getKey();
                all.forEach(x -> x.removeClassName("is-active"));
                b.addClassName("is-active");
            });
            all.add(b);
            box.add(b);
        }
        return box;
    }

    protected static String qtyText(java.math.BigDecimal q) {
        return FieldDef.format(q.doubleValue());
    }
}