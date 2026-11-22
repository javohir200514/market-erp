
package com.universal.erp.platform.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log =
            LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final VerificationCodeService codes;
    private final VerificationSender sender;
    private final ObjectProvider<CompanyRegistrar> registrar;

    public RegistrationServiceImpl(
            VerificationCodeService codes,
            VerificationSender sender,
            ObjectProvider<CompanyRegistrar> registrar) {
        this.codes = codes;
        this.sender = sender;
        this.registrar = registrar;
    }

    @Override
    public void sendCode(String email, String lang) {
        CompanyRegistrar companyRegistrar = registrar.getIfAvailable();

        if (companyRegistrar == null) {
            throw new IllegalStateException(
                    "CompanyRegistrar bean topilmadi"
            );
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (companyRegistrar.emailExists(normalizedEmail)) {
            throw new VerificationException(
                    VerificationException.Reason.EMAIL_TAKEN, 0
            );
        }

        String code = codes.issue(normalizedEmail);

        try {
            sender.send(
                    normalizedEmail,
                    code,
                    lang,
                    codes.ttlMinutes()
            );
        } catch (RuntimeException ex) {
            codes.revoke(normalizedEmail);

            if (ex instanceof VerificationException) {
                throw ex;
            }

            throw new VerificationException(
                    VerificationException.Reason.SEND_FAILED,
                    ex
            );
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return codes.verify(email.trim().toLowerCase(), code);
    }

    @Override
    public String register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase();

        if (!codes.isVerified(email)) {
            throw new IllegalStateException(
                    "Email tasdiqlanmagan yoki tasdiqlash muddati o'tgan"
            );
        }

        CompanyRegistrar companyRegistrar = registrar.getIfAvailable();

        if (companyRegistrar == null) {
            log.error("CompanyRegistrar bean topilmadi");
            throw new IllegalStateException(
                    "Registratsiya xizmati mavjud emas"
            );
        }

        log.info("Registration started: email={}", email);

        String companyLogin = companyRegistrar.register(request);

        if (companyLogin == null || companyLogin.isBlank()) {
            log.error(
                    "Registration returned empty company login: email={}",
                    email
            );
            throw new IllegalStateException(
                    "Kompaniya login nomi yaratilmadi"
            );
        }

        codes.consumeVerified(email);

        log.info(
                "Registration completed: company={}, email={}",
                companyLogin,
                email
        );

        return companyLogin;
    }
}
