package com.mysticmart.inventory.service;
import com.mysticmart.common.exception.AppException;
import com.mysticmart.inventory.dto.ProductDtos;
import com.mysticmart.inventory.model.Product;
import com.mysticmart.inventory.repository.ProductRepository;
import com.mysticmart.inventory.util.BarcodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final BarcodeUtil barcodeUtil;

    @Transactional(readOnly = true)
    public Page<ProductDtos.ProductResponse> getAll(String search, Pageable pageable) {
        return productRepository.findActive(search, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value="product", key="#id")
    public ProductDtos.ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value="lowstock")
    public List<ProductDtos.ProductResponse> getLowStock() {
        return productRepository.findLowStock().stream().map(this::toResponse).toList();
    }

    public byte[] getBarcodeImage(Long id) {
        return barcodeUtil.generateForProductId(findOrThrow(id).getId());
    }

    @Transactional @CacheEvict(value={"product","lowstock"}, allEntries=true)
    public ProductDtos.ProductResponse create(ProductDtos.CreateRequest req) {
        if (productRepository.existsBySku(req.getSku()))
            throw new AppException("SKU already exists: " + req.getSku(), HttpStatus.CONFLICT);
        var p = Product.builder().name(req.getName()).sku(req.getSku()).description(req.getDescription())
                .price(req.getPrice()).stockQty(req.getStockQty()).reorderLevel(req.getReorderLevel())
                .category(req.getCategory()).unit(req.getUnit())
                .gstPercent(req.getGstPercent() != null ? req.getGstPercent() : new java.math.BigDecimal("18.00"))
                .build();
        return toResponse(productRepository.save(p));
    }

    @Transactional @CacheEvict(value={"product","lowstock"}, allEntries=true)
    public ProductDtos.ProductResponse update(Long id, ProductDtos.UpdateRequest req) {
        var p = findOrThrow(id);
        if (StringUtils.hasText(req.getName())) p.setName(req.getName());
        if (StringUtils.hasText(req.getDescription())) p.setDescription(req.getDescription());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getReorderLevel() != null) p.setReorderLevel(req.getReorderLevel());
        if (StringUtils.hasText(req.getCategory())) p.setCategory(req.getCategory());
        if (StringUtils.hasText(req.getUnit())) p.setUnit(req.getUnit());
        if (req.getGstPercent() != null) p.setGstPercent(req.getGstPercent());
        return toResponse(productRepository.save(p));
    }

    @Transactional @CacheEvict(value={"product","lowstock"}, allEntries=true)
    public ProductDtos.ProductResponse adjustStock(Long id, ProductDtos.StockAdjustRequest req) {
        var p = findOrThrow(id);
        int newQty = p.getStockQty() + req.getQuantity();
        if (newQty < 0) throw new AppException("Insufficient stock. Available: " + p.getStockQty(), HttpStatus.BAD_REQUEST);
        p.setStockQty(newQty);
        return toResponse(productRepository.save(p));
    }

    @Transactional @CacheEvict(value={"product","lowstock"}, allEntries=true)
    public void delete(Long id) {
        var p = findOrThrow(id); p.setActive(false); productRepository.save(p);
    }

    @Transactional @CacheEvict(value={"product","lowstock"}, allEntries=true)
    public void deductStock(Long productId, int quantity) {
        var p = productRepository.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> new AppException("Product not found: " + productId, HttpStatus.NOT_FOUND));
        int newQty = p.getStockQty() - quantity;
        if (newQty < 0) throw new AppException("Insufficient stock for: " + p.getName() + ". Available: " + p.getStockQty(), HttpStatus.BAD_REQUEST);
        p.setStockQty(newQty);
        productRepository.save(p);
    }

    public Product findProductEntity(Long id) { return findOrThrow(id); }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkLowStock() {
        var low = productRepository.findLowStock();
        if (!low.isEmpty()) {
            log.warn("LOW STOCK: {} products need restocking", low.size());
            low.forEach(p -> log.warn("  id={} name={} stock={} reorderAt={}", p.getId(), p.getName(), p.getStockQty(), p.getReorderLevel()));
        }
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
    }

    private ProductDtos.ProductResponse toResponse(Product p) {
        return ProductDtos.ProductResponse.builder().id(p.getId()).name(p.getName()).sku(p.getSku())
                .description(p.getDescription()).price(p.getPrice()).gstPercent(p.getGstPercent())
                .stockQty(p.getStockQty()).reorderLevel(p.getReorderLevel()).category(p.getCategory())
                .unit(p.getUnit()).active(p.isActive()).lowStock(p.isLowStock())
                .barcodeUrl("/api/products/" + p.getId() + "/barcode")
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}