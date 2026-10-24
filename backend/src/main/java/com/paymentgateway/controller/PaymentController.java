
package com.paymentgateway.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.paymentgateway.dto.PaymentHistoryResponse;
import com.paymentgateway.dto.PaymentRequest;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentStatus;
import com.paymentgateway.entity.User;
import com.paymentgateway.repository.PaymentRepository;
import com.paymentgateway.repository.UserRepository;
import com.paymentgateway.service.CurrentUserService;
import com.paymentgateway.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public PaymentController(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {

        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    // GET PAYMENTS FOR CURRENT LOGGED-IN USER
    @GetMapping
    public ResponseEntity<List<PaymentHistoryResponse>> getMyPayments() {

        User currentUser = currentUserService.getCurrentUser();

        List<PaymentHistoryResponse> payments =
                paymentRepository.findByUser(
                        currentUser,
                        PageRequest.of(
                                0,
                                100,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                ).getContent()
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return ResponseEntity.ok(payments);
    }

    // CREATE PAYMENT
    @PostMapping
    public ResponseEntity<?> makePayment(
            @Valid @RequestBody PaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.makePayment(request)
        );
    }

    // PAYMENT HISTORY: PAGINATION + FILTERS
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PaymentHistoryResponse>> getUserPayments(
            @PathVariable Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        User user = validateAndGetUser(userId);

        paymentMethod = validatePaymentMethod(paymentMethod);

        LocalDateTime startDateTime = parseFromDate(fromDate);
        LocalDateTime endDateTime = parseToDate(toDate);

        validateDateRange(startDateTime, endDateTime);

        Page<Payment> payments = paymentRepository.searchPayments(
                user.getId(),
                status,
                paymentMethod,
                startDateTime,
                endDateTime,
                pageable
        );

        return ResponseEntity.ok(
                payments.map(this::toHistoryResponse)
        );
    }

    // EXPORT PAYMENT HISTORY TO CSV
    @GetMapping("/user/{userId}/export")
    public ResponseEntity<byte[]> exportPaymentsToCsv(
            @PathVariable Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        User user = validateAndGetUser(userId);

        paymentMethod = validatePaymentMethod(paymentMethod);

        LocalDateTime startDateTime = parseFromDate(fromDate);
        LocalDateTime endDateTime = parseToDate(toDate);

        validateDateRange(startDateTime, endDateTime);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(
                "ID,Transaction ID,Amount,Payment Method,Status,Created At\n"
        );

        int pageNumber = 0;
        Page<Payment> paymentPage;

        do {
            Pageable exportPageable = PageRequest.of(
                    pageNumber,
                    500,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            paymentPage = paymentRepository.searchPayments(
                    user.getId(),
                    status,
                    paymentMethod,
                    startDateTime,
                    endDateTime,
                    exportPageable
            );

            for (Payment payment : paymentPage.getContent()) {

                csv.append(payment.getId()).append(",");
                csv.append(csvEscape(payment.getTransactionId())).append(",");
                csv.append(payment.getAmount()).append(",");
                csv.append(csvEscape(payment.getPaymentMethod())).append(",");
                csv.append(csvEscape(payment.getStatus().name())).append(",");
                csv.append(csvEscape(
                        payment.getCreatedAt() == null
                                ? ""
                                : payment.getCreatedAt().toString()
                ));
                csv.append("\n");
            }

            pageNumber++;

        } while (paymentPage.hasNext());

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                new MediaType("text", "csv", StandardCharsets.UTF_8)
        );
        headers.setContentDispositionFormData(
                "attachment",
                "payment-history.csv"
        );
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(
                csvBytes,
                headers,
                HttpStatus.OK
        );
    }

    // CHECK USER OWNERSHIP
    private User validateAndGetUser(Long userId) {

        User currentUser = currentUserService.getCurrentUser();

        if (!currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access these payments"
            );
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    // VALIDATE PAYMENT METHOD
    private String validatePaymentMethod(String paymentMethod) {

        if (paymentMethod == null || paymentMethod.isBlank()) {
            return null;
        }

        paymentMethod = paymentMethod.trim().toUpperCase();

        if (!paymentMethod.matches(
                "UPI|CARD|NET_BANKING|WALLET")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment method. Use UPI, CARD, NET_BANKING or WALLET"
            );
        }

        return paymentMethod;
    }

    // PARSE START DATE
    private LocalDateTime parseFromDate(String fromDate) {

        if (fromDate == null || fromDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(fromDate).atStartOfDay();

        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid fromDate. Use yyyy-MM-dd"
            );
        }
    }

    // PARSE END DATE: INCLUDE THE WHOLE DAY
    private LocalDateTime parseToDate(String toDate) {

        if (toDate == null || toDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(toDate)
                    .plusDays(1)
                    .atStartOfDay()
                    .minusNanos(1);

        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid toDate. Use yyyy-MM-dd"
            );
        }
    }

    // VALIDATE DATE RANGE
    private void validateDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate must be before or equal to toDate"
            );
        }
    }

    // MAP PAYMENT ENTITY TO RESPONSE
    private PaymentHistoryResponse toHistoryResponse(Payment payment) {

        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }

    // ESCAPE CSV VALUES SAFELY
    private String csvEscape(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // GET PAYMENT BY TRANSACTION ID
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentHistoryResponse> getPaymentByTransactionId(
            @PathVariable String transactionId) {

        Payment payment =
                paymentService.getPaymentByTransactionId(transactionId);

        return ResponseEntity.ok(toHistoryResponse(payment));
    }

    // GET PAYMENT STATUS
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable String transactionId) {

        return ResponseEntity.ok(
                paymentService.getPaymentStatus(transactionId)
        );
    }

    // PROCESS PENDING PAYMENT
    @PutMapping("/process/{transactionId}")
    public ResponseEntity<?> processPendingPayment(
            @PathVariable String transactionId) {

        return ResponseEntity.ok(
                paymentService.processPendingPayment(transactionId)
        );
    }
}