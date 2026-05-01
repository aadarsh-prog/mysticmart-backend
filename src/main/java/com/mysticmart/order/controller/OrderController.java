package com.mysticmart.order.controller;
import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.order.dto.OrderDtos;
import com.mysticmart.order.model.Order;
import com.mysticmart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
@Tag(name = "Orders", description = "Cart creation and order management")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create DRAFT order (cart). Stock verified but NOT deducted yet.")
    public ResponseEntity<ApiResponse<OrderDtos.OrderResponse>> create(
            @Valid @RequestBody OrderDtos.CreateOrderRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created (DRAFT)", orderService.createDraft(req, ud.getUsername())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update DRAFT order — replace items, change discount, etc.")
    public ResponseEntity<ApiResponse<OrderDtos.OrderResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderDtos.UpdateOrderRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateDraft(id, req, ud.getUsername())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full order detail with tax breakdown")
    public ResponseEntity<ApiResponse<OrderDtos.OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List orders with filters")
    public ResponseEntity<ApiResponse<Page<OrderDtos.OrderSummary>>> getAll(
            @RequestParam(required=false) Order.OrderStatus status,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required=false) String search,
            @PageableDefault(size=20, sort="createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAll(status, from, to, search, pageable)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a DRAFT or FAILED order")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", null));
    }
}
