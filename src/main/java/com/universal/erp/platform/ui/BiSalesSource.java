package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Savdo tahlili uchun ma'lumot manbai (kassa + hisob-fakturalar savdo qatorlari).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiSalesSource.java
 */
public interface BiSalesSource {

    /**
     * Savdo qatorlari. Qaytarish (vozvrat) qatorida qty va revenue MANFIY bo'ladi.
     * receipt = chek/hisob-faktura raqami (bir chekdagi qatorlar bir xil receipt oladi).
     * cost = shu qatorning tannarxi (qty * birlik tannarx), so'mda.
     */
    List<Line> lines(String company, LocalDate from, LocalDate to);

    record Line(String receipt, LocalDateTime time, String product, String category, double qty,
                double revenue, double cost, String customer, String cashier, String payment) {}
}