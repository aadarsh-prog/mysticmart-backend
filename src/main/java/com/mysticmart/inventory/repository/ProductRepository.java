package com.mysticmart.inventory.repository;
import com.mysticmart.inventory.model.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
SELECT p FROM Product p WHERE p.active = true
        AND (:s IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:s,'%'))
             OR LOWER(p.sku) LIKE LOWER(CONCAT('%',:s,'%'))
             OR LOWER(COALESCE(p.category,'')) LIKE LOWER(CONCAT('%',:s,'%')))""")
    Page<Product> findActive(@Param("s") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQty <= p.reorderLevel")
    List<Product> findLowStock();

    boolean existsBySku(String sku);
}
