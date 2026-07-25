package com.intellimail.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring application context loads with all configuration
 * properties, beans and auto-configurations wired correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class IntelliMailApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failing context load fails this test.
    }
}
