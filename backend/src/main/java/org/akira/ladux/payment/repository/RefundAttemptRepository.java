package org.akira.ladux.payment.repository;

import java.util.Optional;
import org.akira.ladux.payment.entity.RefundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundAttemptRepository extends JpaRepository<RefundAttempt, Long> {
    Optional<RefundAttempt> findByPaymentId(Integer paymentId);
    Optional<RefundAttempt> findByRequestId(String requestId);
    org.springframework.data.domain.Page<RefundAttempt> findByStatusNot(
            RefundAttempt.Status status, org.springframework.data.domain.Pageable pageable);
}
