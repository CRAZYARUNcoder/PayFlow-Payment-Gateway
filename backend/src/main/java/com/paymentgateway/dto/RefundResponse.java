package com.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private String refundId;
    private String transactionId;
    private Long paymentId;
    private BigDecimal refundAmount;
    private String status;
    private String reason;
    private LocalDateTime createdAt;

    public RefundResponse(
            String refundId,
            String transactionId,
            Long paymentId,
            BigDecimal refundAmount,
            String status,
            String reason,
            LocalDateTime createdAt) {

        this.refundId = refundId;
        this.transactionId = transactionId;
        this.paymentId = paymentId;
        this.refundAmount = refundAmount;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getRefundId() {
        return refundId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}