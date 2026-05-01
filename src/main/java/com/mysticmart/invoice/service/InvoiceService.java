package com.mysticmart.invoice.service;

import com.mysticmart.common.exception.AppException;
import com.mysticmart.invoice.dto.InvoiceDtos;
import com.mysticmart.invoice.model.Invoice;
import com.mysticmart.invoice.repository.InvoiceRepository;
import com.mysticmart.order.dto.OrderDtos;
import com.mysticmart.order.model.Order;
import com.mysticmart.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service @RequiredArgsConstructor @Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    @Transactional
    public Invoice generateInvoice(Order order, Payment payment) {
        return invoiceRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Invoice invoice = Invoice.builder()
                    .invoiceNumber(generateInvoiceNumber())
                    .order(order).payment(payment)
                    .subtotal(order.getSubtotal())
                    .discountAmount(order.getDiscountAmount())
                    .cgst(order.getCgst()).sgst(order.getSgst())
                    .igst(order.getIgst()).totalTax(order.getTotalTax())
                    .totalAmount(order.getTotalPayable())
                    .paymentMode(payment.getMode().name())
                    .status(Invoice.InvoiceStatus.ACTIVE).build();
            Invoice saved = invoiceRepository.save(invoice);
            log.info("Invoice generated: {} for order={}", saved.getInvoiceNumber(), order.getId());
            emailService.sendInvoiceEmail(saved, order);
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.InvoiceResponse getById(Long id) {
        return toResponse(invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException("Invoice not found", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.InvoiceResponse getByOrderId(Long orderId) {
        return toResponse(invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException("No invoice for order", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceDtos.InvoiceResponse> getAll(String search, Pageable pageable) {
        return invoiceRepository
                .findActiveWithSearch(Invoice.InvoiceStatus.ACTIVE, search, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public InvoiceDtos.InvoiceResponse voidInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException("Invoice not found", HttpStatus.NOT_FOUND));
        invoice.setStatus(Invoice.InvoiceStatus.VOIDED);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Async
    public CompletableFuture<Void> resendEmail(Long id) {
        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new AppException("Invoice not found", HttpStatus.NOT_FOUND));
            emailService.sendInvoiceEmail(invoice, invoice.getOrder());
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("resendEmail failed for invoice id={}: {}", id, ex.getMessage());
            return CompletableFuture.failedFuture(ex);
        }
    }

    private String generateInvoiceNumber() {
        String prefix = "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long next = invoiceRepository.maxId() + 1;
        return prefix + String.format("%04d", next);
    }

    private InvoiceDtos.InvoiceResponse toResponse(Invoice inv) {
        Order order = inv.getOrder();
        List<OrderDtos.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderDtos.OrderItemResponse.builder()
                        .id(i.getId()).productId(i.getProduct().getId())
                        .productName(i.getProduct().getName()).productSku(i.getProduct().getSku())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                        .gstPercent(i.getGstPercent()).lineSubtotal(i.getLineSubtotal())
                        .lineTax(i.getLineTax()).lineTotal(i.getLineTotal()).build())
                .toList();
        return InvoiceDtos.InvoiceResponse.builder()
                .id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
                .orderId(order.getId()).paymentId(inv.getPayment().getId())
                .customerName(order.getCustomerName()).customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone()).items(items)
                .subtotal(inv.getSubtotal()).discountAmount(inv.getDiscountAmount())
                .cgst(inv.getCgst()).sgst(inv.getSgst()).igst(inv.getIgst())
                .totalTax(inv.getTotalTax()).totalAmount(inv.getTotalAmount())
                .paymentMode(inv.getPaymentMode()).status(inv.getStatus())
                .createdAt(inv.getCreatedAt()).build();
    }
}