
package com.universal.erp.platform.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class PosCart {

    public record Line(PosService.Item item, BigDecimal qty) {
    }

    public enum Status {
        ADDED,
        INCREASED,
        CHOOSE,
        NOT_FOUND,
        SEARCHING,
        GLOBAL_FOUND
    }

    public record Result(
            Status status,
            PosService.Item item,
            BigDecimal qty,
            BigDecimal have,
            List<PosService.Item> options,
            String query,
            GlobalProductService.Global global
    ) {
        public Result(
                Status status,
                PosService.Item item,
                BigDecimal qty,
                BigDecimal have,
                List<PosService.Item> options,
                String query
        ) {
            this(status, item, qty, have, options, query, null);
        }
    }

    public record Event(String company, Result result) {
    }

    private static final Pattern BARCODE =
            Pattern.compile("\\d{6,14}");

    private static final long DEBOUNCE_MS = 800L;

    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "pos-global-lookup");
                t.setDaemon(true);
                return t;
            });

    private final PosService pos;
    private final GlobalProductService globals;
    private final PosSettings settings;

    private final Map<String, LinkedHashMap<String, Line>> carts =
            new LinkedHashMap<>();

    private final Map<String, String> lastCode =
            new LinkedHashMap<>();

    private final Map<String, Long> lastTime =
            new LinkedHashMap<>();

    private final List<Consumer<Event>> listeners =
            new CopyOnWriteArrayList<>();

    public PosCart(
            PosService pos,
            GlobalProductService globals,
            PosSettings settings
    ) {
        this.pos = pos;
        this.globals = globals;
        this.settings = settings;
    }

    // ---------------------------------------------------------
    // Shtrix-kodni skanerlash
    // ---------------------------------------------------------

    public Result scan(String company, String raw) {

        if (company == null || company.isBlank()) {
            return result(Status.NOT_FOUND, null, raw);
        }

        String code = PosService.normalize(raw);

        if (code.isBlank()) {
            return result(Status.NOT_FOUND, null, code);
        }

        // Bir xil kodni ketma-ket skanerlashni cheklash
        synchronized (this) {
            long now = System.currentTimeMillis();

            String previous = lastCode.get(company);
            Long previousTime = lastTime.get(company);

            if (code.equals(previous)
                    && previousTime != null
                    && now - previousTime < DEBOUNCE_MS) {
                return result(Status.SEARCHING, null, code);
            }

            lastCode.put(company, code);
            lastTime.put(company, now);
        }

        // Avval kompaniyaning o'z mahsulotlaridan qidiramiz
        Optional<PosService.Item> found = pos.find(company, code);

        if (found.isPresent()) {
            return add(company, found.get(), BigDecimal.ONE);
        }

        // Nomi, barcode yoki SKU orqali qidirish
        List<PosService.Item> options = pos.search(company, code);

        if (options.size() == 1) {
            return add(company, options.get(0), BigDecimal.ONE);
        }

        if (options.size() > 1) {
            Result r = new Result(
                    Status.CHOOSE,
                    null,
                    BigDecimal.ZERO,
                    null,
                    options,
                    code
            );

            publish(company, r);
            return r;
        }

        // Mahalliy mahsulot topilmasa, global API tekshiriladi
        boolean globalEnabled;

        try {
            globalEnabled = settings.get(company).global();
        } catch (Exception e) {
            globalEnabled = false;
        }

        if (!globalEnabled || !BARCODE.matcher(code).matches()) {
            Result r = result(Status.NOT_FOUND, null, code);
            publish(company, r);
            return r;
        }

        Result searching = result(Status.SEARCHING, null, code);
        publish(company, searching);

        // Global API so'rovini alohida oqimda bajarish
        EXECUTOR.submit(() -> {
            try {
                Optional<GlobalProductService.Global> global =
                        globals.lookup(code);

                Result r;

                if (global.isPresent()) {
                    r = new Result(
                            Status.GLOBAL_FOUND,
                            null,
                            BigDecimal.ZERO,
                            null,
                            List.of(),
                            code,
                            global.get()
                    );
                } else {
                    r = result(Status.NOT_FOUND, null, code);
                }

                publish(company, r);

            } catch (Exception e) {
                publish(
                        company,
                        result(Status.NOT_FOUND, null, code)
                );
            }
        });

        return searching;
    }

    // ---------------------------------------------------------
    // Savatga mahsulot qo'shish
    // ---------------------------------------------------------

    public Result add(
            String company,
            PosService.Item item,
            BigDecimal qty
    ) {

        if (company == null || company.isBlank()
                || item == null || qty == null
                || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return result(Status.NOT_FOUND, null, "");
        }

        String key = item.id();

        if (key == null || key.isBlank()) {
            key = item.code();
        }

        if (key == null || key.isBlank()) {
            key = item.name();
        }

        synchronized (this) {
            LinkedHashMap<String, Line> cart =
                    carts.computeIfAbsent(
                            company,
                            k -> new LinkedHashMap<>()
                    );

            Line old = cart.get(key);

            BigDecimal newQty;
            Status status;

            if (old == null) {
                newQty = qty;
                status = Status.ADDED;
            } else {
                newQty = old.qty().add(qty);
                status = Status.INCREASED;
            }

            cart.put(key, new Line(item, newQty));

            Result r = new Result(
                    status,
                    item,
                    newQty,
                    null,
                    List.of(),
                    item.code()
            );

            publish(company, r);
            return r;
        }
    }

    public Result addItem(
            String company,
            PosService.Item item
    ) {
        return add(company, item, BigDecimal.ONE);
    }

    public Result addItem(
            String company,
            PosService.Item item,
            BigDecimal qty
    ) {
        return add(company, item, qty);
    }

    // ---------------------------------------------------------
    // Miqdorni o'zgartirish
    // ---------------------------------------------------------

    public synchronized void setQty(
            String company,
            String itemId,
            BigDecimal qty
    ) {
        if (company == null || itemId == null || qty == null) {
            return;
        }

        LinkedHashMap<String, Line> cart = carts.get(company);

        if (cart == null) {
            return;
        }

        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            cart.remove(itemId);
            return;
        }

        Line old = cart.get(itemId);

        if (old != null) {
            cart.put(
                    itemId,
                    new Line(old.item(), qty)
            );
        }
    }

    // ---------------------------------------------------------
    // Mahsulotni savatdan o'chirish
    // ---------------------------------------------------------

    public synchronized void remove(
            String company,
            String itemId
    ) {
        if (company == null || itemId == null) {
            return;
        }

        LinkedHashMap<String, Line> cart = carts.get(company);

        if (cart != null) {
            cart.remove(itemId);
        }
    }

    // ---------------------------------------------------------
    // Savatni tozalash
    // ---------------------------------------------------------

    public synchronized void clear(String company) {
        if (company == null) {
            return;
        }

        LinkedHashMap<String, Line> cart = carts.get(company);

        if (cart != null) {
            cart.clear();
        }
    }

    // ---------------------------------------------------------
    // Savatdagi mahsulotlar
    // ---------------------------------------------------------

    public synchronized List<Line> lines(String company) {
        LinkedHashMap<String, Line> cart = carts.get(company);

        if (cart == null) {
            return List.of();
        }

        return new ArrayList<>(cart.values());
    }

    // ---------------------------------------------------------
    // Savat jami summasi
    // ---------------------------------------------------------

    public synchronized BigDecimal total(String company) {
        BigDecimal total = BigDecimal.ZERO;

        LinkedHashMap<String, Line> cart = carts.get(company);

        if (cart == null) {
            return total;
        }

        for (Line line : cart.values()) {
            BigDecimal price = line.item().price();
            BigDecimal qty = line.qty();

            if (price != null && qty != null) {
                total = total.add(price.multiply(qty));
            }
        }

        return total;
    }

    // ---------------------------------------------------------
    // Savatdagi mahsulotlar soni
    // ---------------------------------------------------------

    public synchronized int count(String company) {
        LinkedHashMap<String, Line> cart = carts.get(company);

        return cart == null ? 0 : cart.size();
    }

    // ---------------------------------------------------------
    // Hodisalar
    // ---------------------------------------------------------

    public void addListener(Consumer<Event> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<Event> listener) {
        listeners.remove(listener);
    }

    private void publish(String company, Result result) {
        Event event = new Event(company, result);

        for (Consumer<Event> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception ignored) {
                // Bitta listener xatosi boshqalariga ta'sir qilmasin
            }
        }
    }

    private Result result(
            Status status,
            PosService.Item item,
            String query
    ) {
        return new Result(
                status,
                item,
                BigDecimal.ZERO,
                null,
                List.of(),
                query
        );
    }
}
