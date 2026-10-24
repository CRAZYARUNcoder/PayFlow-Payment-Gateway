
package com.paymentgateway.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.User;
import com.paymentgateway.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find all payments of a user
    List<Payment> findByUser(User user);

    // Payment history with pagination
    Page<Payment> findByUser(User user, Pageable pageable);

    // Filter by status
    Page<Payment> findByUserAndStatus(
            User user,
            PaymentStatus status,
            Pageable pageable
    );

    // Filter by payment method
    Page<Payment> findByUserAndPaymentMethod(
            User user,
            String paymentMethod,
            Pageable pageable
    );

    // Filter by status and payment method
    Page<Payment> findByUserAndStatusAndPaymentMethod(
            User user,
            PaymentStatus status,
            String paymentMethod,
            Pageable pageable
    );

    // Find by idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // Count payments by status (all users)
    long countByStatus(PaymentStatus status);

    // Total amount of all payments (all users)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal sumAllPaymentAmount();

    // Total amount by status (all users)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(
            @Param("status") PaymentStatus status
    );

    // ==============================
    // PAYMENT SUMMARY API METHODS
    // ==============================

    // Count all payments belonging to a specific user
    long countByUser_Id(Long userId);

    // Count payments by status for a specific user
    long countByUser_IdAndStatus(
            Long userId,
            PaymentStatus status
    );

    // Calculate total amount for a specific user's payments
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.user.id = :userId")
    BigDecimal sumAmountByUserId(
            @Param("userId") Long userId
    );

    // ==============================
    // PAYMENT HISTORY SEARCH
    // ==============================

    @Query(
        value = "SELECT p FROM Payment p " +
                "WHERE p.user.id = :userId " +
                "AND (:status IS NULL OR p.status = :status) " +
                "AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) " +
                "AND (:fromDate IS NULL OR p.createdAt >= :fromDate) " +
                "AND (:toDate IS NULL OR p.createdAt <= :toDate)",

        countQuery = "SELECT COUNT(p) FROM Payment p " +
                "WHERE p.user.id = :userId " +
                "AND (:status IS NULL OR p.status = :status) " +
                "AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) " +
                "AND (:fromDate IS NULL OR p.createdAt >= :fromDate) " +
                "AND (:toDate IS NULL OR p.createdAt <= :toDate)"
    )
    Page<Payment> searchPayments(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}