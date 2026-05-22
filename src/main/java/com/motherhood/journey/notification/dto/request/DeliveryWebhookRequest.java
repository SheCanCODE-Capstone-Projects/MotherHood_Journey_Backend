package com.motherhood.journey.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryWebhookRequest {

    // Africa's Talking message ID
    private String id;

    // The phone number the SMS was sent to
    private String phoneNumber;

    // Possible values: "Success", "Failed", "Rejected"
    private String status;

    // Network the message was sent through
    private String networkCode;

    // Failure reason if status is Failed
    private String failureReason;

    // Number of SMS parts sent
    private String retryCount;
}