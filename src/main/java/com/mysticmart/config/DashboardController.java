package com.mysticmart.config;
import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.inventory.service.ProductService;
import com.mysticmart.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {
    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Stats>> stats() {
        return ResponseEntity.ok(ApiResponse.success(Stats.builder()
                .totalRevenue(orderService.totalRevenue())
                .revenueToday(orderService.revenueToday())
                .totalOrdersPaid(orderService.totalOrdersPaid())
                .lowStockCount((long) productService.getLowStock().size())
                .build()));
    }

    @Data @Builder
    public static class Stats {
        private BigDecimal totalRevenue;
        private BigDecimal revenueToday;
        private long totalOrdersPaid;
        private long lowStockCount;
    }
}
