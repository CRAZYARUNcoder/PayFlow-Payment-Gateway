package com.paymentgateway.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.paymentgateway.dto.RefundHistoryResponse;
import com.paymentgateway.dto.RefundRequest;
import com.paymentgateway.dto.RefundResponse;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.Refund;
import com.paymentgateway.entity.User;
import com.paymentgateway.exception.ConflictException;
import com.paymentgateway.exception.ForbiddenException;
import com.paymentgateway.exception.BadRequestException;
import com.paymentgateway.exception.ResourceNotFoundException;
import com.paymentgateway.repository.PaymentRepository;
import com.paymentgateway.repository.RefundRepository;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService) {

        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    // =========================
    // PROCESS REFUND
    // =========================
    public RefundResponse processRefund(
            RefundRequest request) {

        User currentUser =
                currentUserService.getCurrentUser();

        Payment payment =
                paymentRepository.findById(
                        request.getPaymentId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found"
                        )
                );

        // User can refund only his own payment
        if (!payment.getUser()
                .getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You can only refund your own payment"
            );
        }

        // Only successful payments can be refunded
        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new BadRequestException(
                    "Only successful payments can be refunded"
            );
        }

        // Prevent duplicate refund
        if (refundRepository
                .findByPaymentId(payment.getId())
                .isPresent()) {

            throw new ConflictException(
                    "Payment is already refunded"
            );
        }

        Refund refund =
                new Refund();

        refund.setRefundId(
                generateRefundId()
        );

        refund.setPayment(payment);

        refund.setRefundAmount(
                payment.getAmount()
        );

        refund.setStatus(
                "REFUNDED"
        );

        refund.setReason(
                request.getReason()
        );

        refund.setCreatedAt(
                LocalDateTime.now()
        );

        Refund savedRefund =
                refundRepository.save(refund);

        // Update payment status
        payment.setStatus(
                PaymentStatus.REFUNDED
        );

        paymentRepository.save(payment);

        return new RefundResponse(
                savedRefund.getRefundId(),
                payment.getTransactionId(),
                payment.getId(),
                savedRefund.getRefundAmount(),
                savedRefund.getStatus(),
                savedRefund.getReason(),
                savedRefund.getCreatedAt()
        );
    }

    // =========================
    // GET CURRENT USER REFUNDS
    // =========================
    public List<RefundHistoryResponse> getAllRefunds() {

        User currentUser =
                currentUserService.getCurrentUser();

        // Fetch only current user's refunds
        List<Refund> refunds =
                refundRepository.findByPaymentUserId(
                        currentUser.getId()
                );

        return refunds.stream()
                .map(refund -> {

                    Payment payment =
                            refund.getPayment();

                    return new RefundHistoryResponse(
                            refund.getRefundId(),
                            payment.getTransactionId(),
                            payment.getId(),
                            refund.getRefundAmount(),
                            refund.getStatus(),
                            refund.getReason(),
                            refund.getCreatedAt()
                    );
                })
                .toList();
    }

    // =========================
    // GENERATE REFUND ID
    // =========================
    private String generateRefundId() {

        return "REF"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }
}

