package com.paymentgateway.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.paymentgateway.dto.RefundHistoryResponse;
import com.paymentgateway.dto.RefundRequest;
import com.paymentgateway.dto.RefundResponse;
import com.paymentgateway.service.RefundService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<RefundResponse> processRefund(
            @Valid @RequestBody RefundRequest request) {

        RefundResponse response =
                refundService.processRefund(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RefundHistoryResponse>> getAllRefunds() {

        List<RefundHistoryResponse> refunds =
                refundService.getAllRefunds();

        return ResponseEntity.ok(refunds);
    }
}