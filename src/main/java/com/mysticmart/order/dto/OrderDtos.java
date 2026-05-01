package com.mysticmart.order.dto;
import com.mysticmart.order.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    /** Single item to add to the cart */
    @Data
    public static class CartItemRequest {
        @NotNull private Long productId;
        @NotNull @Min(1) private Integer quantity;
    }

    /** Initial cart creation request */
    @Builder
    @Data
    public static class CreateOrderRequest {
        private String customerName;
        @Email private String customerEmail;
        private String customerPhone;
        @NotEmpty @Valid private List<CartItemRequest> items;
        @DecimalMin("0") @Builder.Default private BigDecimal discountAmount = BigDecimal.ZERO;
        @Builder.Default private boolean interState = false; // true = IGST, false = CGST+SGST
    }

    /** Add/update items in an existing DRAFT order */
    @Data
    public static class UpdateOrderRequest {
        private String customerName;
        @Email private String customerEmail;
        private String customerPhone;
        @NotEmpty @Valid private List<CartItemRequest> items;
        @DecimalMin("0") private BigDecimal discountAmount;
        private Boolean interState;
    }

    @Data @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal gstPercent;
        private BigDecimal lineSubtotal;
        private BigDecimal lineTax;
        private BigDecimal lineTotal;
    }

    @Data @Builder
    public static class OrderResponse {
        private Long id;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal cgst;
        private BigDecimal sgst;
        private BigDecimal igst;
        private BigDecimal totalTax;
        private BigDecimal totalPayable;
        private boolean interState;
        private Order.OrderStatus status;
        private List<OrderItemResponse> items;
        private String createdByEmail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder
    public static class OrderSummary {
        private Long id;
        private String customerName;
        private BigDecimal totalPayable;
        private Order.OrderStatus status;
        private LocalDateTime createdAt;
    }
}
