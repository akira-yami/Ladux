package org.akira.ladux.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    @EntityGraph(attributePaths = {"roles", "customer"})
    @Query("select u from User u join u.customer c where lower(c.email) = lower(:email)")
    Optional<User> findByCustomerEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"roles"})
    @Override
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    @Override
    Optional<User> findById(Integer id);

    @EntityGraph(attributePaths = {"roles"})
    Page<User> findByIsActiveTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"roles", "customer"})
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    /** Tang token_version -> vo hieu hoa tuc thi moi access token cu cua user. */
    @Modifying
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    void incrementTokenVersion(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"roles", "customer"})
    Optional<User> findByGoogleSubject(String googleSubject);

    @EntityGraph(attributePaths = {"roles", "customer"})
    @Query("SELECT u FROM User u LEFT JOIN u.customer c WHERE " +
            "(:name IS NULL AND :phone IS NULL) OR " +
            "(:name IS NOT NULL AND (LOWER(COALESCE(c.fullName, '')) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :name, '%')))) OR " +
            "(:phone IS NOT NULL AND (c.phone LIKE CONCAT('%', :phone, '%') " +
            "OR REPLACE(c.phone, '+84', '0') LIKE CONCAT('%', :phone, '%')))")
    Page<User> findByNameOrPhone(@Param("name") String name,
                                 @Param("phone") String phone,
                                 Pageable pageable);
}
