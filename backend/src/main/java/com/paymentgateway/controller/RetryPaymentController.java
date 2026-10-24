package com.paymentgateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.paymentgateway.dto.RetryPaymentRequest;
import com.paymentgateway.dto.RetryPaymentResponse;
import com.paymentgateway.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class RetryPaymentController {

    private final PaymentService paymentService;

    public RetryPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/retry")
    public ResponseEntity<RetryPaymentResponse> retryPayment(
            @Valid @RequestBody RetryPaymentRequest request) {

        RetryPaymentResponse response =
                paymentService.retryPayment(request);

        return ResponseEntity.ok(response);
    }
}
