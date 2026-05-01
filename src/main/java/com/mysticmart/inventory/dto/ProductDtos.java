package com.mysticmart.inventory.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDtos {
    @Data public static class CreateRequest {
        @NotBlank @Size(max=150) private String name;
        @NotBlank @Size(max=50) private String sku;
        @Size(max=500) private String description;
        @NotNull @DecimalMin("0.01") private BigDecimal price;
        @NotNull @Min(0) private Integer stockQty;
        @NotNull @Min(0) private Integer reorderLevel;
        @Size(max=100) private String category;
        @Size(max=50) private String unit;
        @DecimalMin("0") @DecimalMax("28") private BigDecimal gstPercent = new BigDecimal("18.00");
    }
    @Data public static class UpdateRequest {
        @Size(max=150) private String name;
        @Size(max=500) private String description;
        @DecimalMin("0.01") private BigDecimal price;
        @Min(0) private Integer reorderLevel;
        @Size(max=100) private String category;
        @Size(max=50) private String unit;
        @DecimalMin("0") @DecimalMax("28") private BigDecimal gstPercent;
    }
    @Data public static class StockAdjustRequest {
        @NotNull private Integer quantity;
        private String reason;
    }
    @Data @Builder public static class ProductResponse {
        private Long id;
        private String name;
        private String sku;
        private String description;
        private BigDecimal price;
        private BigDecimal gstPercent;
        private Integer stockQty;
        private Integer reorderLevel;
        private String category;
        private String unit;
        private boolean active;
        private boolean lowStock;
        private String barcodeUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
