package org.akira.ladux.ordering.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.response.OrderItemResponse;
import org.akira.ladux.ordering.mapper.OrderItemMapper;
import org.akira.ladux.ordering.repository.OrderItemRepository;
import org.akira.ladux.ordering.service.OrderItemService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository repo;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderItems", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<OrderItemResponse> getAllOrderItems(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(OrderItemMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderItems", key = "'id:' + #id")
    public OrderItemResponse getOrderItemById(int id) {
        return OrderItemMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order item voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderItems", key = "'order:' + #orderId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<OrderItemResponse> getOrderItemsByOrderId(int orderId, Pageable pageable) {
        return PageResponse.from(repo.findByOrderId(orderId, pageable)
                .map(OrderItemMapper::fromEntity));
    }

}
