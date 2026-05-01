package com.mysticmart.order.service;

import com.mysticmart.auth.model.User;
import com.mysticmart.auth.repository.UserRepository;
import com.mysticmart.common.exception.AppException;
import com.mysticmart.inventory.model.Product;
import com.mysticmart.inventory.service.ProductService;
import com.mysticmart.order.dto.OrderDtos;
import com.mysticmart.order.model.Order;
import com.mysticmart.order.model.OrderItem;
import com.mysticmart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    /**
     * Create a DRAFT order (cart).
     */
    @Transactional
    public OrderDtos.OrderResponse createDraft(OrderDtos.CreateOrderRequest req, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        Order order = buildOrder(req.getItems(), req.getDiscountAmount(),
                req.isInterState(), req.getCustomerName(), req.getCustomerEmail(), req.getCustomerPhone(), creator);
        order.setStatus(Order.OrderStatus.DRAFT);
        return toResponse(orderRepository.save(order));
    }

    /**
     * Update a DRAFT order — replaces all items.
     */
    @Transactional
    public OrderDtos.OrderResponse updateDraft(Long orderId, OrderDtos.UpdateOrderRequest req, String creatorEmail) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() != Order.OrderStatus.DRAFT)
            throw new AppException("Only DRAFT orders can be modified", HttpStatus.BAD_REQUEST);

        order.getItems().clear();
        BigDecimal discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : order.getDiscountAmount();
        boolean interState = req.getInterState() != null ? req.getInterState() : order.isInterState();

        List<OrderItem> newItems = buildItems(req.getItems(), order);
        order.getItems().addAll(newItems);
        recalculate(order, discount, interState);

        if (req.getCustomerName() != null) order.setCustomerName(req.getCustomerName());
        if (req.getCustomerEmail() != null) order.setCustomerEmail(req.getCustomerEmail());
        if (req.getCustomerPhone() != null) order.setCustomerPhone(req.getCustomerPhone());

        return toResponse(orderRepository.save(order));
    }

    /**
     * Fetches order by ID. Added @Transactional to keep session open for DTO mapping.
     */
    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderSummary> getAll(Order.OrderStatus status, LocalDateTime from,
                                               LocalDateTime to, String search, Pageable pageable) {
        return orderRepository.findFiltered(status, from, to, search, pageable).map(this::toSummary);
    }

    @Transactional
    public void cancel(Long id) {
        Order order = findOrThrow(id);
        if (order.getStatus() == Order.OrderStatus.PAID)
            throw new AppException("Cannot cancel a PAID order", HttpStatus.BAD_REQUEST);
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional
    public void markPaid(Long orderId) {
        Order order = findOrThrow(orderId);
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Transactional
    public void markFailed(Long orderId) {
        Order order = findOrThrow(orderId);
        order.setStatus(Order.OrderStatus.FAILED);
        orderRepository.save(order);
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() == Order.OrderStatus.CONFIRMED) return;
        if (order.getStatus() != Order.OrderStatus.DRAFT)
            throw new AppException("Order is not in DRAFT state", HttpStatus.BAD_REQUEST);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    public Order findOrderEntity(Long id) { return findOrThrow(id); }

    public BigDecimal totalRevenue() { return orderRepository.totalRevenue(); }
    public BigDecimal revenueToday() { return orderRepository.revenueFrom(LocalDateTime.now().toLocalDate().atStartOfDay()); }
    public long totalOrdersPaid() { return orderRepository.countPaid(); }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Centralized fetcher using the Join Fetch repository method.
     */
    private Order findOrThrow(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new AppException("Order not found", HttpStatus.NOT_FOUND));
    }

    private Order buildOrder(List<OrderDtos.CartItemRequest> itemReqs, BigDecimal discount,
                             boolean interState, String custName, String custEmail, String custPhone, User creator) {
        Order order = Order.builder()
                .createdBy(creator).customerName(custName).customerEmail(custEmail)
                .customerPhone(custPhone).interState(interState)
                .discountAmount(discount != null ? discount : BigDecimal.ZERO).build();

        List<OrderItem> items = buildItems(itemReqs, order);
        order.getItems().addAll(items);
        recalculate(order, order.getDiscountAmount(), interState);
        return order;
    }

    private List<OrderItem> buildItems(List<OrderDtos.CartItemRequest> itemReqs, Order order) {
        List<OrderItem> items = new ArrayList<>();
        for (var req : itemReqs) {
            Product product = productService.findProductEntity(req.getProductId());
            if (product.getStockQty() < req.getQuantity())
                throw new AppException("Insufficient stock for '" + product.getName() + "'", HttpStatus.BAD_REQUEST);

            BigDecimal lineSubtotal = product.getPrice().multiply(new BigDecimal(req.getQuantity()));
            BigDecimal lineTax = lineSubtotal.multiply(product.getGstPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            items.add(OrderItem.builder()
                    .order(order).product(product).quantity(req.getQuantity())
                    .unitPrice(product.getPrice())
                    .gstPercent(product.getGstPercent())
                    .lineSubtotal(lineSubtotal).lineTax(lineTax).lineTotal(lineTotal)
                    .build());
        }
        return items;
    }

    private void recalculate(Order order, BigDecimal discount, boolean interState) {
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = order.getItems().stream()
                .map(OrderItem::getLineTax).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxableAfterDiscount = subtotal.subtract(discount);
        BigDecimal discountFraction = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? taxableAfterDiscount.divide(subtotal, 8, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        BigDecimal adjustedTax = totalTax.multiply(discountFraction).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalTax(adjustedTax);
        order.setInterState(interState);

        if (interState) {
            order.setIgst(adjustedTax);
            order.setCgst(BigDecimal.ZERO);
            order.setSgst(BigDecimal.ZERO);
        } else {
            BigDecimal half = adjustedTax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            order.setCgst(half);
            order.setSgst(adjustedTax.subtract(half));
            order.setIgst(BigDecimal.ZERO);
        }
        order.setTotalPayable(taxableAfterDiscount.add(adjustedTax));
    }

    public OrderDtos.OrderResponse toResponse(Order o) {
        var items = o.getItems().stream().map(i -> OrderDtos.OrderItemResponse.builder()
                .id(i.getId()).productId(i.getProduct().getId()).productName(i.getProduct().getName())
                .productSku(i.getProduct().getSku()).quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                .gstPercent(i.getGstPercent()).lineSubtotal(i.getLineSubtotal())
                .lineTax(i.getLineTax()).lineTotal(i.getLineTotal()).build()).toList();

        return OrderDtos.OrderResponse.builder()
                .id(o.getId()).customerName(o.getCustomerName()).customerEmail(o.getCustomerEmail())
                .customerPhone(o.getCustomerPhone()).subtotal(o.getSubtotal())
                .discountAmount(o.getDiscountAmount()).cgst(o.getCgst()).sgst(o.getSgst())
                .igst(o.getIgst()).totalTax(o.getTotalTax()).totalPayable(o.getTotalPayable())
                .interState(o.isInterState()).status(o.getStatus()).items(items)
                .createdByEmail(o.getCreatedBy() != null ? o.getCreatedBy().getEmail() : null)
                .createdAt(o.getCreatedAt()).updatedAt(o.getUpdatedAt()).build();
    }

    private OrderDtos.OrderSummary toSummary(Order o) {
        return OrderDtos.OrderSummary.builder().id(o.getId()).customerName(o.getCustomerName())
                .totalPayable(o.getTotalPayable()).status(o.getStatus()).createdAt(o.getCreatedAt()).build();
    }
}