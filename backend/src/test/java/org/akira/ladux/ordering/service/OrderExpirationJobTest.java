package org.akira.ladux.ordering.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.notification.repository.NotificationRepository;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.impl.OrderStateMachineImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderExpirationJobTest {

    @Test
    void oneFailingOrderDoesNotBlockTheRestOfTheBatch() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderExpirationWorker worker = mock(OrderExpirationWorker.class);
        OrderStateMachineImpl job = new OrderStateMachineImpl(orders, mock(OrderLifecycleService.class),
                mock(ApplicationEventPublisher.class), mock(UserRepository.class), mock(NotificationRepository.class), worker);
        when(orders.findExpiredOrderIds(eq(OrderStatus.PENDING), any(Instant.class), any())).thenReturn(List.of(1, 2, 3));
        doThrow(new IllegalStateException("bad stock row")).when(worker).expireIfStillPending(eq(2), any(Instant.class));

        job.expirePendingOrders();

        verify(worker).expireIfStillPending(eq(1), any(Instant.class));
        verify(worker).expireIfStillPending(eq(2), any(Instant.class));
        verify(worker).expireIfStillPending(eq(3), any(Instant.class));
    }

    @Test
    void workerSkipsOrderPaidWhileJobWasRunning() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderLifecycleService lifecycle = mock(OrderLifecycleService.class);
        OrderExpirationWorker worker = new OrderExpirationWorker(orders, lifecycle);
        Instant cutoff = Instant.now();
        Order confirmed = Order.builder().id(7).user(User.builder().id(2).build())
                .status(OrderStatus.CONFIRMED).paymentExpiresAt(cutoff.minusSeconds(60)).build();
        when(orders.findWithItemsByIdForUpdate(7)).thenReturn(Optional.of(confirmed));

        assertFalse(worker.expireIfStillPending(7, cutoff));
        verify(lifecycle, never()).cancelOrder(any(Order.class), anyString());
    }

    @Test
    void workerCancelsOrderStillPendingAfterLock() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderLifecycleService lifecycle = mock(OrderLifecycleService.class);
        OrderExpirationWorker worker = new OrderExpirationWorker(orders, lifecycle);
        Instant cutoff = Instant.now();
        Order pending = Order.builder().id(8).user(User.builder().id(2).build())
                .status(OrderStatus.PENDING).paymentExpiresAt(cutoff.minusSeconds(60)).build();
        when(orders.findWithItemsByIdForUpdate(8)).thenReturn(Optional.of(pending));

        assertTrue(worker.expireIfStillPending(8, cutoff));
        verify(lifecycle).cancelOrder(eq(pending), anyString());
    }
}
