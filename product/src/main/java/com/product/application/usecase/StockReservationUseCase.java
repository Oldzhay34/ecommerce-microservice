package com.product.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.product.application.port.out.ProductCommandPort;
import com.product.domain.model.Product;
import com.product.infrastructure.messaging.dto.OrderCreatedEventPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order servisinden gelen OrderCreatedEvent'i işleyip stok rezervasyonu yapan
 * use case.
 *
 * KONTRAT (order-service ile paylaşılan, StockResponseListener'ın beklediği):
 *   routing key  : stock.event.reserved  |  stock.event.rejected
 *   payload      : {"eventType":"StockReservedEvent","orderId":"..."}
 *                  {"eventType":"StockRejectedEvent","orderId":"...","reason":"..."}
 *
 * NOT (bilinçli basitleştirme): Bu implementasyon, siparişteki tüm kalemler
 * için stok yeterliliğini "hepsi ya da hiçbiri" (all-or-nothing) mantığıyla
 * kontrol eder. Gerçek prod ortamında race condition'lara karşı pesimistik
 * kilit (SELECT ... FOR UPDATE) ya da optimistic locking (@Version) ile
 * güçlendirilmesi gerekir; bu, subsystem/integration testlerde ayrıca ele
 * alınacak bir senaryo olarak not düşülmüştür.
 */
@Service
@org.springframework.modulith.NamedInterface("usecase")
public class StockReservationUseCase {

    public static final String EVENT_TYPE_RESERVED = "stock.event.reserved";
    public static final String EVENT_TYPE_REJECTED = "stock.event.rejected";

    private final ProductCommandPort commandPort;
    private final ObjectMapper objectMapper;

    public StockReservationUseCase(ProductCommandPort commandPort, ObjectMapper objectMapper) {
        this.commandPort = commandPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void reserveStock(OrderCreatedEventPayload orderPayload) {
        String orderId = orderPayload.getId();

        // 1) Adım: Her kalem için ürünü bul ve stok yeterliliğini kontrol et.
        Map<UUID, Product> productsById = new HashMap<>();
        for (OrderCreatedEventPayload.OrderItemPayload item : orderPayload.getItems()) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = commandPort.findProductById(productId).orElse(null);

            if (product == null || product.getStock() < item.getQuantity()) {
                publishRejected(orderId, product == null
                        ? "Product not found: " + item.getProductId()
                        : "Insufficient stock for product: " + item.getProductId());
                return;
            }
            productsById.put(productId, product);
        }

        // 2) Adım: Kontrol geçtiyse tüm kalemler için stoktan düş.
        for (OrderCreatedEventPayload.OrderItemPayload item : orderPayload.getItems()) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = productsById.get(productId);
            product.setStock(product.getStock() - item.getQuantity());
            commandPort.saveProduct(product);
        }

        publishReserved(orderId);
    }

    private void publishReserved(String orderId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "StockReservedEvent");
        payload.put("orderId", orderId);
        commandPort.saveOutboxEvent("Order", orderId, EVENT_TYPE_RESERVED, payload.toString());
    }

    private void publishRejected(String orderId, String reason) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "StockRejectedEvent");
        payload.put("orderId", orderId);
        payload.put("reason", reason);
        commandPort.saveOutboxEvent("Order", orderId, EVENT_TYPE_REJECTED, payload.toString());
    }
}