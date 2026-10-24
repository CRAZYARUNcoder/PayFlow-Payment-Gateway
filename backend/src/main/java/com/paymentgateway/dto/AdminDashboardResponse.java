package com.paymentgateway.dto;

import java.math.BigDecimal;

public class AdminDashboardResponse {

    private long totalUsers;
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long pendingPayments;
    private long refundedPayments;

    private BigDecimal totalTransactionAmount;
    private BigDecimal totalRefundedAmount;

    public AdminDashboardResponse(
            long totalUsers,
            long totalPayments,
            long successfulPayments,
            long failedPayments,
            long pendingPayments,
            long refundedPayments,
            BigDecimal totalTransactionAmount,
            BigDecimal totalRefundedAmount) {

        this.totalUsers = totalUsers;
        this.totalPayments = totalPayments;
        this.successfulPayments = successfulPayments;
        this.failedPayments = failedPayments;
        this.pendingPayments = pendingPayments;
        this.refundedPayments = refundedPayments;
        this.totalTransactionAmount = totalTransactionAmount;
        this.totalRefundedAmount = totalRefundedAmount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public long getRefundedPayments() {
        return refundedPayments;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public BigDecimal getTotalRefundedAmount() {
        return totalRefundedAmount;
    }
}