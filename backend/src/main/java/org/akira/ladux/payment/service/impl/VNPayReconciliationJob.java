package org.akira.ladux.payment.service.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Recover missing IPNs even when the customer closes the browser. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.vnpay.reconciliation-enabled", havingValue = "true", matchIfMissing = true)
public class VNPayReconciliationJob {
    private final PaymentRepository payments;
    private final VNPayQueryService queries;

    @Scheduled(fixedDelayString = "${payment.vnpay.reconciliation-delay-ms:60000}", initialDelayString = "60000", scheduler = "vnPayTaskScheduler")
    @SchedulerLock(name = "VNPayReconciliation", lockAtMostFor = "5m", lockAtLeastFor = "5s")
    public void reconcilePending() {
        Instant now = Instant.now();
        var candidates = payments.findVNPayReconciliationCandidates(now.minusSeconds(86400), now.minusSeconds(60),
                now.minusSeconds(300), PageRequest.of(0, 5));
        for (var payment : candidates) {
            try { queries.reconcile(payment.getUserId(), payment.getMerchantTxnRef()); }
            catch (RuntimeException ex) { log.warn("[VNPAY] Background query unavailable ({})", ex.getClass().getSimpleName()); }
        }
    }
}
