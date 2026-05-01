package com.mysticmart.inventory.controller;
import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.inventory.dto.ProductDtos;
import com.mysticmart.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
@Tag(name = "Inventory")
public class ProductController {
    private final ProductService productService;

    @GetMapping public ResponseEntity<ApiResponse<Page<ProductDtos.ProductResponse>>> getAll(
            @RequestParam(required=false) String search, @PageableDefault(size=20, sort="name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAll(search, pageable)));
    }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }
    @GetMapping("/low-stock") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<ProductDtos.ProductResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success(productService.getLowStock()));
    }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> create(@Valid @RequestBody ProductDtos.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created", productService.create(req)));
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> update(@PathVariable Long id, @Valid @RequestBody ProductDtos.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, req)));
    }
    @PatchMapping("/{id}/stock") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> adjustStock(@PathVariable Long id, @Valid @RequestBody ProductDtos.StockAdjustRequest req) {
        return ResponseEntity.ok(ApiResponse.success(productService.adjustStock(id, req)));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id); return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
    @GetMapping(value="/{id}/barcode", produces=MediaType.IMAGE_PNG_VALUE)
    @Operation(summary="Download barcode PNG — encodes product ID")
    public ResponseEntity<byte[]> barcode(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=barcode-product-" + id + ".png")
                .body(productService.getBarcodeImage(id));
    }
}
