package com.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RetryPaymentResponse {

    private String oldTransactionId;
    private String newTransactionId;
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;

    public RetryPaymentResponse(
            String oldTransactionId,
            String newTransactionId,
            Long paymentId,
            BigDecimal amount,
            String paymentMethod,
            String status,
            String failureReason,
            LocalDateTime createdAt) {

        this.oldTransactionId = oldTransactionId;
        this.newTransactionId = newTransactionId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public String getOldTransactionId() {
        return oldTransactionId;
    }

    public String getNewTransactionId() {
        return newTransactionId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
