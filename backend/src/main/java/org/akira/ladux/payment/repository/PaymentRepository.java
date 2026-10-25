package org.akira.ladux.payment.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @EntityGraph(attributePaths = {"order"})
    @Query("select p from Payment p where (:orderId is null or p.order.id = :orderId) "
            + "and (:status is null or p.status = :status) and (:reviewOnly = false or p.requiresReview = true)")
    Page<Payment> search(@Param("orderId") Integer orderId, @Param("status") PaymentStatus status,
            @Param("reviewOnly") boolean reviewOnly, Pageable pageable);
    // Scalar lookups do not attach a stale PENDING payment before the order lock is acquired.
    @Query("select p.order.id from Payment p where p.merchantTxnRef = :ref")
    Optional<Integer> findOrderIdByMerchantTxnRef(@Param("ref") String ref);

    @Query("select p.order.id from Payment p where p.id = :id")
    Optional<Integer> findOrderIdByPaymentId(@Param("id") Integer id);

    interface PendingVNPay {
        String getMerchantTxnRef();
        Integer getUserId();
    }

    @Query("""
        select p.merchantTxnRef as merchantTxnRef, p.order.user.id as userId
        from Payment p where p.provider = org.akira.ladux.payment.enums.PaymentProvider.VNPAY
          and p.status = org.akira.ladux.payment.enums.PaymentStatus.PENDING
          and p.paymentUrl is not null and p.createdAt >= :since and p.createdAt <= :before
          and (p.vnpQueriedAt is null or p.vnpQueriedAt <= :queryBefore)
        order by p.createdAt asc
        """)
    List<PendingVNPay> findVNPayReconciliationCandidates(@Param("since") java.time.Instant since,
            @Param("before") java.time.Instant before, @Param("queryBefore") java.time.Instant queryBefore, Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    @Override
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Integer orderId);

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByOrderId(Integer orderId, Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Integer orderId);

    /** Tra cuu theo gateway_transaction_no (vnp_TransactionNo) — phuc vu idempotency va doi soat. */
    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByTransactionNo(String transactionNo);

    /** Lock payment moi nhat cua order — tranh race condition khi hai webhook dong thoi. */
    @EntityGraph(attributePaths = {"order"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findTopByOrder_IdOrderByCreatedAtDesc(Integer orderId);

    @EntityGraph(attributePaths = {"order"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /** For user-scoped "my payments" lists */
    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByOrder_User_Id(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByOrder_User_IdAndStatus(Integer userId, PaymentStatus status, Pageable pageable);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(int orderId, PaymentStatus paymentStatus);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByRequiresReviewDescCreatedAtDesc(int orderId, PaymentStatus status);

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByRequiresReviewTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByMerchantTxnRef(String merchantTxnRef);

    @EntityGraph(attributePaths = {"order"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select p
    from Payment p
    where p.merchantTxnRef = :merchantTxnRef
""")
    Optional<Payment> findByMerchantTxnRefForUpdate(
            @Param("merchantTxnRef")
            String merchantTxnRef
    );
}

