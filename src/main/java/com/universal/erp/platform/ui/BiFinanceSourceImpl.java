package com.universal.erp.platform.ui;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * NAMUNA: hozircha 0 va bo'sh ro'yxat qaytaradi. O'zingizning Moliya servisingizni ulang.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiFinanceSourceImpl.java
 */
@Service
public class BiFinanceSourceImpl implements BiFinanceSource {

    @Override
    public double cashBalance(String company) {
        return 0; // TODO: kassa + bank hisoblari yig'indisi
    }

    @Override
    public List<Payable> payables(String company) {
        return List.of(); // TODO: yetkazib beruvchilarga ochiq qarzlar
    }
}