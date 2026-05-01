package com.mysticmart.payment.service;
import com.mysticmart.common.exception.AppException;
import com.mysticmart.inventory.service.ProductService;
import com.mysticmart.invoice.service.InvoiceService;
import com.mysticmart.order.model.Order;
import com.mysticmart.order.service.OrderService;
import com.mysticmart.payment.dto.PaymentDtos;
import com.mysticmart.payment.model.Payment;
import com.mysticmart.payment.model.PaymentSplit;
import com.mysticmart.payment.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final InvoiceService invoiceService;

    @Value("${razorpay.key.id}") private String razorpayKeyId;
    @Value("${razorpay.key.secret}") private String razorpaySecret;

    @Transactional
    public PaymentDtos.PaymentResponse initiate(PaymentDtos.InitiateRequest req) {
        if (req.getIdempotencyKey() != null) {
            var existing = paymentRepository.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent payment request for key={}", req.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }

        Order order = orderService.findOrderEntity(req.getOrderId());
        if (order.getStatus() == Order.OrderStatus.PAID)
            throw new AppException("Order is already PAID", HttpStatus.BAD_REQUEST);
        if (order.getStatus() == Order.OrderStatus.CANCELLED)
            throw new AppException("Cannot pay a CANCELLED order", HttpStatus.BAD_REQUEST);

        orderService.confirmOrder(order.getId());

        Payment payment;

        switch (req.getMode()) {
            case CASH -> {
                BigDecimal cashReceived = req.getCashReceived();
                if (cashReceived == null || cashReceived.compareTo(order.getTotalPayable()) < 0)
                    throw new AppException("Cash received (" + cashReceived + ") is less than order total (" + order.getTotalPayable() + ")", HttpStatus.BAD_REQUEST);
                BigDecimal change = cashReceived.subtract(order.getTotalPayable()).setScale(2, RoundingMode.HALF_UP);
                payment = Payment.builder()
                        .order(order).amount(order.getTotalPayable())
                        .mode(Payment.PaymentMode.CASH)
                        .cashReceived(cashReceived).changeReturned(change)
                        .idempotencyKey(req.getIdempotencyKey())
                        .status(Payment.PaymentStatus.INITIATED).build();
                Payment saved = paymentRepository.save(payment);
                return confirmCashPayment(saved, order);
            }
            case SPLIT -> {
                payment = buildSplitPayment(req, order);
                return toResponse(paymentRepository.save(payment));
            }
            default -> {
                // CARD / UPI / WALLET — create real Razorpay order
                String gatewayOrderId = createRazorpayOrder(order.getTotalPayable());
                payment = Payment.builder()
                        .order(order).amount(order.getTotalPayable()).mode(req.getMode())
                        .gatewayOrderId(gatewayOrderId)
                        .idempotencyKey(req.getIdempotencyKey())
                        .status(Payment.PaymentStatus.PROCESSING).build();
                return toResponse(paymentRepository.save(payment));
            }
        }
    }

    @Transactional
    public PaymentDtos.PaymentResponse verifyDigitalPayment(PaymentDtos.VerifyRequest req) {
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS)
            throw new AppException("Payment already confirmed", HttpStatus.BAD_REQUEST);

        String computed = computeHmac(req.getGatewayOrderId() + "|" + req.getGatewayPaymentId(), razorpaySecret);
        if (!computed.equals(req.getGatewaySignature()))
            throw new AppException("Payment signature verification failed", HttpStatus.BAD_REQUEST);

        payment.setGatewayPaymentId(req.getGatewayPaymentId());
        payment.setGatewaySignature(req.getGatewaySignature());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        completeSuccessfulPayment(payment.getOrder(), payment);

        log.info("Digital payment verified: orderId={} amount={}", payment.getOrder().getId(), payment.getAmount());
        return toResponse(payment);
    }

    @Transactional
    public PaymentDtos.PaymentResponse reportFailure(PaymentDtos.FailureRequest req) {
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(req.getReason());
        payment.setRetryCount(payment.getRetryCount() + 1);
        paymentRepository.save(payment);

        orderService.markFailed(payment.getOrder().getId());
        log.warn("Payment failed: orderId={} reason={}", payment.getOrder().getId(), req.getReason());
        return toResponse(payment);
    }

    @Transactional
    public void retryAllowed(Long orderId) {
        Order order = orderService.findOrderEntity(orderId);
        if (order.getStatus() != Order.OrderStatus.FAILED)
            throw new AppException("Only FAILED orders can be retried", HttpStatus.BAD_REQUEST);
        order.setStatus(Order.OrderStatus.DRAFT);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse getByOrderId(Long orderId) {
        return toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException("No payment for order", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<PaymentDtos.PaymentResponse> getAll(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional
    public PaymentDtos.PaymentResponse refund(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));
        if (p.getStatus() != Payment.PaymentStatus.SUCCESS)
            throw new AppException("Only SUCCESS payments can be refunded", HttpStatus.BAD_REQUEST);
        p.setStatus(Payment.PaymentStatus.REFUNDED);
        return toResponse(paymentRepository.save(p));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String createRazorpayOrder(BigDecimal amount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);
            JSONObject options = new JSONObject();
            // Razorpay requires amount in paise (1 INR = 100 paise)
            options.put("amount", amount.multiply(new BigDecimal("100")).intValue());
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());
            options.put("payment_capture", 1);
            com.razorpay.Order razorpayOrder = client.orders.create(options);
            String gatewayOrderId = razorpayOrder.get("id");
            log.info("Razorpay order created: {}", gatewayOrderId);
            return gatewayOrderId;
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new AppException("Failed to create Razorpay order: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PaymentDtos.PaymentResponse confirmCashPayment(Payment payment, Order order) {
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        completeSuccessfulPayment(order, payment);
        log.info("Cash payment: orderId={} received={} change={}", order.getId(), payment.getCashReceived(), payment.getChangeReturned());
        return toResponse(payment);
    }

    private void completeSuccessfulPayment(Order order, Payment payment) {
        orderService.markPaid(order.getId());

        for (var item : order.getItems()) {
            productService.deductStock(item.getProduct().getId(), item.getQuantity());
        }

        invoiceService.generateInvoice(order, payment);

        log.info("Payment complete: orderId={} stockDeducted={} items invoiceGenerated",
                order.getId(), order.getItems().size());
    }

    private Payment buildSplitPayment(PaymentDtos.InitiateRequest req, Order order) {
        if (req.getSplits() == null || req.getSplits().isEmpty())
            throw new AppException("SPLIT payment requires split components", HttpStatus.BAD_REQUEST);

        BigDecimal splitsTotal = req.getSplits().stream()
                .map(PaymentDtos.SplitComponent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (splitsTotal.compareTo(order.getTotalPayable()) != 0)
            throw new AppException("Split amounts (" + splitsTotal + ") do not equal order total (" + order.getTotalPayable() + ")", HttpStatus.BAD_REQUEST);

        Payment payment = Payment.builder()
                .order(order).amount(order.getTotalPayable())
                .mode(Payment.PaymentMode.SPLIT)
                .idempotencyKey(req.getIdempotencyKey())
                .status(Payment.PaymentStatus.INITIATED).build();

        for (var s : req.getSplits()) {
            PaymentSplit split = new PaymentSplit();
            split.setPayment(payment);
            split.setMode(s.getMode());
            split.setAmount(s.getAmount());
            if (s.getMode() == Payment.PaymentMode.CASH && s.getCashReceived() != null) {
                split.setCashReceived(s.getCashReceived());
                split.setChangeReturned(s.getCashReceived().subtract(s.getAmount()).max(BigDecimal.ZERO));
            }
            payment.getSplits().add(split);
        }
        return payment;
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new AppException("HMAC computation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PaymentDtos.PaymentResponse toResponse(Payment p) {
        var splits = p.getSplits().stream().map(s -> PaymentDtos.SplitResponse.builder()
                .mode(s.getMode()).amount(s.getAmount())
                .cashReceived(s.getCashReceived()).changeReturned(s.getChangeReturned())
                .gatewayPaymentId(s.getGatewayPaymentId()).build()).toList();
        return PaymentDtos.PaymentResponse.builder()
                .id(p.getId()).orderId(p.getOrder().getId()).amount(p.getAmount())
                .mode(p.getMode()).status(p.getStatus()).idempotencyKey(p.getIdempotencyKey())
                .gatewayOrderId(p.getGatewayOrderId()).gatewayPaymentId(p.getGatewayPaymentId())
                .cashReceived(p.getCashReceived()).changeReturned(p.getChangeReturned())
                .failureReason(p.getFailureReason()).retryCount(p.getRetryCount())
                .splits(splits).paidAt(p.getPaidAt()).createdAt(p.getCreatedAt()).build();
    }
}