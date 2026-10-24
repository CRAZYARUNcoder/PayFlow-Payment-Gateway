package com.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminPaymentResponse {

    private Long id;
    private String transactionId;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;

    public AdminPaymentResponse(
            Long id,
            String transactionId,
            Long userId,
            String userName,
            String userEmail,
            BigDecimal amount,
            String paymentMethod,
            String status,
            String failureReason,
            LocalDateTime createdAt) {

        this.id = id;
        this.transactionId = transactionId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
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

