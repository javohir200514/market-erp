package com.universal.erp.platform.ui;

import com.universal.erp.platform.ui.WarehouseProfile.Field;
import com.universal.erp.platform.ui.WarehouseProfile.Unit;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Kirim: yetkazib beruvchidan kelgan tovar. Saqlanganda qoldiq oshadi.
 * Do'kon turiga qarab partiya raqami va yaroqlilik muddati chiqadi.
 * Yozuvlar tahrirlanmaydi va o'chirilmaydi: tuzatish inventarizatsiya orqali.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/ReceiptsView.java
 */
@Route(value = "app/inventory/receipts", layout = MainLayout.class)
@CssImport("./styles/products.css")
@CssImport("./styles/inventory-ops.css")
@PermitAll
public class ReceiptsView extends OpsBase {

    public ReceiptsView(RecordStore store, StockLedger ledger, TenantModules tenantModules,
                        WarehouseProfiles profiles) {
        super("receipts", store, ledger, tenantModules, profiles);
        init();
    }

    @Override
    protected boolean addEnabled() {
        return true;
    }

    @Override
    protected String addLabel() {
        return L.t("Kirim qo'shish", "Добавить приход", "Add receipt");
    }

    @Override
    protected List<Col> columns() {
        Map<String, String> units = productUnits();
        List<Col> cols = new ArrayList<>();
        cols.add(new Col(L.t("Sana", "Дата", "Date"), r -> text(r.get("date")), false, 1));
        cols.add(new Col(L.t("Nomi", "Название", "Name"), r -> text(r.get("product")), false, 3));
        cols.add(new Col(L.t("Miqdor", "Количество", "Quantity"), r -> {
            String u = units.getOrDefault(r.get("product").trim().toLowerCase(Locale.ROOT), "");
            return text(FieldDef.format(FieldDef.parse(r.get("quantity"))) + (u.isEmpty() ? "" : " " + u));
        }, true, 1));
        cols.add(new Col(L.t("Kirim narxi", "Цена прихода", "Receipt price"),
                r -> text(r.get("price").isBlank() ? "—" : FieldDef.format(FieldDef.parse(r.get("price")))),
                true, 1));
        cols.add(new Col(L.t("Yetkazib beruvchi", "Поставщик", "Supplier"),
                r -> text(r.get("supplier").isBlank() ? "—" : r.get("supplier")), false, 2));
        if (profile.has(Field.BATCH_NO)) {
            cols.add(new Col(CatalogTerms.attrLabel(Field.BATCH_NO),
                    r -> text(r.get("batch_no").isBlank() ? "—" : r.get("batch_no")), false, 1));
        }
        if (profile.has(Field.EXPIRY_DATE)) {
            cols.add(new Col(L.t("Yaroqlilik", "Годен до", "Expiry"),
                    r -> text(r.get("expiry_date").isBlank() ? "—" : r.get("expiry_date")), false, 1));
        }
        return cols;
    }

