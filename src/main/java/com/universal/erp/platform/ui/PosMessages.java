
package com.universal.erp.platform.ui;

import java.math.BigDecimal;

import com.vaadin.flow.component.UI;

public final class PosMessages {

    private PosMessages() {
    }

    public static String text(PosCart.Result r) {

        if (r == null) {
            return "";
        }

        switch (r.status()) {

            case ADDED:
                return PosTexts.t("added", r.item().name());

            case INCREASED:
                return PosTexts.t(
                        "inc",
                        r.item().name(),
                        qty(r.qty())
                );

            case CHOOSE:
                return L.t(
                        "Mahsulotni tanlang",
                        "Выберите товар",
                        "Choose a product"
                );

            case NOT_FOUND:
                return PosTexts.t("notfound", r.query());

            case SEARCHING:
                return L.t(
                        "Global bazadan qidirilmoqda...",
                        "Поиск в глобальной базе...",
                        "Searching global database..."
                );

            case GLOBAL_FOUND:
                String name = r.global() == null
                        ? ""
                        : r.global().name();

                return L.t(
                        "Global bazadan topildi: " + name + ". Tasdiqlang",
                        "Найдено в глобальной базе: " + name + ". Подтвердите",
                        "Found in global database: " + name + ". Confirm"
                );

            default:
                return "";
        }
    }

    public static boolean ok(PosCart.Result r) {
        return r != null &&
                (r.status() == PosCart.Status.ADDED ||
                        r.status() == PosCart.Status.INCREASED);
    }

    public static void beep(UI ui, int hz, int ms) {

        if (ui == null) {
            return;
        }

        ui.getPage().executeJs(
                "try {" +
                        " const a = new (window.AudioContext || window.webkitAudioContext)();" +
                        " const o = a.createOscillator();" +
                        " const g = a.createGain();" +
                        " o.connect(g);" +
                        " g.connect(a.destination);" +
                        " o.frequency.value = $0;" +
                        " g.gain.value = 0.08;" +
                        " o.start();" +
                        " setTimeout(() => { o.stop(); a.close(); }, $1);" +
                        "} catch(e) {}",
                hz,
                ms
        );
    }

    /**
     * Ovoz faqat amal bajarilgandan keyin chiqariladi.
     * Faqat o'zbekcha ovoz ishlatiladi.
     */
    public static void sound(
            UI ui,
            String company,
            PosCart.Result r
    ) {

        if (ui == null || r == null) {
            return;
        }

        switch (r.status()) {

            case ADDED:
            case INCREASED:

                beep(ui, 880, 90);

                PosVoice.say(
                        ui,
                        company,
                        r.item().name()
                );

                break;

            case NOT_FOUND:

                beep(ui, 220, 450);

                PosVoice.say(
                        ui,
                        company,
                        "Mahsulot topilmadi"
                );

                break;

            default:
                break;
        }
    }

    private static String qty(BigDecimal q) {

        if (q == null) {
            return "0";
        }

        return q.stripTrailingZeros().toPlainString();
    }
}
