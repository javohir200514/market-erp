package com.universal.erp.platform.ui;

import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Kompaniyaning Ombor profilini va katalog nomlarini beradi: tanlangan yo'nalishlar
 * ({@link CompanyBusinessTypes}) bo'yicha. Bazaga yengil so'rov, shuning uchun keshlanmaydi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/WarehouseProfiles.java
 */
@Service
public class WarehouseProfiles {

    private final CompanyBusinessTypes businessTypes;

    public WarehouseProfiles(CompanyBusinessTypes businessTypes) {
        this.businessTypes = businessTypes;
    }

    /** Joriy kompaniya (kirgan foydalanuvchi) uchun profil. */
    public WarehouseProfile current() {
        return forCompany(CurrentCompany.get());
    }

    /** Berilgan kompaniya kirish nomi uchun profil. */
    public WarehouseProfile forCompany(String companyLogin) {
        return WarehouseProfile.of(types(companyLogin));
    }

    public CatalogTerms.Terms termsFor(String companyLogin) {
        return CatalogTerms.of(types(companyLogin), AppTexts.lang());
    }

    public CatalogTerms.Terms terms() {
        return termsFor(CurrentCompany.get());
    }

    private Set<BusinessType> types(String companyLogin) {
        return businessTypes.find(companyLogin);
    }
}