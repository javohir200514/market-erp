package com.universal.erp.platform.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Faqat dasturlash uchun: kod emailga yuborilmaydi, server logiga yoziladi.
 * uzmark.verification.channel=log bo'lsa (yoki sozlanmagan bo'lsa) ishlaydi.
 * Ishlab chiqarishda uzmark.verification.channel=email qiling.
 */
@Component
@ConditionalOnProperty(name = "uzmark.verification.channel", havingValue = "log", matchIfMissing = true)
public class LoggingVerificationSender implements VerificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationSender.class);

    @Override
    public void send(String destination, String code, String lang, int ttlMinutes) {
        log.warn("[DEV] {} uchun tasdiqlash kodi: {} ({} daqiqa amal qiladi)", destination, code, ttlMinutes);
    }
}