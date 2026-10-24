package com.paymentgateway.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.paymentgateway.entity.AuditLog;
import com.paymentgateway.entity.User;
import com.paymentgateway.repository.AuditLogRepository;
import com.paymentgateway.repository.UserRepository;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogController(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {

        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // Check whether the logged-in user is an admin
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role ->
                        role.equals("ROLE_ADMIN")
                        || role.equals("ADMIN"));
    }

    // Get audit logs for a specific user
    @GetMapping("/user/{userId}")
    public List<AuditLog> getUserLogs(
            @PathVariable Long userId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first");
        }

        User loggedInUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Logged-in user not found"));

        boolean admin = isAdmin(authentication);

        // Temporary debugging logs
        System.out.println("Logged-in email: "
                + authentication.getName());
        System.out.println("Logged-in user ID: "
                + loggedInUser.getId());
        System.out.println("Requested user ID: " + userId);
        System.out.println("Is admin: " + admin);

        if (!admin && !loggedInUser.getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view your own audit logs");
        }

        return auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get all audit logs (admin only)
    @GetMapping
    public List<AuditLog> getAllAuditLogs(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first");
        }

        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admin can view all audit logs");
        }

        return auditLogRepository.findAll();
    }

    // Get audit logs for a transaction
    @GetMapping("/transaction/{transactionId}")
    public List<AuditLog> getTransactionLogs(
            @PathVariable String transactionId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first");
        }

        List<AuditLog> logs = auditLogRepository
                .findByTransactionIdOrderByCreatedAtDesc(
                        transactionId);

        if (isAdmin(authentication)) {
            return logs;
        }

        User loggedInUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Logged-in user not found"));

        boolean ownsTransaction = logs.stream()
                .anyMatch(log ->
                        loggedInUser.getId().equals(log.getUserId()));

        if (!ownsTransaction) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view audit logs for your own transactions");
        }

        return logs;
    }
}

