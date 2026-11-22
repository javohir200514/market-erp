package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.util.List;

/**
 * Moliyaviy tahlil uchun qo'shimcha ma'lumotlar (BiDataSource ustiga).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiFinanceSource.java
 */
public interface BiFinanceSource {

    /** Bugungi pul qoldig'i: kassa + hisob raqamlari, so'mda. */
    double cashBalance(String company);

    /** Yetkazib beruvchilarga to'lanmagan qarzlar (kreditorlik). */
    List<Payable> payables(String company);

    record Payable(String supplier, double amount, LocalDate dueDate) {}
}