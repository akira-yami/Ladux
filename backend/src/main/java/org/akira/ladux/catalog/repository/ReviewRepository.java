package org.akira.ladux.catalog.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.catalog.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @EntityGraph(attributePaths = {"user"})
    @Override
    Page<Review> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Override
    Optional<Review> findById(Integer id);

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByProductId(Integer productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByUserId(Integer userId, Pageable pageable);

    boolean existsByUserIdAndProductId(Integer userId, Integer productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user"})
    Optional<Review> findByUserIdAndId(int userId, int reviewId);

    @EntityGraph(attributePaths = {"user", "user.customer"})
    @Query("SELECT r FROM Review r JOIN r.user u LEFT JOIN u.customer c WHERE " +
            "LOWER(COALESCE(c.fullName, '')) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Review> findByReviewerNameContainingIgnoreCase(String name, Pageable pageable);
}

