package com.paymentgateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.paymentgateway.dto.WebhookRequest;
import com.paymentgateway.service.WebhookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(
            WebhookService webhookService) {

        this.webhookService = webhookService;
    }

    @PostMapping("/payment")
    public ResponseEntity<String> paymentWebhook(
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader(
                    value = "X-Webhook-Signature",
                    required = false
            ) String signature) {

        String response =
                webhookService.processWebhook(
                        request,
                        signature
                );

        return ResponseEntity.ok(response);
    }
}

