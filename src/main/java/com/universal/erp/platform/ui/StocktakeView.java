package com.universal.erp.platform.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventarizatsiya: haqiqiy sanalgan qoldiq hisobdagi bilan solishtiriladi. Saqlanganda farq yoziladi va
 * qoldiq haqiqiy songa tenglanadi. Kirim/chiqimdagi xatolar shu yerda tuzatiladi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/StocktakeView.java
 */
@Route(value = "app/inventory/stocktake", layout = MainLayout.class)
@CssImport("./styles/products.css")
@CssImport("./styles/inventory-ops.css")
@PermitAll
public class StocktakeView extends OpsBase {

    public StocktakeView(RecordStore store, StockLedger ledger, TenantModules tenantModules,
                         WarehouseProfiles profiles) {
        super("stocktake", store, ledger, tenantModules, profiles);
        init();
    }

    @Override
    protected boolean addEnabled() {
        return true;
    }

    @Override
    protected String addLabel() {
        return L.t("Inventarizatsiya", "Инвентаризация", "New stocktake");
    }

    @Override
    protected List<Col> columns() {
        return List.of(
                new Col(L.t("Sana", "Дата", "Date"), r -> text(r.get("date")), false, 1),
                new Col(L.t("Nomi", "Название", "Name"), r -> text(r.get("product")), false, 3),
                new Col(L.t("Hisobda", "По учёту", "Recorded"),
                        r -> text(FieldDef.format(FieldDef.parse(r.get("recorded")))), true, 1),
                new Col(L.t("Haqiqatda", "Фактически", "Actual"),
                        r -> text(FieldDef.format(FieldDef.parse(r.get("actual")))), true, 1),
                new Col(L.t("Farq", "Разница", "Difference"), r -> {
                    double diff = FieldDef.parse(r.get("actual")) - FieldDef.parse(r.get("recorded"));
                    Span s = new Span((diff > 0 ? "+" : "") + FieldDef.format(diff));
                    s.addClassNames("ol-badge", diff == 0 ? "is-ok" : diff < 0 ? "is-zero" : "is-low");
                    return s;
                }, true, 1),
                new Col(L.t("Izoh", "Примечание", "Note"),
                        r -> text(r.get("note").isBlank() ? "—" : r.get("note")), false, 2));
    }

    @Override
    protected void openForm() {
        if (!canWrite) {
            return;
        }
        Input date = input("date", "", today());
        Input product = input("text", L.t("Ro'yxatdan tanlang", "Выберите из списка", "Pick from the list"), "");
        product.setValueChangeMode(ValueChangeMode.LAZY);
        Input actual = input("number", "0", "");
        Input warehouse = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), "");
        Input note = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), "");

        Span hint = new Span();
        hint.addClassName("ol-hint");
        product.addValueChangeListener(e -> {
            String name = canonicalProduct(e.getValue());
            hint.setText(name == null ? ""
                    : L.t("Hisobdagi qoldiq: ", "Остаток по учёту: ", "Recorded stock: ")
                            + qtyText(ledger.quantity(company, name)));
        });
        Div err = errorBox();

        Div body = formBody(
                field(L.t("Nomi", "Название", "Name"), true, productPicker(product, "ol-products-s")),
                hint,
                grid(field(L.t("Sana", "Дата", "Date"), true, date),
                        field(L.t("Haqiqiy qoldiq", "Фактический остаток", "Actual stock"), true, actual)),
                grid(field(L.t("Ombor", "Склад", "Warehouse"), false, warehouse),
                        field(L.t("Izoh", "Примечание", "Note"), false, note)),
                err);

        NativeButton save = button(L.t("Tasdiqlash", "Подтвердить", "Confirm"), "pf-btn-primary");
        save.addClickListener(e -> {
            err.setVisible(false);
            String name = canonicalProduct(product.getValue());
            BigDecimal act = Dk.num(actual.getValue());
            String d = date.getValue() == null ? "" : date.getValue().trim();

            if (name == null) {
                fail(err, L.t("Mahsulotni ro'yxatdan tanlang", "Выберите товар из списка", "Pick a product from the list"));
                return;
            }
            if (act == null || act.signum() < 0) {
                fail(err, L.t("Haqiqiy qoldiqni kiriting (0 yoki ko'p)", "Введите фактический остаток (0 или больше)",
                        "Enter the actual stock (0 or more)"));
                return;
            }
            try {
                LocalDate.parse(d);
            } catch (DateTimeParseException ex) {
                fail(err, L.t("Sanani to'g'ri kiriting", "Введите корректную дату", "Enter a valid date"));
                return;
            }

            BigDecimal recorded = ledger.quantity(company, name);
            Map<String, String> data = new LinkedHashMap<>();
            data.put("date", d);
            data.put("warehouse", warehouse.getValue() == null ? "" : warehouse.getValue().trim());
            data.put("product", name);
            data.put("recorded", Dk.plain(recorded));
            data.put("actual", Dk.plain(act));
            data.put("note", note.getValue() == null ? "" : note.getValue().trim());
            try {
                store.save(company, section.key(), null, data);
                ledger.apply(company, name, act.subtract(recorded), true);
            } catch (RuntimeException ex) {
                fail(err, L.t("Saqlab bo'lmadi", "Не удалось сохранить", "Could not save"));
                return;
            }
            modalHost.removeAll();
            render();
        });

        showDialog(L.t("Yangi inventarizatsiya", "Новая инвентаризация", "New stocktake"), body, footer(save));
    }
}