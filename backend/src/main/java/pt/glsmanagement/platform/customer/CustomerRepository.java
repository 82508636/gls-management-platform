package pt.glsmanagement.platform.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

interface CustomerRepository extends JpaRepository<Customer, UUID> {
    boolean existsByVatKey(String vatKey);

    boolean existsByVatKeyAndIdNot(String vatKey, UUID id);

    @Query("""
            SELECT customer FROM Customer customer
            WHERE (:active IS NULL OR customer.active = :active)
              AND (:query IS NULL
                   OR LOWER(customer.customerCode) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.shippingName) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.vatNumber) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.contactEmail) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.mobile) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.phone) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.locality) LIKE CONCAT('%', :query, '%')
                   OR LOWER(customer.agency) LIKE CONCAT('%', :query, '%'))
            """)
    Page<Customer> search(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);
}
