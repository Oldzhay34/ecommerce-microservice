package com.product.infrastructure.messaging.listener;

import com.product.application.port.out.ProductCachePort;
import com.product.application.port.out.ProductQueryPort;
import com.product.domain.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductSyncListener {

    private final ProductQueryPort productQueryPort;
    private final ProductCachePort productCachePort;
    private final ObjectMapper objectMapper;

    public ProductSyncListener(ProductQueryPort productQueryPort, ProductCachePort productCachePort, ObjectMapper objectMapper) {
        this.productQueryPort = productQueryPort;
        this.productCachePort = productCachePort;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "search.catalog.sync.q") // exchange: ecommerce.topic, routing: catalog.event.*
    public void handleProductSyncEvent(String messagePayload) {
        try {
            String json = messagePayload.trim();

            // Payload çift-encode edilmişse (dış katman JSON string), bir kez soy.
            // Publisher, jsonb değerini String olarak gönderdiğinde mesaj "{\"id\":...}" şeklinde sarılabiliyor.
            if (json.startsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }

            // JSON'u Product POJO'suna çeviriyoruz.
            Product product = objectMapper.readValue(json, Product.class);

            // Elasticsearch'e kaydet/güncelle
            productQueryPort.saveToSearch(product);

            // Veri güncellendiği için listeleme cache'lerini geçersiz kılıyoruz
            productCachePort.invalidateCache("cache:catalog:search:");

        } catch (Exception e) {
            // Hata durumunda exception fırlatılır; RabbitMQ Retry/DLQ mekanizması devreye girer.
            throw new RuntimeException("Failed to sync product to Elasticsearch", e);
        }
    }
}