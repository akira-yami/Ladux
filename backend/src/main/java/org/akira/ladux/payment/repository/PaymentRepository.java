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

