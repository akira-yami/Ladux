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
    // Tổng tiền chi tiêu và điểm thưởng sau khi được tích lũy
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        Order order = event.getOrder();
        if (order.getUser() == null) return;

        customerRepository.findByUserId(order.getUser().getId()).ifPresent(customer -> {
            // 1. Tích lũy tổng số tiền đã chi tiêu (totalSpent)
            BigDecimal currentSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
            BigDecimal newTotalSpent = currentSpent.add(order.getFinalAmount());
            customer.setTotalSpent(newTotalSpent);

            // 2. Quy đổi điểm thưởng (Ví dụ: 10,000 VNĐ = 1 điểm loyalty)
            Long pointsEarned = order.getFinalAmount().divideToIntegralValue(new BigDecimal("10000")).longValue();
            Long currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            customer.setLoyaltyPoints(currentPoints + pointsEarned);

            customerRepository.save(customer);
        });
    }
}