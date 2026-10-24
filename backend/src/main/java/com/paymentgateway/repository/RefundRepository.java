package com.paymentgateway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentgateway.entity.Refund;

public interface RefundRepository
        extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    Optional<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByPaymentUserId(Long userId);
}