package com.mysticmart.order.model;
import com.mysticmart.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ORDER STATE MACHINE:
 *   DRAFT         — cart is being built, items can be added/removed
 *   CONFIRMED     — cart locked, payment has been initiated
 *   PAID          — payment SUCCESS, invoice generated, stock deducted (ATOMIC)
 *   FAILED        — payment failed, order can be retried
 *   CANCELLED     — manually cancelled before payment
 *
 * Stock is ONLY deducted after payment reaches SUCCESS status.
 * Invoice is ONLY generated after payment reaches SUCCESS status.
 */
@Entity @Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by") private User createdBy;

    // Customer info — optional for walk-in
    @Column(length = 100) private String customerName;
    @Column(length = 100) private String customerEmail;
    @Column(length = 20) private String customerPhone;

    // Financials
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal cgst = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal sgst = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal igst = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal totalTax = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal totalPayable = BigDecimal.ZERO;

    // Inter-state flag: true = IGST applied, false = CGST+SGST applied
    @Column(nullable = false) @Builder.Default private boolean interState = false;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private OrderStatus status = OrderStatus.DRAFT;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<OrderItem> items = new ArrayList<>();

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public enum OrderStatus { DRAFT, CONFIRMED, PAID, FAILED, CANCELLED }
}
