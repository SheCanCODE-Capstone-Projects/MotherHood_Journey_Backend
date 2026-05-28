package com.motherhood.journey.notification.service;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live sandbox smoke-test — requires AT_API_KEY and AT_USERNAME env vars.
 * Run with: AT_API_KEY=<key> AT_USERNAME=sandbox mvn test -Dtest=AfricasTalkingClientSandboxTest
 */
class AfricasTalkingClientSandboxTest {

    private static final String API_KEY  =
            System.getenv().getOrDefault("AT_API_KEY",
                    "atsk_6ecd26d0f78341d7aeccf9dffc9f86276ccf573b520d17cd7b0c891a141c94318b52beb1");
    private static final String USERNAME = System.getenv().getOrDefault("AT_USERNAME", "sandbox");
    private static final String TEST_PHONE = "+250793094202";

    @Test
    void sendSms_sandbox_returnsMessageId() throws Exception {
        AfricasTalking.initialize(USERNAME, API_KEY);
        SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

        List<Recipient> recipients = sms.send(
                "MotherHood Journey sandbox test", new String[]{TEST_PHONE}, true);

        assertThat(recipients).isNotEmpty();
        Recipient first = recipients.getFirst();
        System.out.println("Status    : " + first.status);
        System.out.println("MessageId : " + first.messageId);
        System.out.println("Number    : " + first.number);
        System.out.println("Cost      : " + first.cost);

        assertThat(first.status).isNotBlank();
    }
}
