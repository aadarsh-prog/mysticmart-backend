package com.mysticmart.order.repository;
import com.mysticmart.order.model.Order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // ... your other queries ...

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    // Existing queries below...
    @Query("""
         SELECT o FROM Order o WHERE
        (:status IS NULL OR o.status = :status) AND
        (:from IS NULL OR o.createdAt >= :from) AND
        (:to IS NULL OR o.createdAt <= :to) AND
        (:search IS NULL OR LOWER(o.customerName) LIKE LOWER(CONCAT('%',:search,'%'))
             OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%',:search,'%')))""")
    Page<Order> findFiltered(@Param("status") Order.OrderStatus status,
                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("search") String search, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalPayable),0) FROM Order o WHERE o.status = 'PAID'")
    BigDecimal totalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalPayable),0) FROM Order o WHERE o.status = 'PAID' AND o.createdAt >= :from")
    BigDecimal revenueFrom(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID'")
    long countPaid();
}