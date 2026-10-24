
package com.paymentgateway.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentgateway.entity.AuditLog;
import com.paymentgateway.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(
            Long userId,
            String action,
            String transactionId,
            String details) {

        AuditLog auditLog = new AuditLog(
                userId,
                action,
                transactionId,
                details
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTransaction(String transactionId) {
        return auditLogRepository
                .findByTransactionIdOrderByCreatedAtDesc(transactionId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(action);
    }
}