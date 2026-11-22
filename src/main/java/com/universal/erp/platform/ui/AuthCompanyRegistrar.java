package com.universal.erp.platform.ui;

import com.universal.erp.platform.security.AuthService;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wizard ma'lumotlarini {@link AuthService#register} ga o'tkazadi.
 *
 * Kompaniya nomi ("Nur Savdo") dan kirish nomi (slug, "nur-savdo") yasaladi: kichik harf, lotin harflari,
 * raqam va tire; kirill/o'zbek harflari transliteratsiya qilinadi. Nom band bo'lsa "-2", "-3" ... qo'shiladi.
 * Yakuniy nom foydalanuvchiga ko'rsatiladi (login sahifasida shu kerak bo'ladi).
 *
 * Eslatma: AuthService hozircha faqat kompaniya, administrator va uning ruxsatlarini yaratadi.
 * Tanlangan modullar, telefon va STIR hali saqlanmaydi (ular uchun jadval kerak) — pastdagi TODO ga qarang.
 */
@Component
public class AuthCompanyRegistrar implements CompanyRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AuthCompanyRegistrar.class);

    private static final int SLUG_MIN = 3;
    private static final int SLUG_MAX = 30;
    private static final int MAX_ATTEMPTS = 20;

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "yo"), Map.entry('ж', "j"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "x"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sh"), Map.entry('ъ', ""),
            Map.entry('ы', "i"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"), Map.entry('ў', "o"), Map.entry('қ', "q"), Map.entry('ғ', "g"),
            Map.entry('ҳ', "h"));

    private final AuthService authService;

    public AuthCompanyRegistrar(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public String register(RegistrationRequest request) {
        String base = slugify(request.companyName());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String slug = attempt == 1 ? base : withSuffix(base, attempt);
            try {
                authService.register(new AuthService.RegistrationRequest(
                        slug, request.fullName(), request.email(), request.password(), request.locale()));
                log.info("Yangi kompaniya ro'yxatdan o'tdi: {} ({}), modullar: {}",
                        slug, request.email(), request.modules());

                // TODO: request.modules(), request.phone(), request.taxId() ni saqlash.
                //   Buning uchun migratsiya (masalan, tenant_modules jadvali) va AuthRepository'da insert kerak.
                return slug;
            } catch (AuthService.CompanyAlreadyExistsException taken) {
                // nom band (yoki zahiradagi nom) — keyingi variantni sinaymiz
            }
        }
        throw new IllegalStateException("Kompaniya uchun bo'sh kirish nomi topilmadi: " + base);
    }

    // ------------------------------------------------------------------ nom -> slug

    static String slugify(String name) {
        String s = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[ʻʼ'’`´]", "");           // o'zbek tutuq belgilari: o'zbek -> ozbek

        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            String tr = TRANSLIT.get(c);
            sb.append(tr != null ? tr : String.valueOf(c));
        }
        s = Normalizer.normalize(sb, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");

        if (s.isEmpty()) {
            s = "company";
        }
        if (s.length() > SLUG_MAX) {
            s = s.substring(0, SLUG_MAX).replaceAll("-+$", "");
        }
        if (s.length() < SLUG_MIN) {
            s = s + "-erp";
        }
        return s;
    }

    private static String withSuffix(String base, int n) {
        String suffix = "-" + n;
        String cut = base.length() > SLUG_MAX - suffix.length()
                ? base.substring(0, SLUG_MAX - suffix.length()).replaceAll("-+$", "")
                : base;
        return cut + suffix;
    }
}