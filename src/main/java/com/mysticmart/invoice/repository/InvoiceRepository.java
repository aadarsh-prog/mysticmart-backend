package com.mysticmart.invoice.repository;

import com.mysticmart.invoice.model.Invoice;
import com.mysticmart.invoice.model.Invoice.InvoiceStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // FIX #2: works now because the entity field is named `order` (not `purchaseOrder`)
    Optional<Invoice> findByOrderId(Long orderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // FIX #3: JPQL updated — `i.purchaseOrder` → `i.order` to match renamed entity field
    @Query(
            value = """
            SELECT i FROM Invoice i
            WHERE i.status = :status
            AND (:search IS NULL
                 OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(i.order.customerName) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(i) FROM Invoice i
            WHERE i.status = :status
            AND (:search IS NULL
                 OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(i.order.customerName) LIKE LOWER(CONCAT('%', :search, '%')))
            """
    )
    Page<Invoice> findActiveWithSearch(
            @Param("status") InvoiceStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    // FIX #4: declared the missing maxId() used in generateInvoiceNumber()
    // COALESCE handles the empty-table case (returns 0 so first invoice gets id 1)
    @Query("SELECT COALESCE(MAX(i.id), 0) FROM Invoice i")
    long maxId();
}