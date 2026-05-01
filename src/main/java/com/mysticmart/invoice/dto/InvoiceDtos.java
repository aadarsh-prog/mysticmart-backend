package com.mysticmart.invoice.dto;
import com.mysticmart.invoice.model.Invoice;
import com.mysticmart.order.dto.OrderDtos;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDtos {
    @Data @Builder
    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private Long orderId;
        private Long paymentId;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private List<OrderDtos.OrderItemResponse> items;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal cgst;
        private BigDecimal sgst;
        private BigDecimal igst;
        private BigDecimal totalTax;
        private BigDecimal totalAmount;
        private String paymentMode;
        private Invoice.InvoiceStatus status;
        private LocalDateTime createdAt;
    }
}
