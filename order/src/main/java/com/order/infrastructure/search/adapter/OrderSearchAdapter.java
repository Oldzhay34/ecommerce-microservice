package com.order.infrastructure.search.adapter;

import com.order.application.port.out.OrderQueryPort;
import com.order.domain.model.Order;
import com.order.infrastructure.search.document.OrderDocument;
import com.order.infrastructure.search.mapper.OrderDocumentMapper;
import com.order.infrastructure.search.repository.OrderSearchRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderSearchAdapter implements OrderQueryPort {

    private final OrderSearchRepository orderSearchRepository;
    private final OrderDocumentMapper orderDocumentMapper;

    public OrderSearchAdapter(OrderSearchRepository orderSearchRepository, OrderDocumentMapper orderDocumentMapper) {
        this.orderSearchRepository = orderSearchRepository;
        this.orderDocumentMapper = orderDocumentMapper;
    }

    @Override
    public Optional<Order> findById(String id) {
        return orderSearchRepository.findById(id).map(orderDocumentMapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return orderSearchRepository.findByUserId(userId).stream()
                .map(orderDocumentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStoreId(String storeId) {
        return orderSearchRepository.findByItemsStoreId(storeId).stream()
                .map(orderDocumentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        Iterable<OrderDocument> docs = orderSearchRepository.findAll();
        for (OrderDocument doc : docs) {
            list.add(orderDocumentMapper.toDomain(doc));
        }
        return list;
    }

    public void indexOrder(Order order) {
        OrderDocument doc = orderDocumentMapper.toDocument(order);
        orderSearchRepository.save(doc);
    }
}