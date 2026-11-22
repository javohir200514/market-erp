package com.universal.erp.platform.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chiqim: sotuv, ichki sarf, hisobdan chiqarish. Saqlanganda qoldiq kamayadi,
 * qoldiqdan ortiq chiqimga yo'l qo'yilmaydi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/IssuesView.java
 */
@Route(value = "app/inventory/issues", layout = MainLayout.class)
@CssImport("./styles/products.css")
@CssImport("./styles/inventory-ops.css")
@PermitAll
public class IssuesView extends OpsBase {

    public IssuesView(RecordStore store, StockLedger ledger, TenantModules tenantModules,
                      WarehouseProfiles profiles) {
        super("issues", store, ledger, tenantModules, profiles);
        init();
    }

    private static Map<String, String> reasons() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("sale", L.t("Sotuv", "Продажа", "Sale"));
        m.put("internal", L.t("Ichki sarf", "Внутренний расход", "Internal use"));
        m.put("writeoff", L.t("Hisobdan chiqarish", "Списание", "Write-off"));
        return m;
    }

    @Override
    protected boolean addEnabled() {
        return true;
    }

    @Override
    protected String addLabel() {
        return L.t("Chiqim qo'shish", "Добавить расход", "Add issue");
    }

    @Override
    protected List<Col> columns() {
        Map<String, String> units = productUnits();
        Map<String, String> reasons = reasons();
        return List.of(
                new Col(L.t("Sana", "Дата", "Date"), r -> text(r.get("date")), false, 1),
                new Col(L.t("Nomi", "Название", "Name"), r -> text(r.get("product")), false, 3),
                new Col(L.t("Miqdor", "Количество", "Quantity"), r -> {
                    String u = units.getOrDefault(r.get("product").trim().toLowerCase(Locale.ROOT), "");
                    return text(FieldDef.format(FieldDef.parse(r.get("quantity"))) + (u.isEmpty() ? "" : " " + u));
                }, true, 1),
                new Col(L.t("Sabab", "Причина", "Reason"),
                        r -> text(reasons.getOrDefault(r.get("reason"), r.get("reason"))), false, 2),
                new Col(L.t("Mijoz", "Клиент", "Customer"),
                        r -> text(r.get("customer").isBlank() ? "—" : r.get("customer")), false, 2));
    }

    @Override
    protected void openForm() {
        if (!canWrite) {
            return;
        }
        Input date = input("date", "", today());
        Input product = input("text", L.t("Ro'yxatdan tanlang", "Выберите из списка", "Pick from the list"), "");
        Input qty = input("number", "0", "");
        Input customer = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), "");
        String[] reason = {"sale"};
        Div err = errorBox();

        Div body = formBody(
                field(L.t("Nomi", "Название", "Name"), true, productPicker(product, "ol-products-i")),
                grid(field(L.t("Sana", "Дата", "Date"), true, date),
                        field(L.t("Miqdor", "Количество", "Quantity"), true, qty)),
                field(L.t("Sabab", "Причина", "Reason"), false, chips(reasons(), reason)),
                field(L.t("Mijoz", "Клиент", "Customer"), false, customer),
                err);

        NativeButton save = button(L.t("Saqlash", "Сохранить", "Save"), "pf-btn-primary");
        save.addClickListener(e -> {
            err.setVisible(false);
            String name = canonicalProduct(product.getValue());
            BigDecimal q = Dk.num(qty.getValue());
            String d = date.getValue() == null ? "" : date.getValue().trim();

            if (name == null) {
                fail(err, L.t("Mahsulotni ro'yxatdan tanlang", "Выберите товар из списка", "Pick a product from the list"));
                return;
            }
            if (q == null || q.signum() <= 0) {
                fail(err, L.t("Miqdor 0 dan katta bo'lsin", "Количество должно быть больше 0",
                        "Quantity must be greater than 0"));
                return;
            }
            try {
                LocalDate.parse(d);
            } catch (DateTimeParseException ex) {
                fail(err, L.t("Sanani to'g'ri kiriting", "Введите корректную дату", "Enter a valid date"));
                return;
            }

            // avval qoldiqni kamaytiramiz (yetmasa xato), keyin yozuvni saqlaymiz
            try {
                ledger.apply(company, name, q.negate(), false);
            } catch (IllegalStateException ex) {
                BigDecimal have = ledger.quantity(company, name);
                fail(err, L.t("Qoldiq yetarli emas. Mavjud: ", "Недостаточно остатка. Есть: ",
                        "Not enough stock. Available: ") + qtyText(have));
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("date", d);
            data.put("customer", customer.getValue() == null ? "" : customer.getValue().trim());
            data.put("product", name);
            data.put("quantity", Dk.plain(q));
            data.put("reason", reason[0]);
            try {
                store.save(company, section.key(), null, data);
            } catch (RuntimeException ex) {
                ledger.apply(company, name, q, true); // qoldiqni qaytaramiz
                fail(err, L.t("Saqlab bo'lmadi", "Не удалось сохранить", "Could not save"));
                return;
            }
            modalHost.removeAll();
            render();
        });

        showDialog(L.t("Yangi chiqim", "Новый расход", "New issue"), body, footer(save));
    }
}