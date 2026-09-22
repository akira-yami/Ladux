package org.akira.ladux.procurement.repository;

import org.akira.ladux.procurement.dto.response.SupplierResponse;
import org.akira.ladux.procurement.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    Page<Supplier> findByIsActiveTrue(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT new org.akira.ladux.procurement.dto.response.SupplierResponse(" +
           "s.id, s.name, s.address, s.phone, s.email, s.isActive, s.createdAt, s.updatedAt) " +
           "FROM Supplier s WHERE " +
           "(:name IS NULL AND :phone IS NULL) OR " +
           "(:name IS NOT NULL AND LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :name, '%'))) OR " +
           "(:phone IS NOT NULL AND (s.phone LIKE CONCAT('%', :phone, '%') " +
           "OR REPLACE(s.phone, '+84', '0') LIKE CONCAT('%', :phone, '%')))")
    Page<SupplierResponse> searchByNameOrPhone(@Param("name") String name,
                                               @Param("phone") String phone,
                                               Pageable pageable);
}
