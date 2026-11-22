package com.universal.erp.platform.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.universal.erp.platform.security.AuthService;
import com.universal.erp.platform.security.RolePolicy;
import com.universal.erp.platform.ui.AppModule;
import com.universal.erp.platform.ui.AppSection;
import com.universal.erp.platform.ui.CurrentCompany;
import com.universal.erp.platform.ui.FieldDef;
import com.universal.erp.platform.ui.PosService;
import com.universal.erp.platform.ui.PosScanBridge;
import com.universal.erp.platform.ui.RecordStore;
import com.universal.erp.platform.ui.TenantModules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/pos")
public class PosApiController {

    private static final String SESSION_COMPANY = "pos.company";

    public record LoginRequest(
            String company,
            String login,
            String password
    ) {
    }

    public record ScanRequest(String barcode) {
    }

    public record PriceRequest(
            BigDecimal price,
            BigDecimal cost
    ) {
    }

    public record CreateRequest(
            String barcode,
            String name,
            String category,
            String unit,
            BigDecimal cost,
            BigDecimal price,
            BigDecimal quantity
    ) {
    }

    static final class ApiError extends RuntimeException {
        final int status;

        ApiError(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private final Object lock = new Object();

    private final AuthService authService;
    private final TenantModules tenantModules;
    private final RecordStore store;

    public PosApiController(
            AuthService authService,
            TenantModules tenantModules,
            RecordStore store
    ) {
        this.authService = authService;
        this.tenantModules = tenantModules;
        this.store = store;
    }

    // LOGIN

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest body,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        if (body == null
                || blank(body.company())
                || blank(body.login())
                || blank(body.password())) {
            return error(400, "Barcha maydonlarni to'ldiring");
        }

        try {
            authService.login(
                    body.company().trim(),
                    body.login().trim(),
                    body.password(),
                    req,
                    res
            );
        } catch (LockedException ex) {
            return error(423, "Hisob vaqtincha bloklangan");
        } catch (DisabledException ex) {
            return error(403, "Hisob faol emas");
        } catch (Exception ex) {
            return error(401, "Kompaniya, login yoki parol noto'g'ri");
        }

        req.getSession(true).setAttribute(
                SESSION_COMPANY,
                CurrentCompany.key(body.company())
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest req) {
        String company = company(req, false);

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("company", company);
        result.put("user", auth == null ? "" : auth.getName());
        result.put("canWrite", RolePolicy.canWrite(AppModule.INVENTORY));

        return result;
    }

    /**
     * Android telefonidan kelgan shtrix-kodni kompyuterdagi
     * shu sessionga tegishli faol POS oynasiga uzatadi.
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(
            @RequestBody ScanRequest body,
            HttpServletRequest req
    ) {
        company(req, false);

        String barcode = PosService.normalize(
                body == null ? null : body.barcode()
        );

        if (barcode.isBlank()) {
            throw new ApiError(400, "Shtrix-kod bo'sh");
        }

        HttpSession session = req.getSession(false);

        if (session == null
                || !PosScanBridge.send(session.getId(), barcode)) {
            throw new ApiError(
                    409,
                    "Kompyuterda POS oynasini oching va qayta skanerlang"
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("queued", true);
        result.put("barcode", barcode);

        return ResponseEntity.accepted().body(result);
    }

    // MAHSULOT META

    @GetMapping("/meta")
    public Map<String, Object> meta(HttpServletRequest req) {
        company(req, false);

        AppSection ps = products();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", options(ps.field("category")));
        result.put("units", options(ps.field("unit")));

        return result;
    }

    // MAHSULOTNI SHTRIX-KOD BO'YICHA TOPISH

    @GetMapping("/product")
    public Map<String, Object> product(
            @RequestParam("code") String code,
            HttpServletRequest req
    ) {
        String company = company(req, false);
        RecordStore.Rec record = findByCode(company, code);

        Map<String, Object> result = new LinkedHashMap<>();

        if (record == null) {
            result.put("found", false);
            return result;
        }

        return productJson(company, record);
    }

    // NARXNI O'ZGARTIRISH

    @PutMapping("/product/{id}/price")
    public Map<String, Object> updatePrice(
            @PathVariable("id") String id,
            @RequestBody PriceRequest body,
            HttpServletRequest req
    ) {
        String company = company(req, true);

        if (body == null
                || body.price() == null
                || body.price().signum() <= 0) {
            throw new ApiError(
                    400,
                    "Sotish narxi 0 dan katta bo'lishi kerak"
            );
        }

        if (body.cost() != null && body.cost().signum() < 0) {
            throw new ApiError(
                    400,
                    "Tannarx manfiy bo'lmasligi kerak"
            );
        }

        synchronized (lock) {
            AppSection ps = products();
            RecordStore.Rec record = null;

            for (RecordStore.Rec item : store.list(company, ps.key())) {
                if (item.id().equals(id)) {
                    record = item;
                    break;
                }
            }

            if (record == null) {
                throw new ApiError(404, "Mahsulot topilmadi");
            }

            Map<String, String> data =
                    new LinkedHashMap<>(record.data());

            data.put("price", plain(body.price()));

            if (body.cost() != null) {
                data.put("cost", plain(body.cost()));
            }

            RecordStore.Rec saved =
                    save(company, ps.key(), id, data);

            return productJson(company, saved);
        }
    }

    // YANGI MAHSULOT YARATISH

    @PostMapping("/product")
    public Map<String, Object> create(
            @RequestBody CreateRequest body,
            HttpServletRequest req
    ) {
        String company = company(req, true);

        if (body == null) {
            throw new ApiError(400, "Ma'lumot yo'q");
        }

        String barcode = PosService.normalize(body.barcode());
        String name = body.name() == null ? "" : body.name().trim();

        if (barcode.isEmpty()) {
            throw new ApiError(400, "Shtrix-kod kerak");
        }

        if (name.isEmpty()) {
            throw new ApiError(400, "Mahsulot nomini kiriting");
        }

        if (body.price() == null || body.price().signum() <= 0) {
            throw new ApiError(
                    400,
                    "Sotish narxi 0 dan katta bo'lishi kerak"
            );
        }

        if (body.cost() != null && body.cost().signum() < 0) {
            throw new ApiError(
                    400,
                    "Tannarx manfiy bo'lmasligi kerak"
            );
        }

        if (body.quantity() != null
                && body.quantity().signum() < 0) {
            throw new ApiError(
                    400,
                    "Qoldiq manfiy bo'lmasligi kerak"
            );
        }

        AppSection ps = products();

        String category = blank(body.category())
                ? "other"
                : body.category();

        String unit = blank(body.unit())
                ? "pcs"
                : body.unit();

        if (ps.field("category").option(category) == null) {
            throw new ApiError(400, "Noto'g'ri toifa");
        }

        if (ps.field("unit").option(unit) == null) {
            throw new ApiError(400, "Noto'g'ri o'lchov birligi");
        }

        synchronized (lock) {
            for (RecordStore.Rec item : store.list(company, ps.key())) {
                if (PosService.normalize(item.get("barcode"))
                        .equalsIgnoreCase(barcode)
                        || PosService.normalize(item.get("sku"))
                        .equalsIgnoreCase(barcode)) {
                    throw new ApiError(
                            409,
                            "Bu shtrix-kod allaqachon ro'yxatda bor"
                    );
                }

                if (item.get("name").trim()
                        .equalsIgnoreCase(name)) {
                    throw new ApiError(
                            409,
                            "Bunday nomli mahsulot allaqachon bor"
                    );
                }
            }

            Map<String, String> data = new LinkedHashMap<>();

            data.put("barcode", barcode);
            data.put("name", name);
            data.put("category", category);
            data.put("unit", unit);

            if (body.cost() != null) {
                data.put("cost", plain(body.cost()));
            }

            data.put("price", plain(body.price()));

            RecordStore.Rec saved =
                    save(company, ps.key(), null, data);

            if (body.quantity() != null
                    && body.quantity().signum() > 0) {

                AppSection stockSection =
                        AppSection.get(AppModule.INVENTORY, "stock");

                Map<String, String> stock = new LinkedHashMap<>();
                stock.put("product", name);
                stock.put("quantity", plain(body.quantity()));

                save(company, stockSection.key(), null, stock);

                AppSection receiptSection =
                        AppSection.get(AppModule.INVENTORY, "receipts");

                Map<String, String> receipt = new LinkedHashMap<>();
                receipt.put("date", LocalDate.now().toString());
                receipt.put("supplier", "Mobil ilova");
                receipt.put("product", name);
                receipt.put("quantity", plain(body.quantity()));

                if (body.cost() != null) {
                    receipt.put("price", plain(body.cost()));
                }

                save(company, receiptSection.key(), null, receipt);
            }

            return productJson(company, saved);
        }
    }

    // XATOLAR

    @ExceptionHandler(ApiError.class)
    public ResponseEntity<Map<String, Object>> onError(ApiError error) {
        return error(error.status, error.getMessage());
    }

    // ICHKI METODLAR

    private String company(HttpServletRequest req, boolean write) {
        HttpSession session = req.getSession(false);

        Object companyValue = session == null
                ? null
                : session.getAttribute(SESSION_COMPANY);

        if (companyValue == null
                || companyValue.toString().isBlank()) {
            throw new ApiError(401, "Qayta kiring");
        }

        String company = companyValue.toString();

        if (!tenantModules.enabled(company).contains(AppModule.INVENTORY)
                || !RolePolicy.canView(AppModule.INVENTORY)) {
            throw new ApiError(
                    403,
                    "Ombor moduli sizga ochiq emas"
            );
        }

        if (write && !RolePolicy.canWrite(AppModule.INVENTORY)) {
            throw new ApiError(
                    403,
                    "Mahsulotni o'zgartirishga ruxsatingiz yo'q"
            );
        }

        return company;
    }

    private static AppSection products() {
        return AppSection.get(AppModule.INVENTORY, "products");
    }

    private RecordStore.Rec findByCode(
            String company,
            String rawCode
    ) {
        String code = PosService.normalize(rawCode);

        if (code.isEmpty()) {
            return null;
        }

        List<RecordStore.Rec> all =
                store.list(company, products().key());

        for (RecordStore.Rec item : all) {
            if (PosService.normalize(item.get("barcode"))
                    .equalsIgnoreCase(code)) {
                return item;
            }
        }

        for (RecordStore.Rec item : all) {
            if (PosService.normalize(item.get("sku"))
                    .equalsIgnoreCase(code)) {
                return item;
            }
        }

        return null;
    }

    private RecordStore.Rec save(
            String company,
            String section,
            String id,
            Map<String, String> data
    ) {
        try {
            return store.save(company, section, id, data);
        } catch (IllegalStateException ex) {
            throw new ApiError(500, "Saqlab bo'lmadi");
        }
    }

    private Map<String, Object> productJson(
            String company,
            RecordStore.Rec record
    ) {
        AppSection ps = products();

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("found", true);
        result.put("id", record.id());
        result.put("name", record.get("name"));
        result.put("barcode", record.get("barcode"));
        result.put("sku", record.get("sku"));
        result.put("category", record.get("category"));
        result.put(
                "categoryLabel",
                label(ps.field("category"), record.get("category"))
        );
        result.put("unit", record.get("unit"));
        result.put(
                "unitLabel",
                label(ps.field("unit"), record.get("unit"))
        );
        result.put("cost", FieldDef.parse(record.get("cost")));
        result.put("price", FieldDef.parse(record.get("price")));
        result.put("stock", stockOf(company, record.get("name")));

        return result;
    }

    private Double stockOf(String company, String name) {
        AppSection stockSection =
                AppSection.get(AppModule.INVENTORY, "stock");

        for (RecordStore.Rec item :
                store.list(company, stockSection.key())) {

            if (item.get("product").trim()
                    .equalsIgnoreCase(name.trim())) {
                return FieldDef.parse(item.get("quantity"));
            }
        }

        return null;
    }

    private static String label(FieldDef field, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        FieldDef.Opt option = field.option(key);

        return option == null ? key : option.names()[0];
    }

    private static List<Map<String, String>> options(FieldDef field) {
        List<Map<String, String>> result = new ArrayList<>();

        for (FieldDef.Opt option : field.options()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("key", option.key());
            item.put("label", option.names()[0]);
            result.add(item);
        }

        return result;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseEntity<Map<String, Object>> error(
            int status,
            String message
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", message);

        return ResponseEntity.status(status).body(result);
    }
}