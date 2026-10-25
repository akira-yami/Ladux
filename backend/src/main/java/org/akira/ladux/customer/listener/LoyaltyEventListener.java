package org.akira.ladux.customer.listener;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.event.OrderDeliveredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyEventListener {

    private final CustomerRepository customerRepository;

    @EventListener
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true)
    // Tổng tiền chi tiêu và điểm thưởng sau khi được tích lũy
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        Order order = event.getOrder();
        if (order.getUser() == null) return;

        long points = order.getFinalAmount().divideToIntegralValue(new BigDecimal("10000")).longValueExact();
        customerRepository.creditLoyalty(order.getUser().getId(), order.getFinalAmount(), points);
    }
}
