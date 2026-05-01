package com.mysticmart.invoice.service;

import com.mysticmart.invoice.model.Invoice;
import com.mysticmart.order.model.Order;
import com.mysticmart.order.model.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendInvoiceEmail(Invoice invoice, Order order) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            log.warn("Skipping invoice email for order={}: no customer email", order.getId());
            return;
        }
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(order.getCustomerEmail());
            helper.setSubject("Invoice " + invoice.getInvoiceNumber() + " - MysticMart");
            helper.setText(buildHtml(invoice, order), true);
            mailSender.send(msg);
            log.info("Invoice email sent: {} to {}", invoice.getInvoiceNumber(), order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send invoice email for {}: {}", invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    private String buildHtml(Invoice inv, Order order) {
        var sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>");
        sb.append("<div style='background:#1A3557;color:white;padding:20px;border-radius:8px 8px 0 0'>");
        sb.append("<h1 style='margin:0'>MysticMart</h1><p style='margin:4px 0 0;opacity:0.7'>Tax Invoice</p></div>");
        sb.append("<div style='border:1px solid #ddd;border-top:none;padding:24px;border-radius:0 0 8px 8px'>");
        sb.append("<h2>Invoice #").append(inv.getInvoiceNumber()).append("</h2>");
        if (order.getCustomerName() != null)
            sb.append("<p><strong>Customer:</strong> ").append(order.getCustomerName()).append("</p>");
        sb.append("<table style='width:100%;border-collapse:collapse'>");
        sb.append("<tr style='background:#f5f5f5'>");
        sb.append("<th style='padding:10px;text-align:left;border:1px solid #ddd'>Item</th>");
        sb.append("<th style='padding:10px;text-align:right;border:1px solid #ddd'>Qty</th>");
        sb.append("<th style='padding:10px;text-align:right;border:1px solid #ddd'>Price</th>");
        sb.append("<th style='padding:10px;text-align:right;border:1px solid #ddd'>GST</th>");
        sb.append("<th style='padding:10px;text-align:right;border:1px solid #ddd'>Total</th></tr>");
        for (OrderItem item : order.getItems()) {
            sb.append("<tr>")
                    .append("<td style='padding:8px;border:1px solid #ddd'>").append(item.getProduct().getName()).append("</td>")
                    .append("<td style='padding:8px;text-align:right;border:1px solid #ddd'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding:8px;text-align:right;border:1px solid #ddd'>₹").append(item.getUnitPrice()).append("</td>")
                    .append("<td style='padding:8px;text-align:right;border:1px solid #ddd'>").append(item.getGstPercent()).append("%</td>")
                    .append("<td style='padding:8px;text-align:right;border:1px solid #ddd'>₹").append(item.getLineTotal()).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table><div style='text-align:right;margin-top:16px'>");
        sb.append("<p>Subtotal: ₹").append(inv.getSubtotal()).append("</p>");
        if (inv.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
            sb.append("<p>Discount: -₹").append(inv.getDiscountAmount()).append("</p>");
        if (inv.getCgst().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("<p>CGST: ₹").append(inv.getCgst()).append("</p>");
            sb.append("<p>SGST: ₹").append(inv.getSgst()).append("</p>");
        } else {
            sb.append("<p>IGST: ₹").append(inv.getIgst()).append("</p>");
        }
        sb.append("<p><strong>Total: ₹").append(inv.getTotalAmount()).append("</strong></p>");
        sb.append("<p>Payment Mode: ").append(inv.getPaymentMode()).append("</p></div>");
        sb.append("<hr/><p style='color:#888;font-size:12px;text-align:center'>Thank you for shopping at MysticMart!</p>");
        sb.append("</div></body></html>");
        return sb.toString();
    }
}