package com.mysticmart.payment.dto;
import com.mysticmart.payment.model.Payment;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentDtos {

    /** Initiate a payment for an order */
    @Data
    public static class InitiateRequest {
        @NotNull private Long orderId;
        @NotNull private Payment.PaymentMode mode;

        // Required for CASH mode: how much the customer handed over
        @DecimalMin("0") private BigDecimal cashReceived;

        // For SPLIT mode: list of split components
        private List<SplitComponent> splits;

        // Idempotency key — client generates UUID, prevents duplicate payments on retry
        @Size(max=100) private String idempotencyKey;
    }

    @Data
    public static class SplitComponent {
        @NotNull private Payment.PaymentMode mode;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        @DecimalMin("0") private BigDecimal cashReceived; // only for CASH component
    }

    /** Verify digital payment (Razorpay callback) */
    @Data
    public static class VerifyRequest {
        @NotNull private Long paymentId;
        @NotBlank private String gatewayOrderId;
        @NotBlank private String gatewayPaymentId;
        @NotBlank private String gatewaySignature;
    }

    /** Report payment failure */
    @Data
    public static class FailureRequest {
        @NotNull private Long paymentId;
        private String reason;
    }

    @Data @Builder
    public static class PaymentResponse {
        private Long id;
        private Long orderId;
        private BigDecimal amount;
        private Payment.PaymentMode mode;
        private Payment.PaymentStatus status;
        private String idempotencyKey;
        private String gatewayOrderId;
        private String gatewayPaymentId;
        private BigDecimal cashReceived;
        private BigDecimal changeReturned;
        private String failureReason;
        private Integer retryCount;
        private List<SplitResponse> splits;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class SplitResponse {
        private Payment.PaymentMode mode;
        private BigDecimal amount;
        private BigDecimal cashReceived;
        private BigDecimal changeReturned;
        private String gatewayPaymentId;
    }

    /** Returned when Razorpay order is created */
    @Data @Builder
    public static class GatewayOrderResponse {
        private Long paymentId;
        private String gatewayOrderId;
        private BigDecimal amount;
        private String currency;
        private String keyId;
    }
}
