package com.paymentgateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paymentgateway.dto.WebhookRequest;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.exception.BadRequestException;
import com.paymentgateway.exception.ResourceNotFoundException;
import com.paymentgateway.exception.UnauthorizedException;
import com.paymentgateway.repository.PaymentRepository;

@Service
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final String webhookSecret;

    public WebhookService(
            PaymentRepository paymentRepository,
            @Value("${webhook.secret}") String webhookSecret) {

        this.paymentRepository = paymentRepository;
        this.webhookSecret = webhookSecret;
    }

    public String processWebhook(
            WebhookRequest request,
            String signature) {

        // Verify webhook signature
        if (!isValidSignature(request, signature)) {

            throw new UnauthorizedException(
                    "Invalid webhook signature"
            );
        }

        Payment payment =
                paymentRepository
                        .findByTransactionId(
                                request.getTransactionId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found"
                                )
                        );

        // Only pending payments can receive
        // webhook status updates
        if (payment.getStatus()
                != PaymentStatus.PENDING) {

            throw new BadRequestException(
                    "Only pending payments can be updated through webhook"
            );
        }

        String status =
                request.getStatus()
                        .toUpperCase();

        if (status.equals("SUCCESS")) {

            payment.setStatus(
                    PaymentStatus.SUCCESS
            );

            payment.setFailureReason(null);

        } else if (status.equals("FAILED")) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            payment.setFailureReason(
                    "Payment failed through webhook"
            );

        } else {

            throw new BadRequestException(
                    "Invalid webhook status. Use SUCCESS or FAILED"
            );
        }

        paymentRepository.save(payment);

        return "Payment status updated successfully";
    }

    private boolean isValidSignature(
            WebhookRequest request,
            String receivedSignature) {

        if (receivedSignature == null
                || receivedSignature.isBlank()) {

            return false;
        }

        try {

            String payload =
                    request.getTransactionId()
                    + "|"
                    + request.getStatus();

            Mac mac =
                    Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            webhookSecret.getBytes(
                                    StandardCharsets.UTF_8),
                            "HmacSHA256"
                    );

            mac.init(secretKey);

            byte[] hash =
                    mac.doFinal(
                            payload.getBytes(
                                    StandardCharsets.UTF_8)
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return MessageDigest.isEqual(
                    hex.toString()
                            .getBytes(StandardCharsets.UTF_8),
                    receivedSignature
                            .getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {

            return false;
        }
    }
}

