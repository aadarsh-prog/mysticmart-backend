package com.mysticmart.order.model;
import com.mysticmart.inventory.model.Product;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false) private Product product;

    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;    // price snapshot
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal gstPercent;  // GST snapshot
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal lineSubtotal; // unitPrice * qty
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal lineTax;      // tax on this line
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal lineTotal;    // subtotal + tax
}
