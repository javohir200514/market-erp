package com.universal.erp.platform.ui;

import java.util.Set;

/**
 * Ro'yxatdan o'tish wizard'ining yakuniy ma'lumotlari.
 *
 * @param locale uz / ru / en — hisobning standart tili
 */
public record RegistrationRequest(
        String companyName,
        String taxId,
        String fullName,
        String email,
        String phone,
        String password,
        String locale,
        Set<AppModule> modules) {

    /** Parol log'larga tushib qolmasligi uchun toString'dan chiqarilgan. */
    @Override
    public String toString() {
        return "RegistrationRequest[company=" + companyName + ", email=" + email
                + ", locale=" + locale + ", modules=" + modules + "]";
    }
}