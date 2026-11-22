package com.universal.erp.infrastructure;

import com.universal.erp.platform.tenant.TenantRegistrationDto;
import com.universal.erp.platform.tenant.TenantRegistrationService;
import com.universal.erp.platform.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** DEV ONLY (profile "dev"): creates tenant "demo" with admin admin@demo.uz. Never enable in production. */
@Component
@Profile("dev")
public class DevDataRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevDataRunner.class);

    private final TenantRegistrationService registrationService;
    private final TenantRepository tenantRepository;

    public DevDataRunner(TenantRegistrationService registrationService, TenantRepository tenantRepository) {
        this.registrationService = registrationService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void run(String... args) {
        if (tenantRepository.existsBySlugIgnoreCase("demo")) return;
        registrationService.register(new TenantRegistrationDto.Request(
                "Demo Company", "demo", "admin@demo.uz", "Demo", "Admin",
                "ChangeMe-12345", "uz", "Asia/Tashkent"));
        log.info("Dev tenant created: slug=demo, email=admin@demo.uz, password=ChangeMe-12345");
    }
}