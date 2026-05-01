package com.mysticmart.invoice.controller;

import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.invoice.dto.InvoiceDtos;
import com.mysticmart.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/invoices") @RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoices are generated automatically after payment SUCCESS")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceDtos.InvoiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get invoice for a specific order")
    public ResponseEntity<ApiResponse<InvoiceDtos.InvoiceResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByOrderId(orderId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List all active invoices with optional search")
    public ResponseEntity<ApiResponse<Page<InvoiceDtos.InvoiceResponse>>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAll(search, pageable)));
    }

    @PostMapping("/{id}/email")
    @Operation(summary = "Resend invoice email to customer")
    public ResponseEntity<ApiResponse<Void>> resendEmail(@PathVariable Long id) {
        // FIX #5: service now returns CompletableFuture — we fire-and-forget here
        // (the future is not awaited; exceptions are logged inside the service)
        invoiceService.resendEmail(id);
        return ResponseEntity.ok(ApiResponse.success("Email queued", null));
    }

    @PatchMapping("/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Void an invoice (Admin only)")
    public ResponseEntity<ApiResponse<InvoiceDtos.InvoiceResponse>> voidInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.voidInvoice(id)));
    }
}