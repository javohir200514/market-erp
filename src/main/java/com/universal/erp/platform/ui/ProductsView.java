package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.RolePolicy;
import com.universal.erp.platform.ui.WarehouseProfile.Field;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Katalog sahifasi: do'kon turiga qarab nomlanadi ("Kiyimlar", "Elektronika"...) va maydonlari moslashadi.
 *
 * O'zgarishlar:
 *  - Rasm bo'lmasa yoki yuklanmasa, qora quti o'rniga mahsulot nomining bosh harfi ko'rsatiladi.
 *  - Telefonda "Kamera" tugmasi: bevosita kamera ochiladi, olingan rasm shu zahoti yuklanadi.
 *  - Rasm brauzerda kichraytiriladi; butunlay qora chiqqan rasm qabul qilinmaydi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/ProductsView.java
 */
@Route(value = "app/inventory/products", layout = MainLayout.class)
@CssImport("./styles/dk.css")
@CssImport("./styles/products.css")
@CssImport("./styles/product-photo.css")
@PermitAll
public class ProductsView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    /**
     * Tanlangan faylni o'qiydi, 360px gacha kichraytiradi va JPEG data-URL qaytaradi.
     * Muammo bo'lsa (o'qib bo'lmadi, butunlay qora chiqdi) bo'sh matn qaytaradi.
     */
    private static final String RESIZE_JS = """
            const f = this.files && this.files[0];
            this.value = '';
            if (!f) { return ''; }
            return new Promise(res => {
              const r = new FileReader();
              r.onload = () => {
                const im = new Image();
                im.onload = () => {
                  const m = 360;
                  let w = im.naturalWidth || im.width, h = im.naturalHeight || im.height;
                  if (!w || !h) { res(''); return; }
                  const k = Math.min(1, m / Math.max(w, h));
                  w = Math.max(1, Math.round(w * k)); h = Math.max(1, Math.round(h * k));
                  const c = document.createElement('canvas');
                  c.width = w; c.height = h;
                  const x = c.getContext('2d');
                  x.fillStyle = '#fff'; x.fillRect(0, 0, w, h);
                  x.drawImage(im, 0, 0, w, h);
                  let ok = true;
                  try {
                    const t = document.createElement('canvas');
                    t.width = 8; t.height = 8;
                    const y = t.getContext('2d');
                    y.drawImage(c, 0, 0, 8, 8);
                    const d = y.getImageData(0, 0, 8, 8).data;
                    let mx = 0;
                    for (let i = 0; i < d.length; i += 4) { mx = Math.max(mx, d[i], d[i + 1], d[i + 2]); }
                    ok = mx > 12;
                  } catch (e) { ok = true; }
                  res(ok ? c.toDataURL('image/jpeg', 0.75) : '');
                };
                im.onerror = () => res('');
                im.src = r.result;
              };
              r.onerror = () => res('');
              r.readAsDataURL(f);
            });
            """;

    private static final String PENCIL = "<svg viewBox='0 0 24 24' width='18' height='18' fill='none' "
            + "stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<path d='M12 20h9'/><path d='M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z'/></svg>";
    private static final String TRASH = "<svg viewBox='0 0 24 24' width='18' height='18' fill='none' "
            + "stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<path d='M3 6h18'/><path d='M8 6V4h8v2'/><path d='M19 6l-1 14H6L5 6'/>"
            + "<path d='M10 11v6M14 11v6'/></svg>";
    private static final String CAMERA = "<svg viewBox='0 0 24 24' width='18' height='18' fill='none' "
            + "stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<path d='M3 8a2 2 0 0 1 2-2h2l1.5-2h7L17 6h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'/>"
            + "<circle cx='12' cy='13' r='3.5'/></svg>";
    private static final String GALLERY = "<svg viewBox='0 0 24 24' width='18' height='18' fill='none' "
            + "stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
            + "<rect x='3' y='4' width='18' height='16' rx='2'/><circle cx='9' cy='10' r='1.8'/>"
            + "<path d='M21 16l-5-5-8 8'/></svg>";

    private static final int MAX_IMAGE_CHARS = 500_000;

    private final RecordStore store;
    private final PosService pos;
    private final TenantModules tenantModules;
    private final String company = CurrentCompany.get();
    private final AppSection ps = AppSection.get(AppModule.INVENTORY, "products");
    private final AppSection ss = AppSection.get(AppModule.INVENTORY, "stock");
    private final boolean canWrite = RolePolicy.canWrite(AppModule.INVENTORY);

    private final WarehouseProfile profile;
    private final CatalogTerms.Terms terms;

    private final Div list = new Div();
    private final Div filters = new Div();
    private final Span count = new Span();
    private final Input search = new Input();
    private final Div modalHost = new Div();
    private final String[] catFilter = {""};

    public ProductsView(RecordStore store, PosService pos, TenantModules tenantModules,
                        WarehouseProfiles profiles) {
        this.store = store;
        this.pos = pos;
        this.tenantModules = tenantModules;
        this.profile = profiles.forCompany(company);
        this.terms = profiles.termsFor(company);
        addClassName("pl-page");

        // --- sarlavha
        H2 title = new H2(terms.title());
        title.addClassName("pl-title");
        count.addClassName("pl-sub");
        Div titles = new Div(title, count);
        titles.addClassName("pl-titles");

        NativeButton addBtn = new NativeButton("+  " + terms.addLabel());
        addBtn.addClassNames("pl-btn", "pl-btn-primary");
        addBtn.setVisible(canWrite);
        addBtn.addClickListener(e -> openForm(null));

        Div head = new Div(titles, addBtn);
        head.addClassName("pl-head");

        // --- qidiruv
        search.setType("search");
        search.setPlaceholder(L.t("Nomi yoki shtrix-kod bo'yicha qidirish", "Поиск по названию или штрихкоду",
                "Search by name or barcode"));
        search.addClassName("pl-search-input");
        search.getElement().setAttribute("autocomplete", "off");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> render());
        Div searchBox = new Div(HomeIcons.svg("search"), search);
        searchBox.addClassName("pl-search");

        filters.addClassName("pl-filters");
        Div toolbar = new Div(searchBox, filters);
        toolbar.addClassName("pl-toolbar");

        list.addClassName("pl-list");
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
        return "DOKONIX — " + terms.title();
    }

    // ================================================================ rasm / bosh harf

    /**
     * Mahsulot rasmi. Rasm yo'q yoki yuklanmasa (xato, buzilgan ma'lumot), nomning bosh harfi
     * rangli doirada ko'rinadi. Rang nomdan olinadi, shuning uchun har bir mahsulotniki doim bir xil.
     */
    private static Div thumb(String img, String name) {
        Div box = new Div();
        box.addClassName("pt");
        box.getStyle().set("--pt-h", String.valueOf(hue(name)));
        Span letter = new Span(initial(name));
        letter.addClassName("pt-letter");
        box.add(letter);
        if (img != null && !img.isBlank()
                && (img.startsWith("data:image/") || img.startsWith("http") || img.startsWith("/"))) {
            Element el = new Element("img");
            el.setAttribute("src", img);
            el.setAttribute("alt", "");
            el.setAttribute("onerror", "this.remove()"); // rasm ochilmasa, harf ko'rinib turadi
            el.getClassList().add("pt-img");
            box.getElement().appendChild(el);
        }
        return box;
    }

    private static String initial(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            return "?";
        }
        int cp = n.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase(Locale.ROOT);
    }

    private static int hue(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        return Math.abs(n.hashCode()) % 360;
    }

    // ================================================================ birlik yordamchilari

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

    private static WarehouseProfile.Unit unitOf(String key) {
        for (WarehouseProfile.Unit u : WarehouseProfile.Unit.values()) {
            if (unitKey(u).equals(key)) {
                return u;
            }
        }
        return null;
    }

    private static String unitLabel(String key) {
        WarehouseProfile.Unit u = unitOf(key);
        return u == null ? key : u.label(AppTexts.lang());
    }

    // ================================================================ ro'yxat

    private void render() {
        list.removeAll();
        String q = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        Map<String, Double> stock = new LinkedHashMap<>();
        if (ss != null) {
            for (RecordStore.Rec r : store.list(company, ss.key())) {
                stock.put(r.get("product").trim().toLowerCase(Locale.ROOT), FieldDef.parse(r.get("quantity")));
            }
        }

        List<RecordStore.Rec> all = store.list(company, ps.key());
        filterChips(all);

        int n = 0;
        for (RecordStore.Rec r : all) {
            if (!catFilter[0].isEmpty() && !catFilter[0].equals(r.get("category"))) {
                continue;
            }
            if (!q.isEmpty() && !(r.get("name").toLowerCase(Locale.ROOT).contains(q)
                    || r.get("barcode").toLowerCase(Locale.ROOT).contains(q)
                    || r.get("sku").toLowerCase(Locale.ROOT).contains(q))) {
                continue;
            }
            list.add(card(r, stock));
            n++;
        }
        count.setText(L.t("Jami: ", "Всего: ", "Total: ") + n);
        if (n == 0) {
            list.add(emptyState(all.isEmpty()));
        }
    }

    /** Toifa bo'yicha filtr chiplari: faqat ishlatilgan toifalar. */
    private void filterChips(List<RecordStore.Rec> all) {
        filters.removeAll();
        Map<String, Integer> used = new LinkedHashMap<>();
        for (RecordStore.Rec r : all) {
            String c = r.get("category");
            if (!c.isBlank()) {
                used.merge(c, 1, Integer::sum);
            }
        }
        if (used.size() < 2) {
            filters.setVisible(false);
            catFilter[0] = "";
            return;
        }
        filters.setVisible(true);
        if (!catFilter[0].isEmpty() && !used.containsKey(catFilter[0])) {
            catFilter[0] = "";
        }
        filters.add(filterChip(L.t("Hammasi", "Все", "All") + " " + all.size(), ""));
        for (Map.Entry<String, Integer> en : used.entrySet()) {
            filters.add(filterChip(CatalogTerms.categoryLabel(en.getKey()) + " " + en.getValue(), en.getKey()));
        }
    }

    private NativeButton filterChip(String text, String key) {
        NativeButton b = new NativeButton(text);
        b.addClassName("pl-chip");
        b.setClassName("is-active", key.equals(catFilter[0]));
        b.addClickListener(e -> {
            catFilter[0] = key;
            render();
        });
        return b;
    }

    private Component emptyState(boolean noData) {
        Span icon = new Span(HomeIcons.svg("inventory"));
        icon.addClassName("pl-empty-icon");

        Span title = new Span(noData
                ? terms.empty()
                : L.t("Hech narsa topilmadi", "Ничего не найдено", "Nothing found"));
        title.addClassName("pl-empty-title");

        Span text = new Span(noData
                ? L.t("Birinchi yozuvni qo'shing, u shu yerda ko'rinadi.",
                        "Добавьте первую запись, она появится здесь.",
                        "Add your first item and it will show up here.")
                : L.t("Qidiruv yoki toifa filtrini o'zgartirib ko'ring.",
                        "Измените поиск или фильтр категории.",
                        "Try changing the search or category filter."));
        text.addClassName("pl-empty-text");

        Div box = new Div(icon, title, text);
        box.addClassName("pl-empty");
        if (noData && canWrite) {
            NativeButton add = new NativeButton("+  " + terms.addLabel());
            add.addClassNames("pl-btn", "pl-btn-primary");
            add.addClickListener(e -> openForm(null));
            box.add(add);
        }
        return box;
    }

    private Component card(RecordStore.Rec r, Map<String, Double> stock) {
        Span name = new Span(r.get("name"));
        name.addClassName("pl-name");

        List<String> meta = new ArrayList<>();
        if (!r.get("category").isBlank()) {
            meta.add(CatalogTerms.categoryLabel(r.get("category")));
        }
        for (Field f : new Field[] {Field.SIZE, Field.COLOR}) {
            String v = profile.has(f) ? r.get(CatalogTerms.attrKey(f)) : "";
            if (!v.isBlank()) {
                meta.add(v);
            }
        }
        if (profile.has(Field.EXPIRY_DATE) && !r.get("expiry_date").isBlank()) {
            meta.add(CatalogTerms.attrLabel(Field.EXPIRY_DATE) + ": " + r.get("expiry_date"));
        }
        Span metaSpan = new Span(String.join(" · ", meta));
        metaSpan.addClassName("pl-meta");

        String code = !r.get("barcode").isBlank() ? r.get("barcode") : r.get("sku");
        Span codeSpan = new Span(code);
        codeSpan.addClassName("pl-code");
        codeSpan.setVisible(!code.isBlank());

        Div info = new Div(name, metaSpan, codeSpan);
        info.addClassName("pl-info");

        double price = FieldDef.parse(r.get("price"));
        double cost = FieldDef.parse(r.get("cost"));
        Span priceSpan = new Span(FieldDef.format(price) + " " + L.t("so'm", "сум", "UZS"));
        priceSpan.addClassName("pl-price");
        Div prices = new Div(priceSpan);
        if (cost > 0) {
            Span c = new Span(L.t("Tannarx: ", "Себестоимость: ", "Cost: ") + FieldDef.format(cost));
            c.addClassName("pl-cost");
            prices.add(c);
        }
        prices.addClassName("pl-prices");

        Double q = stock.get(r.get("name").trim().toLowerCase(Locale.ROOT));
        String unit = r.get("unit").isBlank() ? "" : " " + unitLabel(r.get("unit"));
        Span badge = new Span(q == null
                ? L.t("Qoldiq yo'q", "Нет остатка", "No stock data")
                : FieldDef.format(q) + unit);
        badge.addClassName("pl-badge");
        badge.addClassName(q == null ? "is-none" : q <= 0 ? "is-zero" : "is-ok");
        Span badgeLabel = new Span(L.t("Qoldiq", "Остаток", "Stock"));
        badgeLabel.addClassName("pl-badge-label");
        Div stockBox = new Div(badgeLabel, badge);
        stockBox.addClassName("pl-stock");

        Div actions = new Div();
        actions.addClassName("pl-actions");
        if (canWrite) {
            NativeButton edit = new NativeButton();
            edit.getElement().appendChild(new Html(PENCIL).getElement());
            edit.addClassName("pl-icon-btn");
            edit.getElement().setAttribute("aria-label", L.t("Tahrirlash", "Редактировать", "Edit"));
            edit.addClickListener(e -> openForm(r));
            NativeButton del = new NativeButton();
            del.getElement().appendChild(new Html(TRASH).getElement());
            del.addClassNames("pl-icon-btn", "is-danger");
            del.getElement().setAttribute("aria-label", L.t("O'chirish", "Удалить", "Delete"));
            del.addClickListener(e -> confirmDelete(r));
            actions.add(edit, del);
        }

        Div thumb = new Div(thumb(r.get("image"), r.get("name")));
        thumb.addClassName("pl-thumb");

        Div row = new Div(thumb, info, prices, stockBox, actions);
        row.addClassName("pl-item");
        return row;
    }

    // ================================================================ forma

    private void openForm(RecordStore.Rec ex) {
        if (!canWrite) {
            return;
        }
        boolean isNew = ex == null;
        Map<String, String> base = snapshot(ex);
        String oldName = isNew ? "" : ex.get("name");

        Input name = input("text", terms.example(), base.get("name"));
        Input barcode = input("text", L.t("Skanerlang yoki yozing", "Отсканируйте или введите", "Scan or type"),
                base.get("barcode"));
        Input sku = input("text", L.t("Ixtiyoriy", "Необязательно", "Optional"), base.get("sku"));
        Input cost = input("number", "0", base.get("cost"));
        Input price = input("number", "0", base.get("price"));
        Input qty = input("number", "0", "");

        String[] cat = {base.getOrDefault("category", terms.defaultCategory())};
        String[] unit = {base.getOrDefault("unit", "pcs")};
        String[] img = {base.getOrDefault("image", "")};

        // --- rasm: ko'rinish (rasm yo'q bo'lsa bosh harf)
        Div preview = new Div();
        preview.addClassName("pf-photo");
        drawPreview(preview, img[0], oldName);

        // nom yozilganda, rasm bo'lmasa, bosh harf shu zahoti yangilanadi
        name.setValueChangeMode(ValueChangeMode.EAGER);
        name.addValueChangeListener(e -> {
            if (img[0].isBlank()) {
                drawPreview(preview, "", e.getValue());
            }
        });

        // --- rasm: kamera (telefonda) va fayl tanlash
        Span photoMsg = new Span();
        photoMsg.addClassName("pf-photo-msg");
        photoMsg.setVisible(false);

        Input cameraInput = fileInput(true);
        Input galleryInput = fileInput(false);
        wirePhoto(cameraInput, img, preview, name, photoMsg);
        wirePhoto(galleryInput, img, preview, name, photoMsg);

        NativeLabel cameraBtn = pickLabel(CAMERA, L.t("Kameradan olish", "Сделать фото", "Take photo"), cameraInput);
        cameraBtn.setVisible(false); // faqat sensorli qurilmada ko'rsatiladi
        UI.getCurrent().getPage().executeJs("return window.matchMedia('(pointer:coarse)').matches;")
                .then(Boolean.class, coarse -> cameraBtn.setVisible(Boolean.TRUE.equals(coarse)));
        NativeLabel galleryBtn = pickLabel(GALLERY, L.t("Rasm tanlash", "Выбрать фото", "Choose photo"), galleryInput);

        NativeButton removeImg = button(L.t("Rasmni olib tashlash", "Убрать фото", "Remove photo"), "pf-btn-link");
        removeImg.addClickListener(e -> {
            img[0] = "";
            photoMsg.setVisible(false);
            drawPreview(preview, "", name.getValue());
        });

        Div photoButtons = new Div(cameraBtn, galleryBtn);
        photoButtons.addClassName("pf-photo-buttons");
        Div imgControls = new Div(photoButtons, removeImg, photoMsg);
        imgControls.addClassName("pf-photo-controls");
        Div imgRow = new Div(preview, imgControls);
        imgRow.addClassName("pf-photo-row");

        // --- toifa va birlik
        List<String> catKeys = new ArrayList<>();
        for (CatalogTerms.Category c : terms.categories()) {
            catKeys.add(c.key());
        }
        if (!cat[0].isBlank() && !catKeys.contains(cat[0])) {
            catKeys.add(cat[0]);
        }
        List<String> unitKeys = new ArrayList<>();
        for (WarehouseProfile.Unit u : profile.units()) {
            unitKeys.add(unitKey(u));
        }
        if (!unit[0].isBlank() && !unitKeys.contains(unit[0])) {
            if (isNew && !unitKeys.isEmpty()) {
                unit[0] = unitKeys.get(0);
            } else {
                unitKeys.add(unit[0]);
            }
        }

        // --- yo'nalishga xos maydonlar
        Map<Field, Input> attrs = new LinkedHashMap<>();
        Div attrGrid = new Div();
        attrGrid.addClassName("pf-grid");
        for (Field f : CatalogTerms.FIELD_ORDER) {
            if (!profile.has(f)) {
                continue;
            }
            Input in = input(CatalogTerms.attrKind(f), "", base.get(CatalogTerms.attrKey(f)));
            attrs.put(f, in);
            Div fld = field(CatalogTerms.attrLabel(f), false, in);
            if (CatalogTerms.attrWide(f)) {
                fld.addClassName("pf-span-2");
            }
            attrGrid.add(fld);
        }

        // --- bo'limlar
        Div body = new Div();
        body.addClassName("pf-dialog-body");

        Div grid1 = new Div(field(L.t("Shtrix-kod", "Штрихкод", "Barcode"), false, barcode),
                field(L.t("Ichki kod", "Внутренний код", "Internal code"), false, sku));
        grid1.addClassName("pf-grid");
        body.add(section(L.t("Asosiy ma'lumot", "Основное", "Basics"),
                field(L.t("Rasm", "Фото", "Photo"), false, imgRow),
                field(L.t("Nomi", "Название", "Name"), true, name),
                grid1));

        body.add(section(L.t("Toifa va o'lchov birligi", "Категория и единица", "Category and unit"),
                field(L.t("Toifa", "Категория", "Category"), false,
                        chips(catKeys, CatalogTerms::categoryLabel, cat)),
                field(L.t("O'lchov birligi", "Единица измерения", "Unit"), false,
                        chips(unitKeys, ProductsView::unitLabel, unit))));

        if (!attrs.isEmpty()) {
            body.add(section(L.t("Xususiyatlari", "Характеристики", "Details"), attrGrid));
        }

        Div money = new Div(field(L.t("Tannarxi", "Себестоимость", "Cost price"), false, cost),
                field(L.t("Sotish narxi", "Цена продажи", "Sale price"), true, price));
        money.addClassName("pf-grid");
        Div moneySection = section(L.t("Narx va qoldiq", "Цена и остаток", "Price and stock"), money);
        if (isNew) {
            moneySection.add(field(L.t("Boshlang'ich qoldiq", "Начальный остаток", "Initial stock"), false, qty));
        }
        body.add(moneySection);

        Div err = new Div();
        err.addClassName("pf-error");
        err.setVisible(false);
        body.add(err);

        // --- tugmalar
        NativeButton cancel = button(L.t("Bekor qilish", "Отмена", "Cancel"), "pf-btn-ghost");
        cancel.addClickListener(e -> modalHost.removeAll());
        NativeButton save = button(L.t("Saqlash", "Сохранить", "Save"), "pf-btn-primary");
        Div foot = new Div(cancel, save);
        foot.addClassName("pf-dialog-foot");

        save.addClickListener(e -> {
            err.setVisible(false);
            save.setEnabled(false); // ikki marta bosib ikki marta saqlab yubormaslik uchun
            if (!trySave(isNew, ex, oldName, name, barcode, sku, cost, price, qty, cat, unit, img, attrs, base, err)) {
                save.setEnabled(true);
            }
        });

        showDialog(isNew ? terms.createTitle() : terms.editTitle(), body, foot);
        barcode.getElement().executeJs("if(!window.matchMedia('(pointer:coarse)').matches){this.focus();}");
    }

    /** Yashirin fayl maydoni. capture=true bo'lsa, telefonda to'g'ridan-to'g'ri orqa kamera ochiladi. */
    private static Input fileInput(boolean capture) {
        Input in = new Input();
        in.setType("file");
        in.getElement().setAttribute("accept", "image/*");
        if (capture) {
            in.getElement().setAttribute("capture", "environment");
        }
        in.addClassName("pf-file-hidden");
        return in;
    }

    /** Tugma ko'rinishidagi label: bosilganda ichidagi fayl maydonini (kamera/galereya) ochadi. */
    private static NativeLabel pickLabel(String svg, String text, Input input) {
        NativeLabel l = new NativeLabel();
        l.addClassNames("pf-btn", "pf-btn-ghost", "pf-pick");
        Span ico = new Span();
        ico.addClassName("pf-pick-ico");
        ico.getElement().appendChild(new Html(svg).getElement());
        l.add(ico, new Span(text), input);
        return l;
    }

    /** Fayl tanlanganda brauzerda kichraytiradi va ko'rinishni yangilaydi. */
    private static void wirePhoto(Input input, String[] img, Div preview, Input name, Span msg) {
        input.getElement().addEventListener("change", e -> {
            preview.addClassName("is-busy");
            msg.setVisible(false);
            input.getElement().executeJs(RESIZE_JS).then(String.class, data -> {
                preview.removeClassName("is-busy");
                if (data == null || data.isBlank() || data.length() > MAX_IMAGE_CHARS) {
                    msg.setText(L.t("Rasm yuklanmadi. Boshqa rasm tanlang yoki qayta suratga oling.",
                            "Фото не загрузилось. Выберите другое или сделайте снимок заново.",
                            "The photo could not be loaded. Choose another or retake it."));
                    msg.setVisible(true);
                    return;
                }
                img[0] = data;
                drawPreview(preview, img[0], name.getValue());
            });
        });
    }

    /** Tekshiradi va saqlaydi. Muvaffaqiyatli bo'lsa true (dialog yopiladi), xatoda false. */
    private boolean trySave(boolean isNew, RecordStore.Rec ex, String oldName,
                            Input name, Input barcode, Input sku, Input cost, Input price, Input qty,
                            String[] cat, String[] unit, String[] img, Map<Field, Input> attrs,
                            Map<String, String> base, Div err) {
        String n = name.getValue() == null ? "" : name.getValue().trim();
        BigDecimal pr = Dk.num(price.getValue());
        BigDecimal co = Dk.num(cost.getValue());
        BigDecimal q = Dk.num(qty.getValue());
        String bc = PosService.normalize(barcode.getValue());
        String sk = PosService.normalize(sku.getValue());

        if (n.isEmpty()) {
            fail(err, L.t("Nomini kiriting", "Введите название", "Enter a name"));
            return false;
        }
        if (pr == null || pr.signum() <= 0) {
            fail(err, L.t("Sotish narxini kiriting", "Введите цену продажи", "Enter the sale price"));
            return false;
        }
        if (co != null && co.signum() < 0) {
            fail(err, L.t("Tannarx manfiy bo'lmasin", "Себестоимость не может быть отрицательной",
                    "Cost cannot be negative"));
            return false;
        }
        if (q != null && q.signum() < 0) {
            fail(err, L.t("Qoldiq manfiy bo'lmasin", "Остаток не может быть отрицательным",
                    "Stock cannot be negative"));
            return false;
        }
        if (img[0].length() > MAX_IMAGE_CHARS) {
            fail(err, L.t("Rasm juda katta, kichikroq rasm tanlang", "Фото слишком большое, выберите поменьше",
                    "The photo is too large, choose a smaller one"));
            return false;
        }
        WarehouseProfile.Unit chosen = unitOf(unit[0]);
        if (q != null && q.signum() > 0 && q.stripTrailingZeros().scale() > 0
                && chosen != null && !profile.allowsFraction(chosen)) {
            fail(err, L.t("Bu birlikda qoldiq butun son bo'lishi kerak",
                    "Для этой единицы остаток должен быть целым числом",
                    "Stock must be a whole number for this unit"));
            return false;
        }

        // yo'nalishga xos maydonlar: backend tekshiruvi
        Map<String, String> attrValues = new LinkedHashMap<>();
        for (Map.Entry<Field, Input> en : attrs.entrySet()) {
            Field f = en.getKey();
            String v = en.getValue().getValue() == null ? "" : en.getValue().getValue().trim();
            if (!v.isEmpty()) {
                try {
                    profile.require(f, CatalogTerms.attrLabel(f));
                } catch (IllegalStateException ise) {
                    fail(err, ise.getMessage());
                    return false;
                }
                String problem = validateAttr(f, v);
                if (problem != null) {
                    fail(err, problem);
                    return false;
                }
            }
            attrValues.put(CatalogTerms.attrKey(f), v);
        }

        for (RecordStore.Rec r : store.list(company, ps.key())) {
            if (!isNew && r.id().equals(ex.id())) {
                continue;
            }
            if (r.get("name").trim().equalsIgnoreCase(n)) {
                fail(err, L.t("Bunday nomli mahsulot bor", "Товар с таким названием уже есть",
                        "A product with this name already exists"));
                return false;
            }
            for (String code : new String[] {bc, sk}) {
                if (!code.isEmpty() && (PosService.normalize(r.get("barcode")).equalsIgnoreCase(code)
                        || PosService.normalize(r.get("sku")).equalsIgnoreCase(code))) {
                    fail(err, L.t("Bu kod boshqa mahsulotda bor: ", "Этот код уже у товара: ",
                            "This code belongs to: ") + r.get("name"));
                    return false;
                }
            }
        }

        Map<String, String> d = new LinkedHashMap<>(base);
        d.put("name", n);
        d.put("barcode", bc);
        d.put("sku", sk);
        d.put("category", cat[0]);
        d.put("unit", unit[0]);
        d.put("cost", co == null ? "" : Dk.plain(co));
        d.put("price", Dk.plain(pr));
        d.put("image", img[0]);
        d.putAll(attrValues);
        try {
            store.save(company, ps.key(), isNew ? null : ex.id(), d);
            if (isNew) {
                if (q != null && q.signum() > 0) {
                    pos.setStock(company, n, q);
                }
            } else if (!n.equals(oldName)) {
                pos.renameStock(company, oldName, n);
            }
        } catch (RuntimeException exc) {
            fail(err, L.t("Saqlab bo'lmadi", "Не удалось сохранить", "Could not save"));
            return false;
        }
        modalHost.removeAll();
        render();
        return true;
    }

    /** Tahrirlanayotgan yozuvning hamma ma'lum maydonlari (yangi bo'lsa bo'sh). */
    private Map<String, String> snapshot(RecordStore.Rec ex) {
        Map<String, String> m = new LinkedHashMap<>();
        if (ex == null) {
            return m;
        }
        List<String> keys = new ArrayList<>();
        for (FieldDef fd : ps.fields()) {
            keys.add(fd.key());
        }
        for (String k : new String[] {"name", "barcode", "sku", "category", "unit", "cost", "price", "image"}) {
            keys.add(k);
        }
        for (Field f : CatalogTerms.FIELD_ORDER) {
            keys.add(CatalogTerms.attrKey(f));
        }
        for (String k : keys) {
            try {
                String v = ex.get(k);
                m.putIfAbsent(k, v == null ? "" : v);
            } catch (RuntimeException ignored) {
                // maydon yo'q
            }
        }
        return m;
    }

    /** Maydon qiymatini turiga qarab tekshiradi; muammo bo'lsa xabar, bo'lmasa null. */
    private static String validateAttr(Field f, String v) {
        String label = CatalogTerms.attrLabel(f);
        switch (CatalogTerms.attrKind(f)) {
            case "number" -> {
                BigDecimal n = Dk.num(v);
                if (n == null || n.signum() < 0) {
                    return label + ": " + L.t("to'g'ri son kiriting", "введите корректное число",
                            "enter a valid number");
                }
            }
            case "date" -> {
                try {
                    LocalDate.parse(v);
                } catch (DateTimeParseException ex) {
                    return label + ": " + L.t("sanani to'g'ri kiriting", "введите корректную дату",
                            "enter a valid date");
                }
            }
            default -> {
            }
        }
        if (f == Field.IMEI && !v.matches("\\d{15}")) {
            return L.t("IMEI 15 ta raqamdan iborat bo'lishi kerak", "IMEI состоит из 15 цифр",
                    "IMEI must be 15 digits");
        }
        return null;
    }

    private void confirmDelete(RecordStore.Rec r) {
        Div body = new Div();
        body.addClassName("pf-dialog-body");
        Div text = new Div();
        text.setText(L.t("«" + r.get("name") + "» o'chirilsinmi? Uning ombor qoldig'i ham o'chadi.",
                "Удалить «" + r.get("name") + "»? Остаток на складе тоже будет удалён.",
                "Delete \"" + r.get("name") + "\"? Its stock record will be deleted too."));
        text.addClassName("pf-confirm");
        Div delErr = new Div();
        delErr.addClassName("pf-error");
        delErr.setVisible(false);
        body.add(text, delErr);

        NativeButton cancel = button(L.t("Bekor qilish", "Отмена", "Cancel"), "pf-btn-ghost");
        cancel.addClickListener(e -> modalHost.removeAll());
        NativeButton del = button(L.t("O'chirish", "Удалить", "Delete"), "pf-btn-danger");
        del.addClickListener(e -> {
            del.setEnabled(false);
            try {
                store.delete(company, ps.key(), r.id());
                pos.deleteStock(company, r.get("name"));
            } catch (RuntimeException exc) {
                fail(delErr, L.t("O'chirib bo'lmadi", "Не удалось удалить", "Could not delete"));
                del.setEnabled(true);
                return;
            }
            modalHost.removeAll();
            render();
        });
        Div foot = new Div(cancel, del);
        foot.addClassName("pf-dialog-foot");
        showDialog(L.t("O'chirish", "Удаление", "Delete"), body, foot);
    }

    // ================================================================ forma qismlari

    private void showDialog(String title, Div body, Div foot) {
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

    private static Div section(String title, Component... content) {
        Span head = new Span(title);
        head.addClassName("pf-section-title");
        Div s = new Div(head);
        s.addClassName("pf-section");
        for (Component c : content) {
            s.add(c);
        }
        return s;
    }

    private static Div field(String label, boolean required, Component control) {
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

    private static Input input(String type, String placeholder, String value) {
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

    private static NativeButton button(String text, String cls) {
        NativeButton b = new NativeButton(text);
        b.addClassNames("pf-btn", cls);
        return b;
    }

    private static void fail(Div err, String message) {
        err.setText(message);
        err.setVisible(true);
    }

    private static void drawPreview(Div box, String img, String name) {
        box.removeAll();
        box.add(thumb(img, name));
    }

    private Div chips(List<String> keys, Function<String, String> label, String[] sel) {
        Div box = new Div();
        box.addClassName("pf-chips");
        List<NativeButton> all = new ArrayList<>();
        for (String key : keys) {
            NativeButton b = new NativeButton(label.apply(key));
            b.addClassName("pf-chip");
            b.setClassName("is-active", key.equals(sel[0]));
            b.addClickListener(e -> {
                sel[0] = key;
                all.forEach(x -> x.removeClassName("is-active"));
                b.addClassName("is-active");
            });
            all.add(b);
            box.add(b);
        }
        return box;
    }
}