package org.akira.ladux.inventory.service;

import java.util.List;
import org.akira.ladux.inventory.service.result.LineDraft;
import org.akira.ladux.ordering.dto.OrderLineRequest;

public interface InventoryService {
    List<LineDraft> reserveStockAndPriceLines(List<OrderLineRequest> items);
}

