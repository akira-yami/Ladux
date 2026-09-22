package org.akira.ladux.customer.repository;

import java.util.Optional;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // Customer chia se khoa chinh voi User nen id == userId.
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<Customer> findByUserId(Integer userId);

    @EntityGraph(attributePaths = {"user"})
    Page<Customer> findByLevel(CustomerLevel level, Pageable pageable);

    @Query("SELECT new org.akira.ladux.customer.dto.response.CustomerResponse(" +
           "c.id, u.id, c.email, u.username, c.fullName, c.phone, c.avatarUrl, " +
           "c.loyaltyPoints, c.level, c.totalSpent) " +
           "FROM Customer c JOIN c.user u WHERE " +
           "(:name IS NULL AND :phone IS NULL) OR " +
           "(:name IS NOT NULL AND LOWER(COALESCE(c.fullName, '')) LIKE LOWER(CONCAT('%', :name, '%'))) OR " +
           "(:phone IS NOT NULL AND (c.phone LIKE CONCAT('%', :phone, '%') " +
           "OR REPLACE(c.phone, '+84', '0') LIKE CONCAT('%', :phone, '%')))")
    Page<CustomerResponse> findByNameOrPhone(@Param("name") String name,
                                            @Param("phone") String phone,
                                            Pageable pageable);

    boolean existsByPhoneAndIdNot(String normalizedPhone, Integer id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
