package com.paymentgateway.dto;

import java.math.BigDecimal;

public class PaymentStatusResponse {

    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;

    public PaymentStatusResponse(
            String transactionId,
            BigDecimal amount,
            String paymentMethod,
            String status) {

        this.transactionId = transactionId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
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
}