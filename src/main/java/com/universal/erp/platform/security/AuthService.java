package com.universal.erp.platform.security;

import com.universal.erp.platform.security.AuthRepository.UserRow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Login va ro'yxatdan o'tish uchun yagona servis.
 * UI (LoginView, RegisterView) faqat shu servis bilan ishlaydi.
 */
@Service
public class AuthService {

    // ------------------------------------------------------------------ ro'yxatdan o'tish shartnomasi

    /**
     * @param companySlug kompaniya nomi (login uchun); server tomonda qayta tekshiriladi
     * @param fullName    "Ism Familiya" — birinchi so'z ism, qolgani familiya
     * @param password    ochiq parol — faqat hash qilish uchun, log qilinmaydi
     * @param locale      uz / ru / en
     */
    public record RegistrationRequest(
            String companySlug,
            String fullName,
            String email,
            String password,
            String locale) {

        @Override
        public String toString() {
            return "RegistrationRequest[companySlug=" + companySlug
                    + ", fullName=" + fullName
                    + ", email=" + email
                    + ", locale=" + locale + "]";
        }
    }

    public static class CompanyAlreadyExistsException extends RuntimeException {
        public CompanyAlreadyExistsException(String companySlug) {
            super("Company already exists: " + companySlug);
        }
    }

    // ------------------------------------------------------------------ sozlamalar

