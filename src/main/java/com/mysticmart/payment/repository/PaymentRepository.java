package com.mysticmart.payment.repository;
import com.mysticmart.payment.model.Payment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String key);
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
