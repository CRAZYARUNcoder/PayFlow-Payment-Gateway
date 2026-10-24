
package com.paymentgateway.dto;

import java.math.BigDecimal;

public class PaymentSummaryResponse {

    private long totalPayments;
    private long successfulPayments;
    private long pendingPayments;
    private long failedPayments;
    private long refundedPayments;
    private BigDecimal totalAmount;

    public PaymentSummaryResponse() {
    }

    public PaymentSummaryResponse(
            long totalPayments,
            long successfulPayments,
            long pendingPayments,
            long failedPayments,
            long refundedPayments,
            BigDecimal totalAmount) {

        this.totalPayments = totalPayments;
        this.successfulPayments = successfulPayments;
        this.pendingPayments = pendingPayments;
        this.failedPayments = failedPayments;
        this.refundedPayments = refundedPayments;
        this.totalAmount = totalAmount;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(long successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(long failedPayments) {
        this.failedPayments = failedPayments;
    }

    public long getRefundedPayments() {
        return refundedPayments;
    }

    public void setRefundedPayments(long refundedPayments) {
        this.refundedPayments = refundedPayments;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
