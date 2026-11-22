package com.universal.erp.platform.ui;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Tasdiqlash kodini SMTP orqali emailga yuboradi (matn + HTML ko'rinishida).
 * uzmark.verification.channel=email bo'lganda yoqiladi; SMTP sozlamalari spring.mail.* da.
 */
@Component
@ConditionalOnProperty(name = "uzmark.verification.channel", havingValue = "email")
public class EmailVerificationSender implements VerificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public EmailVerificationSender(JavaMailSender mailSender,
                                   @Value("${uzmark.mail.from}") String from,
                                   @Value("${uzmark.mail.from-name:Uz-mark ERP}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public void send(String destination, String code, String lang, int ttlMinutes) {
        VerificationMail.Content content = VerificationMail.build(code, lang, ttlMinutes);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(destination);
            helper.setSubject(content.subject());
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            // kodni log'ga yozmaymiz; faqat sabab va manzil
            log.error("Tasdiqlash xatini yuborib bo'lmadi ({}): {}", destination, ex.getMessage());
            throw new VerificationException(VerificationException.Reason.SEND_FAILED, ex);
        }
    }
}