
package com.paymentgateway.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.paymentgateway.dto.PaymentSummaryResponse;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.repository.PaymentRepository;

@Service
public class PaymentSummaryService {

    private final PaymentRepository paymentRepository;

    public PaymentSummaryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentSummaryResponse getPaymentSummary(Long userId) {

        long totalPayments =
                paymentRepository.countByUser_Id(userId);

        long successfulPayments =
                paymentRepository.countByUser_IdAndStatus(
                        userId, PaymentStatus.SUCCESS);

        long pendingPayments =
                paymentRepository.countByUser_IdAndStatus(
                        userId, PaymentStatus.PENDING);

        long failedPayments =
                paymentRepository.countByUser_IdAndStatus(
                        userId, PaymentStatus.FAILED);

        long refundedPayments =
                paymentRepository.countByUser_IdAndStatus(
                        userId, PaymentStatus.REFUNDED);

        BigDecimal totalAmount =
                paymentRepository.sumAmountByUserId(userId);

        return new PaymentSummaryResponse(
                totalPayments,
                successfulPayments,
                pendingPayments,
                failedPayments,
                refundedPayments,
                totalAmount
        );
    }
}