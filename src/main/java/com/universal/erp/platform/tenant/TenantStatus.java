
package com.universal.erp.platform.tenant;

public enum TenantStatus {

    PENDING,    // Ro'yxatdan o'tgan, tasdiqlanishi kutilmoqda
    ACTIVE,     // Faol tenant
    SUSPENDED,  // Vaqtincha bloklangan
    DISABLED,   // Faoliyati o'chirilgan
    CANCELLED   // Obuna bekor qilingan
}
