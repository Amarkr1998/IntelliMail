package com.intellimail.mail.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real HTTP layer of the Stripe webhook endpoint: signature
 * verification and its public-endpoint wiring. The webhook secret in the
 * "test" profile is {@code whsec_test_dummy} (see application-test.yml); the
 * signature here is computed with the same public algorithm Stripe documents
 * (HMAC-SHA256 of "{timestamp}.{payload}"), not a Stripe SDK internal helper,
 * so this test has no dependency on Stripe SDK internals. The actual webhook
 * *business logic* (checkout completed, subscription updated/deleted) is
 * covered directly and more thoroughly by {@code BillingServiceTest}, which
 * constructs real Stripe model objects rather than raw JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingWebhookIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test_dummy";

    @Autowired
    private MockMvc mockMvc;

    private String signaturePayload(String payload, long timestamp) throws Exception {
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }

    @Test
    void webhook_withValidSignature_isAcceptedAndProcessed() throws Exception {
        String payload = """
                {"id":"evt_test_1","object":"event","type":"customer.created","data":{"object":{"id":"cus_test","object":"customer"}}}
                """.trim();
        String signatureHeader = signaturePayload(payload, Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signatureHeader)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_withInvalidSignature_isRejected() throws Exception {
        String payload = """
                {"id":"evt_test_2","object":"event","type":"customer.created","data":{"object":{"id":"cus_test","object":"customer"}}}
                """.trim();

        mockMvc.perform(post("/api/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=deadbeef")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
