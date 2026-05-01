package com.mysticmart.inventory.model;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(unique = true, nullable = false, length = 50) private String sku;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(nullable = false) @Builder.Default private Integer stockQty = 0;
    @Column(nullable = false) @Builder.Default private Integer reorderLevel = 10;
    @Column(length = 100) private String category;
    @Column(length = 50) private String unit;
    // GST slabs: 0, 5, 12, 18, 28
    @Column(nullable = false, precision = 5, scale = 2) @Builder.Default private BigDecimal gstPercent = new BigDecimal("18.00");
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
    public boolean isLowStock() { return stockQty <= reorderLevel; }
}
