
package com.paymentgateway.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.dto.PaymentStatusResponse;
import com.paymentgateway.dto.RetryPaymentRequest;
import com.paymentgateway.dto.RetryPaymentResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.User;
import com.paymentgateway.exception.BadRequestException;
import com.paymentgateway.exception.ConflictException;
import com.paymentgateway.exception.ForbiddenException;
import com.paymentgateway.exception.ResourceNotFoundException;
import com.paymentgateway.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public PaymentService(
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService) {

        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    // MAKE PAYMENT
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {

        User user = currentUserService.getCurrentUser();

        Payment existingPayment = paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);

        if (existingPayment != null) {

            boolean sameUser = existingPayment.getUser()
                    .getId().equals(user.getId());

            boolean sameAmount = existingPayment.getAmount()
                    .compareTo(request.getAmount()) == 0;

            boolean samePaymentMethod = existingPayment
                    .getPaymentMethod()
                    .equals(request.getPaymentMethod());

            if (!sameUser || !sameAmount || !samePaymentMethod) {
                throw new ConflictException(
                        "Idempotency key already used for a different payment request");
            }

            return toPaymentResponse(existingPayment);
        }

        Payment payment = new Payment();

        payment.setTransactionId(generateTransactionId());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());

        if (request.getAmount().compareTo(
                new BigDecimal("100000")) > 0) {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(
                    "Transaction amount exceeds allowed limit");

        } else if (request.getAmount().compareTo(
                new BigDecimal("50000")) >= 0) {

            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(null);

        } else {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
        }

        Payment savedPayment = paymentRepository.save(payment);

        auditLogService.logAction(
                user.getId(),
                "PAYMENT_CREATED",
                savedPayment.getTransactionId(),
                "Payment created with status "
                        + savedPayment.getStatus().name()
        );

        return toPaymentResponse(savedPayment);
    }

    // GET PAYMENT BY TRANSACTION ID
    @Transactional(readOnly = true)
    public Payment getPaymentByTransactionId(String transactionId) {

        Payment payment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found"));

        User currentUser = currentUserService.getCurrentUser();

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only view your own payment");
        }

        return payment;
    }

    // GET PAYMENT STATUS
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String transactionId) {

        Payment payment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found"));

        User currentUser = currentUserService.getCurrentUser();

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only view your own payment status");
        }

        return new PaymentStatusResponse(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus().name());
    }

    // PROCESS PENDING PAYMENT
    @Transactional
    public PaymentResponse processPendingPayment(String transactionId) {

        Payment payment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found"));

        User currentUser = currentUserService.getCurrentUser();

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only process your own payment");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending payments can be processed");
        }

        PaymentStatus oldStatus = payment.getStatus();

        if (payment.getAmount().compareTo(
                new BigDecimal("75000")) <= 0) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);

        } else {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment processing failed");
        }

        Payment updatedPayment = paymentRepository.save(payment);

        auditLogService.logAction(
                currentUser.getId(),
                "PAYMENT_PROCESSED",
                updatedPayment.getTransactionId(),
                "Payment status changed from "
                        + oldStatus.name() + " to "
                        + updatedPayment.getStatus().name()
        );

        return toPaymentResponse(updatedPayment);
    }

    // RETRY FAILED PAYMENT
    @Transactional
    public RetryPaymentResponse retryPayment(RetryPaymentRequest request) {

        Payment oldPayment = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found"));

        User currentUser = currentUserService.getCurrentUser();

        if (!oldPayment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only retry your own payment");
        }

        if (oldPayment.getStatus() != PaymentStatus.FAILED) {
            throw new BadRequestException(
                    "Only failed payments can be retried");
        }

        Payment existingRetry = paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);

        if (existingRetry != null) {

            boolean sameUser = existingRetry.getUser()
                    .getId().equals(currentUser.getId());

            boolean sameAmount = existingRetry.getAmount()
                    .compareTo(oldPayment.getAmount()) == 0;

            boolean samePaymentMethod = existingRetry
                    .getPaymentMethod()
                    .equals(oldPayment.getPaymentMethod());

            if (!sameUser || !sameAmount || !samePaymentMethod) {
                throw new ConflictException(
                        "Idempotency key already used for a different retry request");
            }

            return new RetryPaymentResponse(
                    oldPayment.getTransactionId(),
                    existingRetry.getTransactionId(),
                    existingRetry.getId(),
                    existingRetry.getAmount(),
                    existingRetry.getPaymentMethod(),
                    existingRetry.getStatus().name(),
                    existingRetry.getFailureReason(),
                    existingRetry.getCreatedAt());
        }

        Payment newPayment = new Payment();

        newPayment.setTransactionId(generateTransactionId());
        newPayment.setIdempotencyKey(request.getIdempotencyKey());
        newPayment.setUser(oldPayment.getUser());
        newPayment.setAmount(oldPayment.getAmount());
        newPayment.setPaymentMethod(oldPayment.getPaymentMethod());
        newPayment.setStatus(PaymentStatus.PENDING);
        newPayment.setFailureReason(null);

        Payment savedPayment = paymentRepository.save(newPayment);

        auditLogService.logAction(
                currentUser.getId(),
                "PAYMENT_RETRY_CREATED",
                savedPayment.getTransactionId(),
                "Retry created for original transaction "
                        + oldPayment.getTransactionId()
        );

        return new RetryPaymentResponse(
                oldPayment.getTransactionId(),
                savedPayment.getTransactionId(),
                savedPayment.getId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod(),
                savedPayment.getStatus().name(),
                savedPayment.getFailureReason(),
                savedPayment.getCreatedAt());
    }

    // CONVERT PAYMENT TO RESPONSE
    private PaymentResponse toPaymentResponse(Payment payment) {

        return new PaymentResponse(
                payment.getTransactionId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus().name(),
                payment.getFailureReason(),
                payment.getCreatedAt());
    }

    // GENERATE TRANSACTION ID
    private String generateTransactionId() {

        return "TXN"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }
}