package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * NAMUNA: bo'sh ro'yxat qaytaradi. O'zingizning kassa/hisob-faktura repository'ingizni ulang.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiSalesSourceImpl.java
 */
@Service
public class BiSalesSourceImpl implements BiSalesSource {

    @Override
    public List<Line> lines(String company, LocalDate from, LocalDate to) {
        // TODO: from..to oralig'idagi savdo qatorlarini Line ga aylantirib qaytaring
        return List.of();
    }
}