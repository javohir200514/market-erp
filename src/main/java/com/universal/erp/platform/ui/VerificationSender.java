package com.universal.erp.platform.ui;

/**
 * Tasdiqlash kodini yetkazish kanali. Hozir: email ({@link EmailVerificationSender})
 * va dasturlash uchun log ({@link LoggingVerificationSender}). Telegram qo'shilsa, shu interfeysni implement qilinadi.
 */
public interface VerificationSender {

    /**
     * @param destination email manzil (yoki boshqa kanalda — chat identifikatori)
     * @param lang        xat tili: uz / ru / en
     * @throws VerificationException (SEND_FAILED) yuborib bo'lmasa
     */
    void send(String destination, String code, String lang, int ttlMinutes);
}