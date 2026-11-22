package com.universal.erp.platform.ui;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;

/**
 * Telefon orqali kelgan shtrix-kodni ayni HTTP sessiondagi
 * faol POS oynasiga uzatadi.
 */
public final class PosScanBridge {

    private record Target(UI ui, Consumer<String> receiver) {}

    private static final ConcurrentHashMap<String, Target> TARGETS =
            new ConcurrentHashMap<>();

    private PosScanBridge() {
    }

    public static void register(
            String sessionId,
            UI ui,
            Consumer<String> receiver
    ) {
        TARGETS.put(sessionId, new Target(ui, receiver));
    }

    public static void unregister(String sessionId, UI ui) {
        TARGETS.computeIfPresent(sessionId, (key, target) ->
                target.ui() == ui ? null : target
        );
    }

    public static boolean send(String sessionId, String barcode) {
        Target target = TARGETS.get(sessionId);

        if (target == null || !target.ui().isAttached()) {
            return false;
        }

        target.ui().access(() -> {
            if (target.ui().isAttached()) {
                target.receiver().accept(barcode);
            }
        });

        return true;
    }
}