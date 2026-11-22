package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

import jakarta.annotation.security.PermitAll;

/**
 * Kassa ekrani: skaner, qog'oz chek ko'rinishidagi savat (rasmli), to'lov va qaytim.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/PosView.java
 */
@PageTitle("POS")
@Route(value = "app/pos/terminal", layout = MainLayout.class)
@CssImport("./styles/pos.css")
@CssImport("./styles/dk.css")
@PermitAll
public class PosView extends Div implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(PosView.class);

    private static final class Line {
        final PosService.Item item;
        BigDecimal qty;

        Line(PosService.Item item, BigDecimal qty) {
            this.item = item;
            this.qty = qty;
        }
    }

    private static final String BEEP_JS =
            "try{const a=new (window.AudioContext||window.webkitAudioContext)();"
                    + "const o=a.createOscillator();const g=a.createGain();o.connect(g);g.connect(a.destination);"
                    + "o.frequency.value=$0;g.gain.value=0.08;o.start();"
                    + "setTimeout(()=>{o.stop();a.close();},$1);}catch(e){}";

    private static final String KEY_CAPTURE_JS =
            "const el=this;"
                    + "if(window.__posKey){document.removeEventListener('keydown',window.__posKey,true);}"
                    + "window.__posKey=function(e){"
                    + "const t=e.target;const tag=t&&t.tagName;"
                    + "if(tag==='INPUT'||tag==='TEXTAREA'||tag==='SELECT'||(t&&t.isContentEditable)){return;}"
                    + "if(e.ctrlKey||e.metaKey||e.altKey||e.key.length!==1){return;}"
                    + "if(document.body.contains(el)&&!window.matchMedia('(pointer:coarse)').matches){el.focus();}"
                    + "};"
                    + "document.addEventListener('keydown',window.__posKey,true);"
                    + "setTimeout(()=>{if(!window.matchMedia('(pointer:coarse)').matches){el.focus();}},250);";

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final PosService pos;
    private final TenantModules tenantModules;
    private final PhoneScannerClient phone;
    private final String company;

    private Runnable phoneUnsub;

    private final Map<String, Line> cart;
    private final Input scan = new Input();
    private final Div suggest = new Div();
    private final Div done = new Div();
    private final Div msg = new Div();
    private final Div lines = new Div();
    private final Span totalValue = new Span();
    private final Span saleNoValue = new Span();

    // chek: faqat savdo raqami, sana va jami
    private final Span rcNo = new Span();
    private final Span rcDate = new Span();
    private final Span rcTotal = new Span();

    private final NativeButton cashBtn = new NativeButton(PosTexts.t("cash"));
    private final NativeButton cardBtn = new NativeButton(PosTexts.t("card"));
    private final NativeButton closeBtn = new NativeButton(PosTexts.t("close"));

    private final Div cashBox = new Div();
    private final Input received = new Input();
    private final Span changeValue = new Span("—");

    private String method = "cash";
    private String lastId;
    private String lastScanCode = "";
    private long lastScanTime = 0L;

    private final PhoneProductService products;
    private final GlobalProductService globals;
    private final PosSettings posSettings;
    private final ObjectMapper objectMapper;

    private Dialog globalDlg;

    public PosView(
            PosService pos,
            TenantModules tenantModules,
            PhoneScannerClient phone,
            PhoneProductService products,
            GlobalProductService globals,
            PosSettings posSettings,
            ObjectMapper objectMapper
    ) {
        this.pos = pos;
        this.tenantModules = tenantModules;
        this.phone = phone;
        this.products = products;
        this.globals = globals;
        this.posSettings = posSettings;
        this.objectMapper = objectMapper;

        this.company = CurrentCompany.get();
        this.cart = sessionCart(this.company);

        addClassName("pos-root");

        scan.setType("text");
        scan.setPlaceholder(L.t(
                "Shtrix-kodni skanerlang yoki mahsulot nomini yozing",
                "Отсканируйте штрихкод или введите название",
                "Scan a barcode or type a product name"));
        scan.addClassName("pos-scan-input");
        scan.getElement().setAttribute("autocomplete", "off");
        scan.getElement().setAttribute("autocapitalize", "off");
        scan.getElement().setAttribute("spellcheck", "false");
        scan.getElement().setAttribute("aria-label", PosTexts.t("scan.placeholder"));
        scan.setValueChangeMode(ValueChangeMode.LAZY);
        scan.setValueChangeTimeout(250);

        scan.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                updateSuggest(e.getValue());
            }
        });

        scan.getElement()
                .addEventListener("keydown",
                        e -> scan.getElement()
                                .executeJs("const v=this.value;this.value='';return v;")
                                .then(String.class, this::onScan))
                .setFilter("event.key === 'Enter'");

        suggest.addClassName("pos-suggest");
        suggest.setVisible(false);

        Span dot = new Span();
        dot.addClassName("pos-hint-dot");
        Span hintText = new Span(PosTexts.t("scan.hint"));
        Div hint = new Div(dot, hintText);
        hint.addClassName("pos-hint");

        Div scanBar = new Div(scan, suggest, hint);
        scanBar.addClassName("pos-scanbar");

        done.addClassName("pos-done");
        done.setVisible(false);

        msg.addClassNames("pos-msg", "is-idle");
        msg.setText(PosTexts.t("ready"));

        lines.addClassName("pos-lines");

        // ---- qog'oz chek: faqat raqam va sana
        rcNo.addClassName("pos-rc-no");
        rcDate.addClassName("pos-rc-date");
        Div rcHead = new Div(rcNo, rcDate);
        rcHead.addClassName("pos-rc-head");

        Span rcTotalLabel = new Span(PosTexts.t("total"));
        rcTotal.addClassName("pos-rc-total-value");
        Div rcTotalRow = new Div(rcTotalLabel, rcTotal);
        rcTotalRow.addClassNames("pos-rc-row", "is-total");
        Div rcFoot = new Div(rcTotalRow);
        rcFoot.addClassName("pos-rc-foot");

        Div paper = new Div(rcHead, lines, rcFoot);
        paper.addClassName("pos-paper");

        Div left = new Div(scanBar, done, msg, paper);
        left.addClassName("pos-left");

        // ---- o'ng panel
        Span saleLabel = new Span(PosTexts.t("sale"));
        saleLabel.addClassName("pos-sale-label");
        saleNoValue.addClassName("pos-sale-no");
        saleNoValue.setText(company.isEmpty() ? "—" : pos.nextNumber(company));

        NativeButton settingsBtn = new NativeButton("⚙");
        settingsBtn.addClassName("pos-settings-icon");
        settingsBtn.getElement().setAttribute("title", "POS sozlamalari");
        settingsBtn.getElement().setAttribute("aria-label", "POS sozlamalari");
        settingsBtn.addClickListener(e -> UI.getCurrent().navigate("app/pos/settings"));

        Div saleNoBox = new Div(saleLabel, saleNoValue);
        saleNoBox.addClassName("pos-sale-id");
        Div saleHead = new Div(saleNoBox, settingsBtn);
        saleHead.addClassName("pos-sale-head");

        Span totalLabel = new Span(PosTexts.t("total"));
        totalLabel.addClassName("pos-sum-label");
        totalValue.addClassName("pos-total-value");
        Div totalRow = new Div(totalLabel, totalValue);
        totalRow.addClassNames("pos-sum-row", "is-total");

        Span methodLabel = new Span(PosTexts.t("method"));
        methodLabel.addClassName("pos-field-label");

        cashBtn.addClassName("pos-method");
        cardBtn.addClassName("pos-method");
        cashBtn.addClickListener(e -> {
            setMethod("cash");
            refocusIfCash();
        });
        cardBtn.addClickListener(e -> {
            setMethod("card");
            refocus();
        });
        Div methods = new Div(cashBtn, cardBtn);
        methods.addClassName("pos-methods");

        Span recvLabel = new Span(PosTexts.t("received"));
        recvLabel.addClassName("pos-field-label");

        received.setType("number");
        received.addClassName("pos-recv-input");
        received.getElement().setAttribute("min", "0");
        received.getElement().setAttribute("step", "any");
        received.getElement().setAttribute("inputmode", "decimal");
        received.getElement().setAttribute("placeholder", "0");
        received.getElement().addEventListener("input", e -> refreshChange());

        NativeButton exact = new NativeButton(PosTexts.t("exact"));
        exact.addClassName("pos-btn-ghost");
        exact.addClickListener(e -> {
            received.getElement().executeJs("this.value=$0", total().stripTrailingZeros().toPlainString());
            refreshChange();
        });

        Div recvRow = new Div(received, exact);
        recvRow.addClassName("pos-recv-row");

        Span changeLabel = new Span(PosTexts.t("change"));
        changeLabel.addClassName("pos-sum-label");
        changeValue.addClassName("pos-change-value");
        Div changeRow = new Div(changeLabel, changeValue);
        changeRow.addClassName("pos-sum-row");

        cashBox.add(recvLabel, recvRow, changeRow);
        cashBox.addClassName("pos-cash");

        closeBtn.addClassName("pos-btn-close");
        closeBtn.addClickListener(e -> received.getElement()
                .executeJs("return this.value")
                .then(String.class, this::closeSale));

        NativeButton clear = new NativeButton(PosTexts.t("clear"));
        clear.addClassName("pos-btn-clear");
        clear.addClickListener(e -> {
            cart.clear();
            lastId = null;
            render();
            refocus();
        });

        NativeButton phoneBtn = new NativeButton(PosTexts.t("phone"));
        phoneBtn.addClassName("pos-btn-phone");
        phoneBtn.addClickListener(e -> UI.getCurrent().navigate("app/inventory/phone-link"));

        Div secondary = new Div(clear, phoneBtn);
        secondary.addClassName("pos-secondary");

        Div right = new Div(saleHead, totalRow, methodLabel, methods, cashBox, closeBtn, secondary);
        right.addClassName("pos-summary");

        add(left, right);

        setMethod("cash");
        render();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean enabled = false;

        if (!company.isEmpty()) {
            for (AppModule m : tenantModules.enabled(company)) {
                if (m == AppModule.POS) {
                    enabled = true;
                }
            }
        }

        if (!enabled || !RolePolicy.canView(AppModule.POS)) {
            event.forwardTo("app");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Line> sessionCart(String company) {
        VaadinSession s = VaadinSession.getCurrent();
        if (s == null) {
            return new LinkedHashMap<>();
        }
        String key = "pos.cart." + company;
        Object o = s.getAttribute(key);
        if (o instanceof Map) {
            return (Map<String, Line>) o;
        }
        Map<String, Line> m = new LinkedHashMap<>();
        s.setAttribute(key, m);
        return m;
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        scan.getElement().executeJs(KEY_CAPTURE_JS);

        // kompyuterda chap menyu yig'iladi, POS butun ekranni oladi
        event.getUI().getPage().executeJs(
                "if(!window.matchMedia('(max-width:1000px)').matches){"
                        + "const r=document.querySelector('.sh-root');"
                        + "if(r){r.classList.add('is-collapsed');}}");

        String sessionId = VaadinSession.getCurrent().getSession().getId();
        PosScanBridge.register(sessionId, event.getUI(), this::onScan);

        UI ui = event.getUI();
        if (phoneUnsub != null) {
            phoneUnsub.run();
        }
        phoneUnsub = phone.addCodeListener(code -> ui.access(() -> onScan(code)));
        phone.setCompany(company);
    }

    @Override
    protected void onDetach(DetachEvent event) {
        String sessionId = event.getUI().getSession().getSession().getId();
        PosScanBridge.unregister(sessionId, event.getUI());

        if (phoneUnsub != null) {
            phoneUnsub.run();
            phoneUnsub = null;
        }

        super.onDetach(event);

        event.getUI().getPage().executeJs(
                "if(window.__posKey){"
                        + "document.removeEventListener('keydown',window.__posKey,true);"
                        + "window.__posKey=null;}");
        event.getUI().getPage().executeJs(
                "const r=document.querySelector('.sh-root');if(r){r.classList.remove('is-collapsed');}");
    }

    // ================================================================
    // Skaner va qidirish
    // ================================================================

    private void onScan(String raw) {
        if (raw != null && raw.trim().startsWith("{")) {
            return;
        }

        scan.setValue("");
        hideSuggest();

        String text = raw == null ? "" : raw.trim();
        String code = PosService.normalize(raw);

        if (code.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (code.equals(lastScanCode) && now - lastScanTime < 800) {
            lastScanTime = now;
            return;
        }
        lastScanCode = code;
        lastScanTime = now;

        Optional<PosService.Item> found = pos.find(company, code);
        if (found.isPresent()) {
            addItem(found.get());
            return;
        }

        List<PosService.Item> res = pos.search(company, text);

        if (res.size() == 1) {
            addItem(res.get(0));
        } else if (res.size() > 1) {
            showSuggest(res);
            setMessage(null, L.t("Mahsulotni tanlang", "Выберите товар", "Choose a product"));
        } else if (code.matches("\\d{6,14}") && posSettings.get(company).global()) {
            lookupGlobalBarcode(code);
        } else {
            setMessage(false, PosTexts.t("notfound", text));
            beep(220, 300);
        }
    }

    private void lookupGlobalBarcode(String barcode) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        setMessage(null, "Global katalogdan mahsulot qidirilmoqda...");

        CompletableFuture
                .supplyAsync(() -> globals.lookup(barcode))
                .whenComplete((result, error) -> ui.access(() -> {
                    if (!isAttached()) {
                        return;
                    }
                    if (error != null) {
                        setMessage(false, "Global katalog bilan bog'lanib bo'lmadi");
                        beep(220, 300);
                        return;
                    }
                    if (result == null || result.isEmpty()) {
                        setMessage(false, PosTexts.t("notfound", barcode));
                        beep(220, 300);
                        return;
                    }
                    openGlobalProductDialog(barcode, result.get());
                }));
    }

    // ================================================================
    // Global mahsulotni saqlash
    // ================================================================

    private void openGlobalProductDialog(String barcode, GlobalProductService.Global global) {
        if (globalDlg != null && globalDlg.isOpened()) {
            globalDlg.close();
        }

        globalDlg = new Dialog();
        globalDlg.setHeaderTitle("Global katalogdan mahsulot topildi");

        TextField name = new TextField("Mahsulot nomi");
        name.setWidthFull();
        name.setValue(global.name() == null ? "" : global.name());

        TextField size = new TextField("Mahsulot hajmi");
        size.setWidthFull();
        size.setPlaceholder("Masalan: 1 L, 1.5 L, 500 ml");
        size.setValue(global.quantity() == null ? "" : global.quantity());

        TextField brand = new TextField("Brend");
        brand.setWidthFull();
        brand.setValue(global.brand() == null ? "" : global.brand());
        brand.setReadOnly(true);

        TextField code = new TextField("Shtrix-kod");
        code.setWidthFull();
        code.setValue(barcode);
        code.setReadOnly(true);

        NumberField price = new NumberField("Sotish narxi (UZS)");
        price.setWidthFull();
        price.setMin(0.01);
        price.setStep(100);
        price.setValue(null);

        NumberField cost = new NumberField("Tannarx (ixtiyoriy)");
        cost.setWidthFull();
        cost.setMin(0);

        Span note = new Span("Mahsulot hajmi nomiga qo'shilib, kompaniya omboriga saqlanadi.");

        Div body = new Div(name, size, brand, code, price, cost, note);
        body.setWidth("min(420px, 90vw)");

        Button save = new Button("Saqlash", e -> {
            String baseName = name.getValue() == null ? "" : name.getValue().trim();
            String productSize = size.getValue() == null ? "" : size.getValue().trim();
            Double salePrice = price.getValue();
            Double purchaseCost = cost.getValue();

            if (baseName.isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Mahsulot nomini kiriting");
                return;
            }
            if (salePrice == null || salePrice <= 0) {
                price.setInvalid(true);
                price.setErrorMessage("Sotish narxi 0 dan katta bo'lishi kerak");
                return;
            }

            String productName = baseName;
            if (!productSize.isBlank()
                    && !baseName.toLowerCase(java.util.Locale.ROOT)
                    .contains(productSize.toLowerCase(java.util.Locale.ROOT))) {
                productName = baseName + " " + productSize;
            }

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("op", "create");
            request.put("barcode", barcode);
            request.put("name", productName);
            request.put("category", "other");
            request.put("unit", "pcs");
            request.put("price", BigDecimal.valueOf(salePrice));
            if (purchaseCost != null) {
                request.put("cost", BigDecimal.valueOf(purchaseCost));
            }

            try {
                products.handle(company, objectMapper.valueToTree(request));
                globalDlg.close();

                Optional<PosService.Item> created = pos.find(company, barcode);
                if (created.isPresent()) {
                    addItem(created.get());
                } else {
                    setMessage(true, "Mahsulot saqlandi. Qayta skanerlang.");
                }
            } catch (RuntimeException ex) {
                setMessage(false, ex.getMessage() == null ? "Mahsulotni saqlab bo'lmadi" : ex.getMessage());
                beep(220, 300);
            }
        });

        Button cancel = new Button("Bekor qilish", e -> globalDlg.close());

        globalDlg.add(body);
        globalDlg.getFooter().add(cancel, save);
        globalDlg.open();
    }

    private void updateSuggest(String value) {
        String v = value == null ? "" : value.trim();

        if (v.length() < 2 || (v.matches("\\d+") && v.length() >= 6)) {
            hideSuggest();
            return;
        }

        List<PosService.Item> res = pos.search(company, v);
        if (res.isEmpty()) {
            hideSuggest();
        } else {
            showSuggest(res);
        }
    }

    private void showSuggest(List<PosService.Item> items) {
        suggest.removeAll();

        for (PosService.Item it : items) {
            NativeButton b = new NativeButton();
            b.addClassName("pos-sg");

            Span name = new Span(it.name());
            name.addClassName("pos-sg-name");

            Span meta = new Span(money(it.price()) + " UZS"
                    + (it.code().isBlank() ? "" : " · " + it.code()));
            meta.addClassName("pos-sg-meta");

            Div text = new Div(name, meta);
            text.addClassName("pos-sg-text");

            b.add(Dk.thumb(it.image(), it.name()), text);

            b.addClickListener(e -> {
                scan.setValue("");
                hideSuggest();
                addItem(it);
                refocus();
            });

            suggest.add(b);
        }

        suggest.setVisible(true);
    }

    private void hideSuggest() {
        suggest.removeAll();
        suggest.setVisible(false);
    }

    private void addItem(PosService.Item item) {
        done.setVisible(false);

        Line line = cart.get(item.id());

        if (line == null) {
            cart.put(item.id(), new Line(item, BigDecimal.ONE));
            setMessage(true, PosTexts.t("added", item.name()));
        } else {
            line.qty = line.qty.add(BigDecimal.ONE);
            setMessage(true, PosTexts.t("inc", item.name(), qtyText(line.qty)));
        }

        lastId = item.id();
        beep(1046, 100);
        render();
    }

    private void change(Line line, BigDecimal qty) {
        if (qty.signum() <= 0) {
            cart.remove(line.item.id());
        } else {
            line.qty = qty;
        }
        render();
        refocus();
    }

    // ================================================================
    // To'lov va sotuvni yopish
    // ================================================================

    private void setMethod(String m) {
        this.method = m;
        boolean cash = "cash".equals(m);
        cashBtn.setClassName("is-active", cash);
        cardBtn.setClassName("is-active", !cash);
        cashBox.setVisible(cash);
    }

    private void refreshChange() {
        received.getElement()
                .executeJs("return this.value")
                .then(String.class, v -> {
                    BigDecimal r = parseMoney(v);
                    if (r == null) {
                        changeValue.setText("—");
                        changeValue.removeClassName("is-short");
                        return;
                    }
                    BigDecimal d = r.subtract(total());
                    changeValue.setText((d.signum() < 0 ? "−" : "") + money(d.abs()));
                    changeValue.setClassName("is-short", d.signum() < 0);
                });
    }

    private void closeSale(String receivedRaw) {
        if (cart.isEmpty()) {
            setMessage(false, PosTexts.t("nocart"));
            return;
        }

        if (!(RolePolicy.canWrite(AppModule.POS)
                || RolePolicy.canWrite(AppModule.INVENTORY)
                || RolePolicy.canWrite(AppModule.CRM))) {
            setMessage(false, PosTexts.t("denied"));
            beep(220, 300);
            return;
        }

        BigDecimal total = total();
        BigDecimal recv = null;
        BigDecimal back = null;

        if ("cash".equals(method)) {
            recv = parseMoney(receivedRaw);
            if (recv == null) {
                recv = total;
            }
            if (recv.compareTo(total) < 0) {
                setMessage(false, PosTexts.t("short"));
                beep(220, 300);
                return;
            }
            back = recv.subtract(total);
        }

        StringBuilder items = new StringBuilder();
        List<PosService.SaleLine> sold = new ArrayList<>();

        for (Line l : cart.values()) {
            if (items.length() > 0) {
                items.append("; ");
            }
            items.append(l.item.name())
                    .append(" × ")
                    .append(qtyText(l.qty))
                    .append(" = ")
                    .append(money(l.item.price().multiply(l.qty)));

            sold.add(new PosService.SaleLine(l.item.name(), l.qty));
        }

        String no;

        try {
            no = pos.saveSale(company, method, total, recv, back, items.toString(), cashier(), sold);
        } catch (RuntimeException ex) {
            // haqiqiy sababni konsolda ko'rish uchun
            log.error("Savdoni saqlab bo'lmadi (kompaniya={}, usul={}, summa={})", company, method, total, ex);
            setMessage(false, PosTexts.t("error") + " (" + ex.getClass().getSimpleName() + ")");
            beep(220, 300);
            return;
        }

        String methodName = PosTexts.t("cash".equals(method) ? "cash" : "card");
        String text = PosTexts.t("closed", no, money(total), methodName);

        if (back != null && back.signum() > 0) {
            text += " · " + PosTexts.t("change") + ": " + money(back);
        }

        done.setText(text);
        done.setVisible(true);

        beep(1046, 100);
        startNewSale();
    }

    private void startNewSale() {
        cart.clear();
        lastId = null;
        lastScanCode = "";

        saleNoValue.setText(pos.nextNumber(company));

        received.getElement().executeJs("this.value=''");
        changeValue.setText("—");
        changeValue.removeClassName("is-short");

        setMethod("cash");
        setMessage(null, PosTexts.t("ready"));

        render();
        refocus();
    }

    private static String cashier() {
        VaadinRequest r = VaadinRequest.getCurrent();
        Principal p = r == null ? null : r.getUserPrincipal();
        return p == null ? "" : p.getName();
    }

    // ================================================================
    // Ekranni chizish
    // ================================================================

    private BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        for (Line l : cart.values()) {
            t = t.add(l.item.price().multiply(l.qty));
        }
        return t;
    }

    private void render() {
        lines.removeAll();

        if (cart.isEmpty()) {
            Div empty = new Div();
            empty.setText(PosTexts.t("empty"));
            empty.addClassName("pos-empty");
            lines.add(empty);
        }

        int no = 0;
        for (Line l : cart.values()) {
            no++;
            lines.add(lineRow(l, no));
        }

        totalValue.setText(money(total()) + " UZS");

        rcNo.setText("№ " + saleNoValue.getText());
        rcDate.setText(LocalDateTime.now().format(RECEIPT_DATE));
        rcTotal.setText(money(total()) + " UZS");

        closeBtn.setEnabled(!cart.isEmpty());

        // yangi mahsulot qo'shilganda chek pastga aylanadi
        lines.getElement().executeJs("this.scrollTop = this.scrollHeight");

        if ("cash".equals(method)) {
            refreshChange();
        }
    }

    private Component lineRow(Line l, int no) {
        Span num = new Span(String.format("%02d", no));
        num.addClassName("pos-line-no");

        Div thumb = new Div(Dk.thumb(l.item.image(), l.item.name()));
        thumb.addClassName("pos-line-thumb");

        Span name = new Span(l.item.name());
        name.addClassName("pos-line-name");

        String meta = qtyText(l.qty) + " × " + money(l.item.price())
                + (l.item.unit().isBlank() ? "" : " / " + l.item.unit());
        Span metaSpan = new Span(meta);
        metaSpan.addClassName("pos-line-meta");

        Div text = new Div(name, metaSpan);
        text.addClassName("pos-line-text");

        Div info = new Div(num, thumb, text);
        info.addClassName("pos-line-info");

        NativeButton minus = new NativeButton("−");
        minus.addClassName("pos-qty-btn");
        minus.addClickListener(e -> change(l, l.qty.subtract(BigDecimal.ONE)));

        NativeButton plus = new NativeButton("+");
        plus.addClassName("pos-qty-btn");
        plus.addClickListener(e -> change(l, l.qty.add(BigDecimal.ONE)));

        Input q = new Input();
        q.setType("number");
        q.addClassName("pos-qty-input");
        q.getElement().setAttribute("min", "0");
        q.getElement().setAttribute("step", "any");
        q.getElement().setAttribute("inputmode", "decimal");
        q.getElement().setProperty("value", qtyText(l.qty));
        q.getElement().addEventListener("change", e -> q.getElement()
                .executeJs("return this.value")
                .then(String.class, v -> {
                    BigDecimal n = parseMoney(v);
                    if (n == null) {
                        render();
                    } else {
                        change(l, n);
                    }
                }));

        Div qty = new Div(minus, q, plus);
        qty.addClassName("pos-line-qty");

        Span sum = new Span(money(l.item.price().multiply(l.qty)));
        sum.addClassName("pos-line-sum");

        NativeButton del = new NativeButton("✕");
        del.addClassName("pos-line-del");
        del.getElement().setAttribute("aria-label", PosTexts.t("remove"));
        del.getElement().setAttribute("title", PosTexts.t("remove"));
        del.addClickListener(e -> {
            cart.remove(l.item.id());
            render();
            refocus();
        });

        Div row = new Div(info, qty, sum, del);
        row.addClassName("pos-line");
        if (l.item.id().equals(lastId)) {
            row.addClassName("is-last");
        }
        return row;
    }

    private void setMessage(Boolean ok, String text) {
        msg.setText(text);
        msg.removeClassNames("is-idle", "is-ok", "is-err");
        msg.addClassName(ok == null ? "is-idle" : ok ? "is-ok" : "is-err");
    }

    private void refocus() {
        scan.getElement().executeJs("if(!window.matchMedia('(pointer:coarse)').matches){this.focus();}");
    }

    private void refocusIfCash() {
        if ("cash".equals(method)) {
            refreshChange();
        }
        refocus();
    }

    private static void beep(int hz, int ms) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(BEEP_JS, hz, ms);
        }
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String qtyText(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString();
    }

    private static String money(BigDecimal v) {
        return FieldDef.format(v.doubleValue());
    }
}