    static final String ADMIN_ROLE = "ADMIN";
    private static final int TRIAL_DAYS = 14;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private static final int SLUG_MIN = 3;
    private static final int SLUG_MAX = 30;
    private static final int NAME_MAX = 100;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 72; // BCrypt chegarasi

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> SUPPORTED_LOCALES = Set.of("uz", "ru", "en");
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "app", "www", "login", "register", "logout", "static",
            "support", "help", "platform", "system", "root", "billing", "mail");

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    /** Foydalanuvchi topilmaganda ham bir xil vaqt sarflash uchun (timing orqali aniqlanmasin). */
    private final String dummyHash;

    public AuthService(AuthRepository repository, ObjectProvider<PasswordEncoder> passwordEncoderProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoderProvider
                .getIfAvailable(PasswordEncoderFactories::createDelegatingPasswordEncoder);
        this.dummyHash = this.passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ================================================================ ro'yxatdan o'tish

    /**
     * Yangi kompaniya (tenant), uning tizim ADMIN roli (barcha ruxsatlar bilan), birinchi
     * administrator va audit yozuvini BITTA tranzaksiyada yaratadi.
     * Tenant ID bu yerda yaratiladi (klientdan olinmaydi).
     */
    @Transactional
    public void register(RegistrationRequest request) {
        String slug = normalize(request.companySlug());
        String email = normalize(request.email());
        String fullName = request.fullName() == null ? "" : request.fullName().trim();
        String password = request.password() == null ? "" : request.password();
        String locale = SUPPORTED_LOCALES.contains(request.locale()) ? request.locale() : "uz";

        if (slug.length() < SLUG_MIN || slug.length() > SLUG_MAX || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("Invalid company name");
        }
        if (RESERVED_SLUGS.contains(slug)) {
            throw new CompanyAlreadyExistsException(slug);
        }

        String[] nameParts = fullName.split("\\s+", 2);
        if (nameParts.length < 2 || nameParts[0].length() > NAME_MAX || nameParts[1].length() > NAME_MAX) {
            throw new IllegalArgumentException("Invalid full name");
        }
        String firstName = nameParts[0];
        String lastName = nameParts[1];

        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (!isStrong(password)) {
            throw new IllegalArgumentException("Weak password");
        }

        String code = slug.toUpperCase(Locale.ROOT); // V3: tenants.code = UPPER(slug)
        if (repository.tenantExists(slug, code)) {
            throw new CompanyAlreadyExistsException(slug);
        }

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        try {
            repository.insertTenant(tenantId, slug, code, slug, email, locale,
                    OffsetDateTime.now().plusDays(TRIAL_DAYS));
            repository.insertRole(roleId, tenantId, ADMIN_ROLE, "Administrator",
                    "Kompaniya administratori — barcha ruxsatlar");
            repository.grantAllPermissions(roleId);
            repository.insertUser(userId, tenantId, email, firstName, lastName,
                    passwordEncoder.encode(password), locale);
            repository.assignRole(UUID.randomUUID(), tenantId, userId, roleId);
            repository.auditTenantRegistered(tenantId, userId, email, slug);
        } catch (DuplicateKeyException exception) {
            // Parallel so'rov bir vaqtda xuddi shu nomni olgan bo'lsa
            throw new CompanyAlreadyExistsException(slug);
        }
    }

    // ================================================================ login

    /**
     * Kompaniya + email + parol bo'yicha kiritadi va SecurityContext'ni sessiyaga saqlaydi.
     *
     * @throws BadCredentialsException noto'g'ri kompaniya / email / parol (sababi oshkor qilinmaydi)
     * @throws LockedException         ko'p xato urinishdan keyin vaqtincha bloklangan
     * @throws DisabledException       hisob yoki kompaniya faol emas
     */

    public void login(
            String companySlug,
            String email,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1. Company va email ma'lumotlarini normallashtirish
        String slug = normalize(companySlug);
        String mail = normalize(email);

        if (slug.isEmpty() || mail.isEmpty() || password == null) {
            throw new BadCredentialsException("Bad credentials");
        }

        // 2. Agar xodim username orqali kirayotgan bo'lsa,
        // uni to'liq email formatiga o'tkazish
        if (!mail.contains("@")) {
            mail = mail + "@" + slug + UserAdminService.USERNAME_DOMAIN;
        }

        // 3. Foydalanuvchini bazadan qidirish
        Optional<UserRow> found = repository.findUser(slug, mail);

        // 4. Foydalanuvchi topilmagan bo'lsa ham BCrypt tekshiruvini bajarish
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            throw new BadCredentialsException("Bad credentials");
        }

        UserRow user = found.get();

        // 5. Hisob bloklanganligini tekshirish
        if (user.lockedUntil() != null
                && user.lockedUntil().isAfter(OffsetDateTime.now())) {
            throw new LockedException("Account locked");
        }

        // 6. Bazadagi hashni olish
        String storedHash = user.passwordHash();
        System.out.println(storedHash);
        // 7. Parolni tekshirish
        boolean passwordMatches;

        if (storedHash == null || storedHash.isBlank()) {
            passwordEncoder.matches(password, dummyHash);
            passwordMatches = false;
        } else {
            passwordMatches = passwordEncoder.matches(password, storedHash);
            System.out.println(passwordMatches);
            System.out.println(storedHash);
        }

        if (!passwordMatches) {
            repository.recordFailedAttempt(
                    user.id(),
                    5,
                    15
            );

            throw new BadCredentialsException("Bad credentials");
        }

        // 9. Foydalanuvchi va tenant holatini tekshirish
        if (!"ACTIVE".equals(user.status())
                || !"ACTIVE".equals(user.tenantStatus())) {
            throw new DisabledException("Account disabled");
        }

        // 10. Muvaffaqiyatli login ma'lumotlarini saqlash
        repository.markLoggedIn(user.id());

        // 11. Foydalanuvchining rollarini olish
        List<String> roleCodes = List.copyOf(
                repository.findRoleCodes(
                        user.tenantId(),
                        user.id()
                )
        );

        // 12. Foydalanuvchining ruxsatlarini olish
        List<String> permissionCodes = List.copyOf(
                repository.findPermissionCodes(
                        user.tenantId(),
                        user.id()
                )
        );

        // 13. AuthenticatedUser principal yaratish
        AuthenticatedUser principal = new AuthenticatedUser(
                user.id(),
                user.tenantId(),
                user.tenantSlug(),
                user.email(),
                (user.firstName() + " " + user.lastName()).trim(),
                user.locale(),
                user.timeZone(),
                roleCodes,
                permissionCodes
        );

        // 14. Authentication yaratish
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        // 15. Session fixation hujumidan himoyalanish
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        // 16. SecurityContext yaratish
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        // 17. Joriy thread uchun SecurityContext o'rnatish
        SecurityContextHolder
                .getContextHolderStrategy()
                .setContext(context);

        // 18. SecurityContext'ni session/repository'ga saqlash
        securityContextRepository.saveContext(
                context,
                request,
                response
        );
    }



    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isStrong(String password) {
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return letter && digit;
    }
}