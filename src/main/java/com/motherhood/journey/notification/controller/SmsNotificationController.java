package com.motherhood.journey.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.notification.dto.request.DeliveryWebhookRequest;
import com.motherhood.journey.notification.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/at")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Webhooks (Africa's Talking)",
    description = "HMAC-validated delivery callbacks from Africa's Talking. Public, signature-checked.")
@Slf4j
public class SmsNotificationController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    // POST /webhooks/at/delivery
    // Receives delivery status callbacks from Africa's Talking
    // This endpoint is PUBLIC
    @PostMapping("/delivery")
    public ResponseEntity<Void> handleDeliveryStatus(
            // The raw request body as a String for signature validation
            @RequestBody String rawBody,

            // Africa's Talking sends their signature in this header
            @RequestHeader(
                    value = "X-AT-Signature",
                    required = false
            ) String signature) {

        // Validate the signature
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook received without signature — rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!webhookService.isValidSignature(rawBody, signature)) {
            log.warn("Webhook signature validation failed — rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        //  Parse the request body into our DTO
        try {
            DeliveryWebhookRequest request = objectMapper
                    .readValue(rawBody, DeliveryWebhookRequest.class);

            // Update the SMS notification status
            webhookService.handleDeliveryUpdate(request);

            // Return 200 OK to tell Africa's Talking we received it
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process delivery webhook: {}",
                    e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}