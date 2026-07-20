package com.order.infrastructure.search.mapper;

import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.domain.model.OrderStatus;
import com.order.infrastructure.search.document.OrderDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Component
public class OrderDocumentMapper {

    public OrderDocument toDocument(Order domain) {
        if (domain == null) return null;
        OrderDocument doc = new OrderDocument();
        doc.setId(domain.getId());
        doc.setUserId(domain.getUserId());
        doc.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        doc.setTotalAmount(domain.getTotalAmount());

        // GÜNCELLEME: LocalDateTime'dan Instant'a (UTC) dönüştürme
        if (domain.getCreatedAt() != null) {
            doc.setCreatedAt(domain.getCreatedAt().toInstant(ZoneOffset.UTC));
        }

        if (domain.getItems() != null) {
            doc.setItems(domain.getItems().stream().map(item -> {
                OrderDocument.OrderItemDocument itemDoc = new OrderDocument.OrderItemDocument();
                itemDoc.setId(item.getId());
                itemDoc.setProductId(item.getProductId());
                itemDoc.setStoreId(item.getStoreId());
                itemDoc.setQuantity(item.getQuantity());
                itemDoc.setPrice(item.getPrice());
                return itemDoc;
            }).collect(Collectors.toList()));
        }
        return doc;
    }

    public Order toDomain(OrderDocument doc) {
        if (doc == null) return null;
        Order domain = new Order();
        domain.setId(doc.getId());
        domain.setUserId(doc.getUserId());
        domain.setStatus(doc.getStatus() != null ? OrderStatus.valueOf(doc.getStatus()) : null);
        domain.setTotalAmount(doc.getTotalAmount());

        // GÜNCELLEME: Instant'tan LocalDateTime'a (UTC) dönüştürme
        if (doc.getCreatedAt() != null) {
            domain.setCreatedAt(LocalDateTime.ofInstant(doc.getCreatedAt(), ZoneOffset.UTC));
        }

        if (doc.getItems() != null) {
            domain.setItems(doc.getItems().stream().map(itemDoc -> {
                OrderItem item = new OrderItem();
                item.setId(itemDoc.getId());
                item.setProductId(itemDoc.getProductId());
                item.setStoreId(itemDoc.getStoreId());
                item.setQuantity(itemDoc.getQuantity());
                item.setPrice(itemDoc.getPrice());
                return item;
            }).collect(Collectors.toList()));
        }
        return domain;
    }
}