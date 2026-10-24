
package com.paymentgateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.paymentgateway.dto.PaymentSummaryResponse;
import com.paymentgateway.entity.User;
import com.paymentgateway.repository.UserRepository;
import com.paymentgateway.service.PaymentSummaryService;

@RestController
@RequestMapping("/api/payments")
public class PaymentSummaryController {

    private final PaymentSummaryService paymentSummaryService;
    private final UserRepository userRepository;

    public PaymentSummaryController(
            PaymentSummaryService paymentSummaryService,
            UserRepository userRepository) {
        this.paymentSummaryService = paymentSummaryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary(
            @PathVariable Long userId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }

        User loggedInUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Logged-in user not found"
                ));

        if (!loggedInUser.getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view your own payment summary"
            );
        }

        PaymentSummaryResponse summary =
                paymentSummaryService.getPaymentSummary(userId);

        return ResponseEntity.ok(summary);
    }
}