package org.akira.ladux.payment.repository;

import java.util.Optional;
import org.akira.ladux.payment.entity.RefundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundAttemptRepository extends JpaRepository<RefundAttempt, Long> {
    Optional<RefundAttempt> findByOrderId(Integer orderId);
}
