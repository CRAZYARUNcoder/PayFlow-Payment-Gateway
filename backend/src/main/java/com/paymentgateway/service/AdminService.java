
package com.paymentgateway.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.paymentgateway.dto.AdminDashboardResponse;
import com.paymentgateway.dto.AdminPaymentResponse;
import com.paymentgateway.dto.AdminUserResponse;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.User;
import com.paymentgateway.repository.PaymentRepository;
import com.paymentgateway.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public AdminService(
            UserRepository userRepository,
            PaymentRepository paymentRepository) {

        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    // GET ALL USERS WITH PAGINATION
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(user ->
                        new AdminUserResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getRole()
                        )
                );
    }

    // GET ALL PAYMENTS WITH PAGINATION
    public Page<AdminPaymentResponse> getAllPayments(
            Pageable pageable) {

        return paymentRepository.findAll(pageable)
                .map(payment -> {

                    User user = payment.getUser();

                    return new AdminPaymentResponse(
                            payment.getId(),
                            payment.getTransactionId(),
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            payment.getAmount(),
                            payment.getPaymentMethod(),
                            payment.getStatus().name(),
                            payment.getFailureReason(),
                            payment.getCreatedAt()
                    );
                });
    }

    // TOTAL USERS
    public long getTotalUsers() {
        return userRepository.count();
    }

    // TOTAL PAYMENTS
    public long getTotalPayments() {
        return paymentRepository.count();
    }

    // SUCCESSFUL PAYMENTS
    public long getSuccessfulPayments() {
        return paymentRepository.countByStatus(
                PaymentStatus.SUCCESS
        );
    }

    // FAILED PAYMENTS
    public long getFailedPayments() {
        return paymentRepository.countByStatus(
                PaymentStatus.FAILED
        );
    }

    // PENDING PAYMENTS
    public long getPendingPayments() {
        return paymentRepository.countByStatus(
                PaymentStatus.PENDING
        );
    }

    // REFUNDED PAYMENTS
    public long getRefundedPayments() {
        return paymentRepository.countByStatus(
                PaymentStatus.REFUNDED
        );
    }

    // TOTAL TRANSACTION AMOUNT
    public BigDecimal getTotalTransactionAmount() {
        return paymentRepository.sumAllPaymentAmount();
    }

    // TOTAL REFUNDED AMOUNT
    public BigDecimal getTotalRefundedAmount() {
        return paymentRepository.sumAmountByStatus(
                PaymentStatus.REFUNDED
        );
    }

    // ADMIN DASHBOARD
    public AdminDashboardResponse getDashboard() {

        return new AdminDashboardResponse(
                getTotalUsers(),
                getTotalPayments(),
                getSuccessfulPayments(),
                getFailedPayments(),
                getPendingPayments(),
                getRefundedPayments(),
                getTotalTransactionAmount(),
                getTotalRefundedAmount()
        );
    }
}