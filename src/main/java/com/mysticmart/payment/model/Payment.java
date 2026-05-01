package com.mysticmart.payment.model;
import com.mysticmart.order.model.Order;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PAYMENT STATE MACHINE:
 *   INITIATED   — payment record created, waiting for processing
 *   PROCESSING  — sent to payment gateway, waiting for callback
 *   SUCCESS     — payment confirmed (triggers: stock deduction, invoice generation, order → PAID)
 *   FAILED      — payment failed (order → FAILED, can retry)
 *   REFUNDED    — payment reversed after success
 *
 * For SPLIT payments: one Payment record, multiple PaymentSplit records.
 */
@Entity @Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) private Order order;

    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentMode mode;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private PaymentStatus status = PaymentStatus.INITIATED;

    // For digital payments: idempotency key to prevent duplicate processing
    @Column(unique = true, length = 100)
    private String idempotencyKey;

    // Gateway fields (null for cash payments)
    @Column(length = 100) private String gatewayOrderId;
    @Column(length = 100) private String gatewayPaymentId;
    @Column(length = 200) private String gatewaySignature;

    // Cash payment fields
    @Column(precision = 10, scale = 2) private BigDecimal cashReceived;   // how much customer gave
    @Column(precision = 10, scale = 2) private BigDecimal changeReturned; // how much we give back

    // Failure tracking
    @Column(length = 500) private String failureReason;
    @Builder.Default private Integer retryCount = 0;

    private LocalDateTime paidAt;

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;

    // Split payment components
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<PaymentSplit> splits = new ArrayList<>();

    public enum PaymentMode { CASH, CARD, UPI, WALLET, SPLIT }
    public enum PaymentStatus { INITIATED, PROCESSING, SUCCESS, FAILED, REFUNDED }
}