    @Override
    protected void openForm() {
        if (!canWrite) {
            return;
        }
        Map<String, String> units = productUnits();

        Input date = input("date", "", today());
        Input product = input("text", L.t("Ro'yxatdan tanlang", "Выберите из списка", "Pick from the list"), "");
        Input qty = input("number", "0", "");
        Input price = input("number", "0", "");
        Input supplier = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), "");
        Input batch = input("text", "", "");
        Input expiry = input("date", "", "");

        Div err = errorBox();

        List<Div> parts = new ArrayList<>();
        parts.add(field(L.t("Nomi", "Название", "Name"), true, productPicker(product, "ol-products-r")));
        parts.add(grid(field(L.t("Sana", "Дата", "Date"), true, date),
                field(L.t("Yetkazib beruvchi", "Поставщик", "Supplier"), false, supplier)));
        parts.add(grid(field(L.t("Miqdor", "Количество", "Quantity"), true, qty),
                field(L.t("Kirim narxi", "Цена прихода", "Receipt price"), false, price)));

        List<Div> extra = new ArrayList<>();
        if (profile.has(Field.BATCH_NO)) {
            extra.add(field(CatalogTerms.attrLabel(Field.BATCH_NO), false, batch));
        }
        if (profile.has(Field.EXPIRY_DATE)) {
            extra.add(field(CatalogTerms.attrLabel(Field.EXPIRY_DATE), false, expiry));
        }
        if (!extra.isEmpty()) {
            parts.add(grid(extra.toArray(new Div[0])));
        }
        parts.add(err);
        Div body = formBody(parts.toArray(new Div[0]));

        NativeButton save = button(L.t("Saqlash", "Сохранить", "Save"), "pf-btn-primary");
        save.addClickListener(e -> {
            err.setVisible(false);
            save.setEnabled(false); // qayta bosib ikki marta saqlab yubormaslik uchun

            String name = canonicalProduct(product.getValue());
            BigDecimal q = Dk.num(qty.getValue());
            BigDecimal pr = Dk.num(price.getValue());
            String d = value(date);
            String exp = value(expiry);
            String problem = validate(name, q, pr, d, exp, units);
            if (problem != null) {
                fail(err, problem);
                save.setEnabled(true);
                return;
            }

            Set<Object> before = new HashSet<>();
            for (RecordStore.Rec r : store.list(company, section.key())) {
                before.add(r.id());
            }

            Map<String, String> data = new LinkedHashMap<>();
            data.put("date", d);
            data.put("supplier", cut(value(supplier), 120));
            data.put("product", name);
            data.put("quantity", Dk.plain(q));
            data.put("price", pr == null ? "" : Dk.plain(pr));
            if (profile.has(Field.BATCH_NO)) {
                data.put("batch_no", cut(value(batch), 60));
            }
            if (profile.has(Field.EXPIRY_DATE)) {
                data.put("expiry_date", exp);
            }

            try {
                store.save(company, section.key(), null, data);
            } catch (RuntimeException ex) {
                fail(err, L.t("Saqlab bo'lmadi", "Не удалось сохранить", "Could not save"));
                save.setEnabled(true);
                return;
            }
            try {
                ledger.apply(company, name, q, true);
            } catch (RuntimeException ex) {
                // qoldiq yangilanmasa, hozir qo'shilgan kirim yozuvi ham olib tashlanadi
                for (RecordStore.Rec r : store.list(company, section.key())) {
                    if (!before.contains(r.id())) {
                        store.delete(company, section.key(), r.id());
                    }
                }
                fail(err, L.t("Qoldiqni yangilab bo'lmadi", "Не удалось обновить остаток",
                        "Could not update stock"));
                save.setEnabled(true);
                return;
            }
            modalHost.removeAll();
            render();
        });

        showDialog(L.t("Yangi kirim", "Новый приход", "New receipt"), body, footer(save));
    }

    /** Hamma tekshiruvlar bir joyda; xato matnini yoki (hammasi joyida bo'lsa) null qaytaradi. */
    private String validate(String name, BigDecimal q, BigDecimal pr, String d, String exp,
                            Map<String, String> units) {
        if (name == null) {
            return L.t("Mahsulotni ro'yxatdan tanlang", "Выберите товар из списка", "Pick a product from the list");
        }
        if (q == null || q.signum() <= 0) {
            return L.t("Miqdor 0 dan katta bo'lsin", "Количество должно быть больше 0",
                    "Quantity must be greater than 0");
        }
        Unit unit = unitOf(units.get(name.trim().toLowerCase(Locale.ROOT)));
        if (unit != null && !unit.fractional() && q.stripTrailingZeros().scale() > 0) {
            return L.t("Bu tovar \"" + unit.label("uz") + "\" bilan o'lchanadi, butun son kiriting",
                    "Этот товар считается в «" + unit.label("ru") + "», введите целое число",
                    "This item is counted in \"" + unit.label("en") + "\", enter a whole number");
        }
        if (pr != null && pr.signum() < 0) {
            return L.t("Narx manfiy bo'lmasin", "Цена не может быть отрицательной", "Price cannot be negative");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(d);
        } catch (DateTimeParseException ex) {
            return L.t("Sanani to'g'ri kiriting", "Введите корректную дату", "Enter a valid date");
        }
        if (day.isAfter(LocalDate.now())) {
            return L.t("Kirim sanasi kelajakda bo'lmasin", "Дата прихода не может быть в будущем",
                    "Receipt date cannot be in the future");
        }
        if (!exp.isEmpty()) {
            try {
                profile.require(Field.EXPIRY_DATE, CatalogTerms.attrLabel(Field.EXPIRY_DATE));
                if (LocalDate.parse(exp).isBefore(day)) {
                    return L.t("Yaroqlilik muddati kirim sanasidan oldin bo'lmasin",
                            "Срок годности не может быть раньше даты прихода",
                            "Expiry cannot be earlier than the receipt date");
                }
            } catch (DateTimeParseException ex) {
                return L.t("Yaroqlilik sanasini to'g'ri kiriting", "Введите корректный срок годности",
                        "Enter a valid expiry date");
            } catch (IllegalStateException ex) {
                return ex.getMessage();
            }
        }
        return null;
    }

    private static Unit unitOf(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String l = label.trim();
        for (Unit u : Unit.values()) {
            if (l.equalsIgnoreCase(u.label("uz")) || l.equalsIgnoreCase(u.label("ru"))
                    || l.equalsIgnoreCase(u.label("en"))) {
                return u;
            }
        }
        return null;
    }

    private static String value(Input in) {
        return in.getValue() == null ? "" : in.getValue().trim();
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}