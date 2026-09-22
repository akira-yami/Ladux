package org.akira.ladux.ordering.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.ordering.mapper.OrderHistoryMapper;
import org.akira.ladux.ordering.repository.OrderHistoryRepository;
import org.akira.ladux.ordering.service.OrderHistoryService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {
    private final OrderHistoryRepository repo;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderHistories", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<OrderHistoryResponse> getAllOrderHistories(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(OrderHistoryMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderHistories", key = "'id:' + #id")
    public OrderHistoryResponse getOrderHistoryById(int id) {
        return OrderHistoryMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order history voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderHistories", key = "'order:' + #orderId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<OrderHistoryResponse> getOrderHistoriesByOrderId(int orderId, Pageable pageable) {
        return PageResponse.from(repo.findByOrderId(orderId, pageable)
                .map(OrderHistoryMapper::fromEntity));
    }
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderHistories", key = "'user:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<OrderHistoryResponse> getOrdersHistoryByUser(Integer userId, Pageable pageable) {
        return PageResponse.from(repo.findByUserId(userId, pageable)
                .map(OrderHistoryMapper::fromEntity));
    }
}
