package com.universal.erp.platform.security;

import com.universal.erp.platform.security.UserAdminRepository.MemberRow;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kompaniya xodimlari: yaratish (kirish nomi va parolni tizim o'zi yaratadi), rol berish,
 * parolni yangilash, bloklash. Faqat ADMIN chaqira oladi.
 *
 * Kirish nomi: "ali.valiyev" — login'da kompaniya nomi bilan birga yoziladi. Bazada
 * "ali.valiyev@{kompaniya}.local" ko'rinishida saqlanadi (email ustuni).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/security/UserAdminService.java
 */
@Service
public class UserAdminService {

    /** Kirish nomi uchun domen qismi: AuthService.login ham shuni ishlatadi. */
    public static final String USERNAME_DOMAIN = ".local";

    public record Member(UUID id, String login, String email, String fullName, List<String> roles,
                         boolean blocked, OffsetDateTime lastLogin) {
    }

    /** Yaratilgan hisob: parol faqat shu paytda ma'lum. */
    public record Credentials(String username, String password) {
    }

    public static class EmailTakenException extends RuntimeException {
        public EmailTakenException() {
            super("Email already used");
        }
    }

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> LOCALES = Set.of("uz", "ru", "en");
    private static final String PW_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int PW_LENGTH = 10;

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

    private final UserAdminRepository repository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public UserAdminService(UserAdminRepository repository,
                            AuthRepository authRepository,
                            ObjectProvider<PasswordEncoder> passwordEncoderProvider) {
        this.repository = repository;
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoderProvider
                .getIfAvailable(PasswordEncoderFactories::createDelegatingPasswordEncoder);
    }

    // ------------------------------------------------------------------ ro'yxat

    public List<Member> list(String tenantSlug) {
        requireAdmin();
        UUID tenantId = tenant(tenantSlug);
        String suffix = "@" + tenantSlug + USERNAME_DOMAIN;
        return repository.listMembers(tenantId).stream().map(r -> toMember(r, suffix)).toList();
    }

    private static Member toMember(MemberRow r, String suffix) {
        String login = r.email().toLowerCase(Locale.ROOT).endsWith(suffix)
                ? r.email().substring(0, r.email().length() - suffix.length())
                : r.email();
        String name = ((r.firstName() == null ? "" : r.firstName()) + " "
                + (r.lastName() == null ? "" : r.lastName())).trim();
        return new Member(r.id(), login, r.email(), name, r.roles(), r.blocked(), r.lastLoginAt());
    }

    // ------------------------------------------------------------------ yaratish

    /**
     * @param realEmail bo'sh bo'lsa, kirish nomi ism-familiyadan yaratiladi
     * @throws EmailTakenException   email band
     * @throws IllegalArgumentException ism yoki email noto'g'ri
     */
    @Transactional
    public Credentials create(String tenantSlug, String fullName, String realEmail,
                              String roleCode, String roleName, String roleDescription, String locale) {
        requireAdmin();
        UUID tenantId = tenant(tenantSlug);
        requireKnownRole(roleCode);

        String[] parts = (fullName == null ? "" : fullName.trim()).split("\\s+", 2);
        if (parts.length < 2 || parts[0].length() > 100 || parts[1].length() > 100) {
            throw new IllegalArgumentException("Invalid full name");
        }

        String login;
        String stored;
        String mail = realEmail == null ? "" : realEmail.trim().toLowerCase(Locale.ROOT);
        if (!mail.isEmpty()) {
            if (mail.length() > 255 || !EMAIL.matcher(mail).matches()) {
                throw new IllegalArgumentException("Invalid email");
            }
            if (repository.emailExists(tenantId, mail)) {
                throw new EmailTakenException();
            }
            login = mail;
            stored = mail;
        } else {
            String base = usernameBase(parts[0], parts[1]);
            String candidate = base;
            int n = 1;
            while (repository.emailExists(tenantId, candidate + "@" + tenantSlug + USERNAME_DOMAIN)) {
                candidate = base + (++n);
            }
            login = candidate;
            stored = candidate + "@" + tenantSlug + USERNAME_DOMAIN;
        }

        String password = generatePassword();
        UUID userId = UUID.randomUUID();
        try {
            authRepository.insertUser(userId, tenantId, stored, parts[0], parts[1],
                    passwordEncoder.encode(password), LOCALES.contains(locale) ? locale : "uz");
        } catch (DuplicateKeyException ex) {
            throw new EmailTakenException();
        }
        authRepository.assignRole(UUID.randomUUID(), tenantId, userId,
                ensureRole(tenantId, roleCode, roleName, roleDescription));
        return new Credentials(login, password);
    }

    // ------------------------------------------------------------------ boshqarish

    /** Yangi parol yaratadi va qaytaradi (faqat shu paytda ko'rinadi). */
    @Transactional
    public String resetPassword(String tenantSlug, UUID userId) {
        requireAdmin();
        UUID tenantId = tenant(tenantSlug);
        requireMember(tenantId, userId);
        String password = generatePassword();
        repository.setPassword(tenantId, userId, passwordEncoder.encode(password));
        return password;
    }

    @Transactional
    public void changeRole(String tenantSlug, UUID userId, String roleCode, String roleName, String roleDescription) {
        requireAdmin();
        UUID tenantId = tenant(tenantSlug);
        requireMember(tenantId, userId);
        requireNotSelf(tenantId, userId);
        requireKnownRole(roleCode);
        UUID roleId = ensureRole(tenantId, roleCode, roleName, roleDescription);
        repository.clearRoles(tenantId, userId);
        authRepository.assignRole(UUID.randomUUID(), tenantId, userId, roleId);
    }

    @Transactional
    public void setBlocked(String tenantSlug, UUID userId, boolean blocked) {
        requireAdmin();
        UUID tenantId = tenant(tenantSlug);
        requireMember(tenantId, userId);
        requireNotSelf(tenantId, userId);
        repository.setBlocked(tenantId, userId, blocked);
    }

    // ------------------------------------------------------------------ yordamchilar

    private UUID ensureRole(UUID tenantId, String code, String name, String description) {
        Optional<UUID> existing = repository.findRoleId(tenantId, code);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID id = UUID.randomUUID();
        try {
            authRepository.insertRole(id, tenantId, code, name, description);
            return id;
        } catch (DuplicateKeyException ex) {
            return repository.findRoleId(tenantId, code).orElseThrow();
        }
    }

    private UUID tenant(String slug) {
        return repository.tenantIdBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Unknown company"));
    }

    private void requireMember(UUID tenantId, UUID userId) {
        if (!repository.userInTenant(tenantId, userId)) {
            throw new IllegalArgumentException("Unknown user");
        }
    }

    /** Joriy foydalanuvchi shu hisobmi? (kirish nomi email yoki ID bo'yicha solishtiriladi) */
    public static boolean isSelf(String email, UUID id) {
        org.springframework.security.core.Authentication a =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) {
            return false;
        }
        return a.getName().equalsIgnoreCase(email) || a.getName().equals(String.valueOf(id));
    }

    private void requireNotSelf(UUID tenantId, UUID userId) {
        String email = repository.findEmail(tenantId, userId).orElse("");
        if (isSelf(email, userId)) {
            throw new IllegalArgumentException("Cannot change own account");
        }
    }

    private static void requireKnownRole(String code) {
        if (code == null || !RoleChecks.KNOWN.contains(code)) {
            throw new IllegalArgumentException("Unknown role");
        }
    }

    private static void requireAdmin() {
        if (!RoleChecks.isAdmin()) {
            throw new AccessDeniedException("Administrator only");
        }
    }

    private String generatePassword() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            boolean letter = false;
            boolean digit = false;
            for (int i = 0; i < PW_LENGTH; i++) {
                char c = PW_ALPHABET.charAt(random.nextInt(PW_ALPHABET.length()));
                sb.append(c);
                letter |= Character.isLetter(c);
                digit |= Character.isDigit(c);
            }
            if (letter && digit) {
                return sb.toString();
            }
        }
    }

    /** "Ali", "Valiyev" -> "ali.valiyev" (kirill va diakritiklar lotinga o'giriladi). */
    static String usernameBase(String first, String last) {
        String a = latin(first);
        String b = latin(last.split("\\s+")[0]);
        String base = (a + (a.isEmpty() || b.isEmpty() ? "" : ".") + b);
        if (base.isEmpty()) {
            base = "user";
        }
        return base.length() > 40 ? base.substring(0, 40) : base;
    }

    private static String latin(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            String t = TRANSLIT.get(c);
            out.append(t != null ? t : String.valueOf(c));
        }
        String n = Normalizer.normalize(out.toString(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("[^a-z0-9]", "");
    }
}