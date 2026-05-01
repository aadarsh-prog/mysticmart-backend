package com.mysticmart.invoice.model;

import com.mysticmart.order.model.Order;
import com.mysticmart.payment.model.Payment;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "invoices")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String invoiceNumber; // Format: INV-YYYYMM-NNNN

    // FIX #1: renamed from `purchaseOrder` → `order`
    // so that findByOrderId(), getOrder(), and JPQL all resolve correctly
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal discountAmount;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal cgst;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal sgst;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal igst;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal totalTax;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal totalAmount;

    @Column(length = 50)
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.ACTIVE;

    @CreatedDate @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum InvoiceStatus { ACTIVE, VOIDED }
}