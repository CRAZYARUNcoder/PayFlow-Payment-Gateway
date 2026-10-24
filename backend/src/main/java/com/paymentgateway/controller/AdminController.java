
package com.paymentgateway.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.paymentgateway.dto.AdminDashboardResponse;
import com.paymentgateway.dto.AdminPaymentResponse;
import com.paymentgateway.dto.AdminUserResponse;
import com.paymentgateway.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // USERS WITH PAGINATION
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                adminService.getAllUsers(pageable)
        );
    }

    // PAYMENTS WITH PAGINATION
    @GetMapping("/payments")
    public ResponseEntity<Page<AdminPaymentResponse>> getAllPayments(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                adminService.getAllPayments(pageable)
        );
    }

    @GetMapping("/stats/users")
    public ResponseEntity<Long> getTotalUsers() {
        return ResponseEntity.ok(adminService.getTotalUsers());
    }

    @GetMapping("/stats/payments")
    public ResponseEntity<Long> getTotalPayments() {
        return ResponseEntity.ok(adminService.getTotalPayments());
    }

    @GetMapping("/stats/success")
    public ResponseEntity<Long> getSuccessfulPayments() {
        return ResponseEntity.ok(
                adminService.getSuccessfulPayments()
        );
    }

    @GetMapping("/stats/failed")
    public ResponseEntity<Long> getFailedPayments() {
        return ResponseEntity.ok(
                adminService.getFailedPayments()
        );
    }

    @GetMapping("/stats/pending")
    public ResponseEntity<Long> getPendingPayments() {
        return ResponseEntity.ok(
                adminService.getPendingPayments()
        );
    }

    @GetMapping("/stats/refunded")
    public ResponseEntity<Long> getRefundedPayments() {
        return ResponseEntity.ok(
                adminService.getRefundedPayments()
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }
}