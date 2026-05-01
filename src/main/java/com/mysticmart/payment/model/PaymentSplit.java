package com.mysticmart.payment.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Used when PaymentMode = SPLIT.
 * A customer can pay part in cash, part in UPI, etc.
 * The sum of all splits must equal the order total.
 */
@Entity @Table(name = "payment_splits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSplit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false) private Payment payment;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Payment.PaymentMode mode;

    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;

    // For cash split component
    @Column(precision = 10, scale = 2) private BigDecimal cashReceived;
    @Column(precision = 10, scale = 2) private BigDecimal changeReturned;

    // For digital split component
    @Column(length = 100) private String gatewayPaymentId;
}
