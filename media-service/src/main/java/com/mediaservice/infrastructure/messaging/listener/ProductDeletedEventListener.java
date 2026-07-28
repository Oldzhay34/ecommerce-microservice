package com.mediaservice.infrastructure.messaging.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.infrastructure.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * product.exchange / product.deleted dinlenir. Urun silindiginde o urunun TUM media asset'leri
 * soft delete edilir ve cache invalidate edilir.
 *
 * [Varsayim 1 - EN ONCELIKLI] Product servisi ProductDeletedEvent yayinliyor mu? Yayinlamiyorsa
 * bu listener PASIF kalir - kuyruk olusur, mesaj gelmez, kod zararsizdir. Product servisine bu
 * event eklenene kadar hicbir sey yapmaz. DOGRULANMALIDIR.
 *
 * Beklenen payload (esnek okuma - productId zorunlu, digerleri opsiyonel):
 *   { "eventId": "uuid", "eventType": "ProductDeletedEvent",
 *     "occurredAt": "ISO-8601", "productId": "uuid" }
 */
@Component
public class ProductDeletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductDeletedEventListener.class);

    private final MediaCommandUseCase commandUseCase;
    private final ObjectMapper objectMapper;

    public ProductDeletedEventListener(MediaCommandUseCase commandUseCase, ObjectMapper objectMapper) {
        this.commandUseCase = commandUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * Hata durumunda mesaj retry edilir (spring.rabbitmq.listener.simple.retry, max 3),
     * tukenirse media.dlx uzerinden DLQ'ya dusuruilur.
     *
     * Idempotent: softDeleteAllByProduct zaten ACTIVE kayit yoksa hicbir sey yapmaz.
     * <p>
     * Parametre bilerek HAM {@link Message}'dir, {@code @Payload byte[]} DEGILDIR:
     * bean olarak tanimli {@code Jackson2JsonMessageConverter}, {@code application/json}
     * content-type'li bir mesaji once bir Java nesnesine cevirir (tip ipucu yoksa
     * {@code Map}, {@code __TypeId__} varsa o tipe) ve sonrasinda {@code byte[]}'a
     * donusturulemedigi icin MessageConversionException firlatilirdi - yani gercek bir
     * product-service'in bastigi her JSON mesaj DLQ'ya duserdi. Ham Message parametresi
     * payload donusumunu tamamen devre disi birakir; JSON'i zaten asagida ObjectMapper
     * ile kendimiz okuyoruz (bkz. OutboxEventPublisher.buildMessage - yayin tarafi da
     * ayni sekilde ham byte + content-type json basar).
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_PRODUCT_DELETED)
    public void onProductDeleted(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode productIdNode = node.get("productId");

            if (productIdNode == null || productIdNode.isNull() || productIdNode.asText().isBlank()) {
                log.warn("ProductDeletedEvent icinde productId yok, mesaj atlaniyor. messageId={}, payload={}",
                        messageId, payload);
                return;
            }

            UUID productId = UUID.fromString(productIdNode.asText());
            commandUseCase.softDeleteAllByProduct(productId);

            log.info("ProductDeletedEvent islendi. productId={}, messageId={}", productId, messageId);

        } catch (IllegalArgumentException e) {
            // Bozuk UUID - retry anlamsiz, mesaji dusur.
            log.error("ProductDeletedEvent icinde gecersiz productId, mesaj atlaniyor. payload={}", payload, e);
        } catch (Exception e) {
            log.error("ProductDeletedEvent islenemedi, retry edilecek. messageId={}, payload={}",
                    messageId, payload, e);
            throw new IllegalStateException("ProductDeletedEvent islenemedi", e);
        }
    }
}