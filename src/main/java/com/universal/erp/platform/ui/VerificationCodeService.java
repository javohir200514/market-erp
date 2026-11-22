package com.universal.erp.platform.ui;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tasdiqlash kodlarini yaratadi va tekshiradi.
 * - kod 6 xonali, SecureRandom bilan; xotirada faqat tuzli hash saqlanadi (kodning o'zi emas);
 * - muddati (ttl) va urinishlar soni cheklangan, kod bir marta ishlatiladi;
 * - qayta yuborish orasida kutish (cooldown) va soatiga limit bor.
 *
 * Eslatma: holat xotirada saqlanadi — bitta server uchun yetarli. Bir nechta server (klaster) bo'lsa,
 * shu sinfni Redis yoki bazaga ko'chirish kerak.
 */
@Service
public class VerificationCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Duration ttl;
    private final Duration cooldown;
    private final Duration verifiedTtl;
    private final int maxAttempts;
    private final int maxPerHour;

    private final Map<String, Entry> entries = new HashMap<>();   // this bilan sinxronlanadi

    private static final class Entry {
        byte[] salt;
        byte[] hash;                          // null — faol kod yo'q
        Instant expires = Instant.EPOCH;
        int attempts;
        Instant lastSent = Instant.EPOCH;
        Instant windowStart = Instant.EPOCH;
        int sentInWindow;
        Instant verifiedUntil = Instant.EPOCH;
    }

    public VerificationCodeService(
            @Value("${uzmark.verification.ttl-minutes:10}") int ttlMinutes,
            @Value("${uzmark.verification.cooldown-seconds:60}") int cooldownSeconds,
            @Value("${uzmark.verification.verified-minutes:30}") int verifiedMinutes,
            @Value("${uzmark.verification.max-attempts:5}") int maxAttempts,
            @Value("${uzmark.verification.max-per-hour:5}") int maxPerHour) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        this.verifiedTtl = Duration.ofMinutes(verifiedMinutes);
        this.maxAttempts = maxAttempts;
        this.maxPerHour = maxPerHour;
    }

    public int ttlMinutes() {
        return (int) ttl.toMinutes();
    }

    /** Yangi kod yaratadi (eskisi bekor bo'ladi). Kutish yoki limit buzilsa VerificationException tashlaydi. */
    public synchronized String issue(String email) {
        Instant now = Instant.now();
        purge(now);
        Entry e = entries.computeIfAbsent(key(email), k -> new Entry());

        long wait = Duration.between(now, e.lastSent.plus(cooldown)).getSeconds();
        if (wait > 0) {
            throw new VerificationException(VerificationException.Reason.COOLDOWN, (int) Math.max(1, wait));
        }
        if (now.isAfter(e.windowStart.plus(WINDOW))) {
            e.windowStart = now;
            e.sentInWindow = 0;
        }
        if (e.sentInWindow >= maxPerHour) {
            int retry = (int) Math.max(1, Duration.between(now, e.windowStart.plus(WINDOW)).getSeconds());
            throw new VerificationException(VerificationException.Reason.TOO_MANY, retry);
        }

        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        e.salt = new byte[16];
        RANDOM.nextBytes(e.salt);
        e.hash = hash(e.salt, code);
        e.expires = now.plus(ttl);
        e.attempts = 0;
        e.lastSent = now;
        e.sentInWindow++;
        e.verifiedUntil = Instant.EPOCH;
        return code;
    }

    /** Xatni yuborib bo'lmasa, kodni bekor qiladi va urinishni hisobdan chiqaradi. */
    public synchronized void revoke(String email) {
        Entry e = entries.get(key(email));
        if (e == null) {
            return;
        }
        e.hash = null;
        e.lastSent = Instant.EPOCH;
        if (e.sentInWindow > 0) {
            e.sentInWindow--;
        }
    }

    /** Kod to'g'ri bo'lsa true; muvaffaqiyatda kod bir marta ishlatilgan hisoblanadi. */
    public synchronized boolean verify(String email, String code) {
        Instant now = Instant.now();
        Entry e = entries.get(key(email));
        if (e == null || e.hash == null || now.isAfter(e.expires) || code == null) {
            return false;
        }
        if (++e.attempts > maxAttempts) {
            e.hash = null;
            return false;
        }
        boolean ok = MessageDigest.isEqual(e.hash, hash(e.salt, code.trim()));
        if (ok) {
            e.hash = null;
            e.verifiedUntil = now.plus(verifiedTtl);
        }
        return ok;
    }

    /** Email tasdiqlangan va muddati o'tmaganmi? (holatni o'zgartirmaydi) */
    public synchronized boolean isVerified(String email) {
        Entry e = entries.get(key(email));
        return e != null && Instant.now().isBefore(e.verifiedUntil);
    }

    /** Email tasdiqlangan va muddati o'tmagan bo'lsa true; bir marta ishlatiladi (ro'yxatdan o'tish uchun). */
    public synchronized boolean consumeVerified(String email) {
        Entry e = entries.get(key(email));
        if (e == null || !Instant.now().isBefore(e.verifiedUntil)) {
            return false;
        }
        e.verifiedUntil = Instant.EPOCH;
        return true;
    }

    private void purge(Instant now) {
        entries.values().removeIf(e ->
                now.isAfter(e.expires) && now.isAfter(e.verifiedUntil) && now.isAfter(e.windowStart.plus(WINDOW)));
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] hash(byte[] salt, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}