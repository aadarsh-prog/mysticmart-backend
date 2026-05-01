package com.mysticmart.payment.controller;
import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.payment.dto.PaymentDtos;
import com.mysticmart.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
@Tag(name = "Payments", description = "Full payment lifecycle: initiate, verify, fail, retry, refund")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment. CASH = instant SUCCESS. DIGITAL = returns gateway details.")
    public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> initiate(
            @Valid @RequestBody PaymentDtos.InitiateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.initiate(req)));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay HMAC signature. ATOMIC: confirms order, deducts stock, generates invoice.")
    public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> verify(
            @Valid @RequestBody PaymentDtos.VerifyRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", paymentService.verifyDigitalPayment(req)));
    }

    @PostMapping("/failure")
    @Operation(summary = "Report payment failure. Order moves to FAILED state. Retry is allowed.")
    public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> failure(
            @Valid @RequestBody PaymentDtos.FailureRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment marked failed", paymentService.reportFailure(req)));
    }

    @PostMapping("/{orderId}/retry")
    @Operation(summary = "Reset a FAILED order to DRAFT so payment can be retried")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long orderId) {
        paymentService.retryAllowed(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order reset to DRAFT — payment can be retried", null));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment record for an order")
    public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByOrderId(orderId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List all payment transactions")
    public ResponseEntity<ApiResponse<Page<PaymentDtos.PaymentResponse>>> getAll(
            @PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAll(pageable)));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a successful payment")
    public ResponseEntity<ApiResponse<PaymentDtos.PaymentResponse>> refund(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.refund(id)));
    }
}
