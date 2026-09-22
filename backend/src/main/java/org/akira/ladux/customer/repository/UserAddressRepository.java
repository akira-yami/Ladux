package org.akira.ladux.customer.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.customer.entity.UserAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {
    @EntityGraph(attributePaths = {"user"})
    @Override
    Page<UserAddress> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<UserAddress> findByUserId(Integer userId);

    @EntityGraph(attributePaths = {"user"})
    List<UserAddress> findByUserIdAndIsDefaultTrue(Integer userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<UserAddress> findByUserIdAndId(int userId, int addressId);

    @EntityGraph(attributePaths = {"user"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAddress a where a.user.id = :userId")
    List<UserAddress> findByUserIdForUpdate(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"user"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAddress a where a.user.id = :userId and a.id = :addressId")
    Optional<UserAddress> findByUserIdAndIdForUpdate(@Param("userId") int userId, @Param("addressId") int addressId);

    @EntityGraph(attributePaths = {"user"})
    @Query("select a from UserAddress a where a.id = :id")
    Optional<UserAddress> findByIdWithUser(@Param("id") int id);

    @Modifying(flushAutomatically = true)
    @Query("update UserAddress a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
    int clearDefaultByUserId(@Param("userId") Integer userId);
}

