package com.paymentgateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.dto.PaymentResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.User;
import com.paymentgateway.exception.ConflictException;
import com.paymentgateway.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PaymentService paymentService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(3L);

        when(currentUserService.getCurrentUser())
                .thenReturn(user);
    }

    @Test
    void makePaymentBelow50000ShouldSucceed() {
        PaymentRequest request = createRequest(
                new BigDecimal("1000"), "key-success");

        when(paymentRepository.findByIdempotencyKey("key-success"))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.makePayment(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        verify(paymentRepository).save(any(Payment.class));
        verify(auditLogService).logAction(
                eq(3L),
                eq("PAYMENT_CREATED"),
                any(String.class),
                eq("Payment created with status SUCCESS"));
    }

    @Test
    void makePaymentOf50000ShouldBePending() {
        PaymentRequest request = createRequest(
                new BigDecimal("50000"), "key-pending");

        when(paymentRepository.findByIdempotencyKey("key-pending"))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.makePayment(request);

        assertEquals("PENDING", response.getStatus());

        verify(auditLogService).logAction(
                eq(3L),
                eq("PAYMENT_CREATED"),
                any(String.class),
                eq("Payment created with status PENDING"));
    }

    @Test
    void makePaymentAbove100000ShouldFail() {
        PaymentRequest request = createRequest(
                new BigDecimal("100001"), "key-failed");

        when(paymentRepository.findByIdempotencyKey("key-failed"))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.makePayment(request);

        assertEquals("FAILED", response.getStatus());

        verify(auditLogService).logAction(
                eq(3L),
                eq("PAYMENT_CREATED"),
                any(String.class),
                eq("Payment created with status FAILED"));
    }

    @Test
    void repeatedIdempotencyKeyForSamePaymentShouldNotSaveAgain() {
        PaymentRequest request = createRequest(
                new BigDecimal("1000"), "same-key");

        Payment existingPayment = new Payment();
       
        existingPayment.setTransactionId("TXN123456789");
        existingPayment.setIdempotencyKey("same-key");
        existingPayment.setUser(user);
        existingPayment.setAmount(new BigDecimal("1000"));
        existingPayment.setPaymentMethod(
                request.getPaymentMethod());
        existingPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByIdempotencyKey("same-key"))
                .thenReturn(Optional.of(existingPayment));

        PaymentResponse response =
                paymentService.makePayment(request);

        assertEquals("TXN123456789", response.getTransactionId());
        assertEquals("SUCCESS", response.getStatus());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(auditLogService, never()).logAction(
                eq(3L), any(String.class),
                any(String.class), any(String.class));
    }

    @Test
    void reusedIdempotencyKeyForDifferentAmountShouldThrowConflict() {
        PaymentRequest request = createRequest(
                new BigDecimal("2000"), "duplicate-key");

        Payment existingPayment = new Payment();
        existingPayment.setTransactionId("TXN123456789");
        existingPayment.setIdempotencyKey("duplicate-key");
        existingPayment.setUser(user);
        existingPayment.setAmount(new BigDecimal("1000"));
        existingPayment.setPaymentMethod(
                request.getPaymentMethod());
        existingPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByIdempotencyKey("duplicate-key"))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                ConflictException.class,
                () -> paymentService.makePayment(request));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private PaymentRequest createRequest(
            BigDecimal amount, String idempotencyKey) {

        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);
        request.setIdempotencyKey(idempotencyKey);

        // Adjust this value if your PaymentRequest uses an enum.
        request.setPaymentMethod("UPI");

        return request;
    }
}

