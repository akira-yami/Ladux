package org.akira.ladux.ordering.event;

import lombok.Getter;
import org.akira.ladux.ordering.entity.Order;

@Getter
public class OrderDeliveredEvent {
    private final Order order;
    public OrderDeliveredEvent(Order order) {
        this.order = order;
    }
}
