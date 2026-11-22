package com.universal.erp.platform.ui;

/**
 * Kompaniya va administrator foydalanuvchisini bazaga yozadigan qatlam.
 * Standart implementatsiya: {@link AuthCompanyRegistrar} (AuthService orqali).
 */
public interface CompanyRegistrar {

    /**
     * Bu email bilan foydalanuvchi allaqachon bormi? Kod yuborishdan oldin tekshiriladi.
     * Standart: false — tizimda email kompaniya doirasida noyob, shuning uchun bir email turli kompaniyalarda bo'la oladi.
     */
    default boolean emailExists(String email) {
        return false;
    }

    /**
     * Kompaniya, administrator va (imkon bo'lsa) tanlangan modullarni yaratadi.
     *
     * @return kompaniyaning tizimga kirish nomi (slug). Foydalanuvchi login sahifasida shuni kiritadi.
     */
    String register(RegistrationRequest request);
}