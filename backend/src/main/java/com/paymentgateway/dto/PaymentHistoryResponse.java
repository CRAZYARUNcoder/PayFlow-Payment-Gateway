package com.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentHistoryResponse {

    private Long id;
    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;

    public PaymentHistoryResponse(
            Long id,
            String transactionId,
            BigDecimal amount,
            String paymentMethod,
            String status,
            LocalDateTime createdAt) {

        this.id = id;
        this.transactionId = transactionId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
