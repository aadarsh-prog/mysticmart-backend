package com.mysticmart.inventory.util;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.oned.Code128Writer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;

@Component @Slf4j
public class BarcodeUtil {
    public byte[] generateForProductId(Long productId) {
        try {
            var matrix = new Code128Writer().encode(String.valueOf(productId), BarcodeFormat.CODE_128, 300, 80);
            var out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Barcode generation failed for id={}: {}", productId, e.getMessage());
            throw new RuntimeException("Barcode generation failed", e);
        }
    }
}
