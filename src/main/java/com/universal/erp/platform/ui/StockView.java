package com.universal.erp.platform.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Joriy qoldiq. Qo'lda o'zgartirilmaydi: kirim, chiqim, savdo va inventarizatsiyadan hosil bo'ladi.
 * Faqat minimal qoldiq (ogohlantirish chegarasi) tahrirlanadi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/StockView.java
 */
@Route(value = "app/inventory/stock", layout = MainLayout.class)
@CssImport("./styles/products.css")
@CssImport("./styles/inventory-ops.css")
@PermitAll
public class StockView extends OpsBase {

    public StockView(RecordStore store, StockLedger ledger, TenantModules tenantModules,
                     WarehouseProfiles profiles) {
        super("stock", store, ledger, tenantModules, profiles);
        init();
    }

    @Override
    protected boolean hasActions() {
        return true;
    }

    @Override
    protected List<Col> columns() {
        Map<String, String> units = productUnits();
        return List.of(
                new Col(L.t("Nomi", "Название", "Name"), r -> text(r.get("product")), false, 3),
                new Col(L.t("Ombor", "Склад", "Warehouse"), r -> text(r.get("warehouse")), false, 2),
                new Col(L.t("Qoldiq", "Остаток", "Stock"), r -> {
                    String u = units.getOrDefault(r.get("product").trim().toLowerCase(Locale.ROOT), "");
                    return text(FieldDef.format(FieldDef.parse(r.get("quantity"))) + (u.isEmpty() ? "" : " " + u));
                }, true, 2),
                new Col(L.t("Minimal", "Минимум", "Minimum"),
                        r -> text(r.get("min").isBlank() ? "—" : FieldDef.format(FieldDef.parse(r.get("min")))),
                        true, 1),
                new Col(L.t("Holat", "Статус", "Status"), this::status, false, 2));
    }

    private Component status(RecordStore.Rec r) {
        double q = FieldDef.parse(r.get("quantity"));
        double min = FieldDef.parse(r.get("min"));
        Span b;
        if (q <= 0) {
            b = new Span(L.t("Tugagan", "Закончился", "Out of stock"));
            b.addClassNames("ol-badge", "is-zero");
        } else if (min > 0 && q <= min) {
            b = new Span(L.t("Kam qoldi", "Мало", "Low"));
            b.addClassNames("ol-badge", "is-low");
        } else {
            b = new Span(L.t("Yetarli", "Достаточно", "OK"));
            b.addClassNames("ol-badge", "is-ok");
        }
        return b;
    }

    @Override
    protected Component rowActions(RecordStore.Rec r) {
        NativeButton edit = new NativeButton("✎");
        edit.addClassName("pl-icon-btn");
        edit.getElement().setAttribute("aria-label", L.t("Minimal qoldiq", "Минимальный остаток", "Minimum stock"));
        edit.getElement().setAttribute("title", L.t("Minimal qoldiq", "Минимальный остаток", "Minimum stock"));
        edit.addClickListener(e -> editMin(r));
        return edit;
    }

    private void editMin(RecordStore.Rec r) {
        Input min = input("number", "0", r.get("min"));
        Input wh = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), r.get("warehouse"));
        Div err = errorBox();
        Div body = formBody(
                field(L.t("Nomi", "Название", "Name"), false, readOnly(r.get("product"))),
                grid(field(L.t("Minimal qoldiq", "Минимальный остаток", "Minimum stock"), false, min),
                        field(L.t("Ombor", "Склад", "Warehouse"), false, wh)),
                err);

        NativeButton save = button(L.t("Saqlash", "Сохранить", "Save"), "pf-btn-primary");
        save.addClickListener(e -> {
            BigDecimal m = Dk.num(min.getValue());
            if (m != null && m.signum() < 0) {
                fail(err, L.t("Minimal qoldiq manfiy bo'lmasin", "Минимум не может быть отрицательным",
                        "Minimum cannot be negative"));
                return;
            }
            Map<String, String> d = new LinkedHashMap<>(r.data());
            d.put("min", m == null ? "" : Dk.plain(m));
            d.put("warehouse", wh.getValue() == null ? "" : wh.getValue().trim());
            try {
                store.save(company, section.key(), r.id(), d);
            } catch (RuntimeException ex) {
                fail(err, L.t("Saqlab bo'lmadi", "Не удалось сохранить", "Could not save"));
                return;
            }
            modalHost.removeAll();
            render();
        });
        showDialog(L.t("Minimal qoldiq", "Минимальный остаток", "Minimum stock"), body, footer(save));
    }

    private static Component readOnly(String value) {
        Input in = input("text", "", value);
        in.setReadOnly(true);
        return in;
    }
}