
package com.paymentgateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentgateway.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find all audit logs for a particular user, newest first
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find audit logs for a specific transaction, newest first
    List<AuditLog> findByTransactionIdOrderByCreatedAtDesc(
            String transactionId
    );

    // Find all logs of a particular action, newest first
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}