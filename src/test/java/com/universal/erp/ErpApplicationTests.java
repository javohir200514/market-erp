package com.universal.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ErpApplicationTests {

    @Test
    void contextLoads() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

        String rawPassword = "javohir1401";

        String databaseHash =
                "$2a$12$x368LLs5SucrILeqzbfTbOw8iJNcVs0Saaozp47s7MUrUip.X7qnK";

        boolean result = passwordEncoder.matches(
                rawPassword,
                databaseHash
        );

        System.out.println("PASSWORD MATCH: " + result);
    }

}
