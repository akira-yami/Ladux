package org.akira.ladux.payment.service.impl;

import java.util.List;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class VNPayReconciliationJobTest {
    @Test void backgroundRecoveryContinuesAfterOneFailedQuery() {
        var payments = mock(PaymentRepository.class);
        var queries = mock(VNPayQueryService.class);
        var first = mock(PaymentRepository.PendingVNPay.class);
        var second = mock(PaymentRepository.PendingVNPay.class);
        when(first.getUserId()).thenReturn(1); when(first.getMerchantTxnRef()).thenReturn("FIRST");
        when(second.getUserId()).thenReturn(2); when(second.getMerchantTxnRef()).thenReturn("SECOND");
        when(payments.findVNPayReconciliationCandidates(any(), any(), any(), any())).thenReturn(List.of(first, second));
        when(queries.reconcile(1, "FIRST")).thenThrow(new RuntimeException("timeout"));
        new VNPayReconciliationJob(payments, queries).reconcilePending();
        verify(queries).reconcile(2, "SECOND");
    }
}
