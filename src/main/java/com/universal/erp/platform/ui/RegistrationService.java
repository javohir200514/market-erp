package com.universal.erp.platform.ui;

/**
 * Ro'yxatdan o'tish wizard'i shu interfeys orqali backend bilan gaplashadi.
 * Implementatsiya: {@link RegistrationServiceImpl}.
 */
public interface RegistrationService {

    /**
     * Emailga 6 xonali tasdiqlash kodini yuboradi.
     *
     * @param lang xat tili: uz / ru / en
     * @throws VerificationException kutilgan xatolarda (kutish vaqti, limit, email band, yuborib bo'lmadi)
     */
    void sendCode(String email, String lang);

    /** Kod to'g'ri va muddati o'tmagan bo'lsa true. Kod bir marta ishlatiladi. */
    boolean verifyCode(String email, String code);

    /**
     * Kompaniya, administrator va tanlangan modullarni yaratadi. Email avval tasdiqlangan bo'lishi shart.
     *
     * @return kompaniyaning tizimga kirish nomi (login sahifasidagi "Kompaniya nomi")
     */
    String register(RegistrationRequest request);
}