package com.universal.erp.platform.ui;

/** Tasdiqlash xati matni (uz / ru / en): oddiy matn va HTML ko'rinishi. */
final class VerificationMail {

    record Content(String subject, String text, String html) {
    }

    // [0] mavzu, [1] sarlavha, [2] kirish, [3] muddat ({min}), [4] "siz yubormagan bo'lsangiz", [5] ogohlantirish
    private static final String[] UZ = {
            "Uz-mark: tasdiqlash kodi",
            "Emailingizni tasdiqlang",
            "Uz-mark ERP'da kompaniyani ro'yxatdan o'tkazishni yakunlash uchun quyidagi koddan foydalaning:",
            "Kod {min} daqiqa amal qiladi.",
            "Agar bu so'rovni siz yubormagan bo'lsangiz, xatga e'tibor bermang.",
            "Kodni hech kimga bermang."
    };
    private static final String[] RU = {
            "Uz-mark: код подтверждения",
            "Подтвердите email",
            "Чтобы завершить регистрацию компании в Uz-mark ERP, используйте этот код:",
            "Код действует {min} мин.",
            "Если вы не отправляли этот запрос, просто проигнорируйте письмо.",
            "Никому не сообщайте код."
    };
    private static final String[] EN = {
            "Uz-mark: verification code",
            "Verify your email",
            "Use this code to finish registering your company in Uz-mark ERP:",
            "The code is valid for {min} minutes.",
            "If you didn't request this, you can safely ignore this email.",
            "Never share this code with anyone."
    };

    private static final String HTML = """
            <!doctype html>
            <html>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;">
                <tr><td align="center" style="padding:32px 16px;">
                  <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                         style="width:100%;max-width:480px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;">
                    <tr><td style="padding:28px 32px 0 32px;">
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td style="width:34px;height:34px;background:#f0b04a;border-radius:8px;text-align:center;
                                   font-size:18px;font-weight:700;color:#1a1405;">U</td>
                        <td style="padding-left:10px;font-size:19px;font-weight:700;color:#111827;">Uz-mark</td>
                      </tr></table>
                    </td></tr>
                    <tr><td style="padding:24px 32px 0 32px;">
                      <h1 style="margin:0;font-size:22px;line-height:1.3;color:#111827;">{{heading}}</h1>
                      <p style="margin:12px 0 0 0;font-size:15px;line-height:1.6;color:#4b5563;">{{intro}}</p>
                    </td></tr>
                    <tr><td align="center" style="padding:24px 32px 0 32px;">
                      <div style="display:inline-block;padding:16px 28px;background:#0e131a;border-radius:12px;
                                  font-family:Consolas,'Courier New',monospace;font-size:32px;font-weight:700;
                                  letter-spacing:6px;color:#f0b04a;">{{code}}</div>
                    </td></tr>
                    <tr><td style="padding:20px 32px 0 32px;">
                      <p style="margin:0;font-size:14px;color:#4b5563;">{{validity}}</p>
                      <p style="margin:8px 0 0 0;font-size:14px;font-weight:700;color:#b45309;">{{warn}}</p>
                    </td></tr>
                    <tr><td style="padding:24px 32px 28px 32px;">
                      <p style="margin:0;padding-top:16px;border-top:1px solid #e5e7eb;font-size:13px;color:#9ca3af;">{{ignore}}</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;

    private VerificationMail() {
    }

    static Content build(String code, String lang, int ttlMinutes) {
        String[] s = switch (lang == null ? "uz" : lang) {
            case "ru" -> RU;
            case "en" -> EN;
            default -> UZ;
        };
        String shown = code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code;
        String validity = s[3].replace("{min}", String.valueOf(ttlMinutes));

        String text = s[1] + "\n\n" + s[2] + "\n\n    " + shown + "\n\n" + validity + "\n" + s[5] + "\n\n" + s[4] + "\n";
        String html = HTML
                .replace("{{heading}}", s[1])
                .replace("{{intro}}", s[2])
                .replace("{{code}}", shown)
                .replace("{{validity}}", validity)
                .replace("{{warn}}", s[5])
                .replace("{{ignore}}", s[4]);
        return new Content(s[0], text, html);
    }
}