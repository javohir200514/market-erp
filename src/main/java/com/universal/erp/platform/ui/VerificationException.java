package com.universal.erp.platform.ui;

/** Tasdiqlash kodini yuborishdagi kutilgan xatolar (UI ularni foydalanuvchiga tushunarli matn bilan ko'rsatadi). */
public class VerificationException extends RuntimeException {

    public enum Reason {
        /** Kod yaqinda yuborilgan — retryAfterSeconds dan keyin qayta yuborish mumkin. */
        COOLDOWN,
        /** Soatiga ruxsat etilgan kodlar soni tugadi. */
        TOO_MANY,
        /** Bu email bilan hisob allaqachon mavjud. */
        EMAIL_TAKEN,
        /** Xat yuborib bo'lmadi (SMTP xatosi va h.k.). */
        SEND_FAILED
    }

    private final Reason reason;
    private final int retryAfterSeconds;

    public VerificationException(Reason reason, int retryAfterSeconds) {
        super(reason.name());
        this.reason = reason;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public VerificationException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
        this.retryAfterSeconds = 0;
    }

    public Reason reason() {
        return reason;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}