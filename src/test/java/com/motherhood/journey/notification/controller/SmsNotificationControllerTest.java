package com.motherhood.journey.notification.controller;

import com.motherhood.journey.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HMAC signature validation is SECURITY-CRITICAL:
 *  - missing header -> 401
 *  - invalid signature -> 401
 *  - valid signature -> 200 (or 500 if downstream fails; we expect 200 with valid body)
 */
@AutoConfigureMockMvc
@Transactional
class SmsNotificationControllerTest extends IntegrationTestBase {

    private static final String AT_API_KEY = "test-key";

    @Autowired MockMvc mockMvc;

    private static final String VALID_BODY = """
        {"id":"AT-MSG-1","phoneNumber":"+250700000001","status":"Success",
         "networkCode":"63510","failureReason":null,"retryCount":"0"}
        """;

    private static String hmacSha256Base64(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void deliveryWebhook_missingSignature_returns401() throws Exception {
        mockMvc.perform(post("/webhooks/at/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deliveryWebhook_blankSignature_returns401() throws Exception {
        mockMvc.perform(post("/webhooks/at/delivery")
                .header("X-AT-Signature", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deliveryWebhook_invalidSignature_returns401() throws Exception {
        mockMvc.perform(post("/webhooks/at/delivery")
                .header("X-AT-Signature", "not-a-valid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deliveryWebhook_validSignature_returns200() throws Exception {
        String signature = hmacSha256Base64(VALID_BODY, AT_API_KEY);

        mockMvc.perform(post("/webhooks/at/delivery")
                .header("X-AT-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isOk());
    }

    @Test
    void deliveryWebhook_validSignatureButMalformedBody_returns500() throws Exception {
        // Valid signature is computed over the malformed body, so signature check passes,
        // but JSON parsing fails -> 500
        String malformed = "{not valid json";
        String signature = hmacSha256Base64(malformed, AT_API_KEY);

        mockMvc.perform(post("/webhooks/at/delivery")
                .header("X-AT-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformed))
            .andExpect(status().isInternalServerError());
    }
}
