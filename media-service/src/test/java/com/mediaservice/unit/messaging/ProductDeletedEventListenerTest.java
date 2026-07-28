package com.mediaservice.unit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.infrastructure.messaging.listener.ProductDeletedEventListener;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - product.exchange/product.deleted dinleyicisi. MediaCommandUseCase
 * mock'lanir; broker/DB yoktur (bkz. subsystem katmani icin gercek broker testi).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - ProductDeletedEventListener")
class ProductDeletedEventListenerTest {

    @Mock private MediaCommandUseCase commandUseCase;

    private ProductDeletedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProductDeletedEventListener(commandUseCase, new ObjectMapper());
    }

    /**
     * Gercek bir yayincinin (product-service veya media-service'in kendi
     * OutboxEventPublisher'i) bastigi mesajin AYNISI: ham JSON govde +
     * content-type application/json, tip ipucu header'i YOK.
     */
    private static Message jsonMessage(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(UUID.randomUUID().toString())
                .build();
    }

    @Test
    @DisplayName("U1: onProductDeleted - Gecerli payload icin softDeleteAllByProduct dogru productId ile cagirilir")
    void onProductDeleted_WhenValidPayload_ShouldSoftDeleteByProduct() {
        UUID productId = UUID.randomUUID();
        Message payload = jsonMessage(MediaTestFixtures.productDeletedPayload(productId));

        listener.onProductDeleted(payload);

        verify(commandUseCase).softDeleteAllByProduct(productId);
    }

    @Test
    @DisplayName("U2: onProductDeleted - productId eksikse mesaj sessizce atlanir, use case cagrilmaz, exception firlamaz")
    void onProductDeleted_WhenProductIdMissing_ShouldSkipSilently() {
        Message payload = jsonMessage(MediaTestFixtures.productDeletedPayloadMissingProductId());

        assertThatCode(() -> listener.onProductDeleted(payload)).doesNotThrowAnyException();

        verify(commandUseCase, never()).softDeleteAllByProduct(any());
    }

    @Test
    @DisplayName("U3: onProductDeleted - productId gecersiz UUID ise mesaj DUSURULUR (retry anlamsiz), exception firlamaz")
    void onProductDeleted_WhenProductIdInvalid_ShouldDropWithoutThrowing() {
        Message payload = jsonMessage(MediaTestFixtures.productDeletedPayloadInvalidProductId());

        assertThatCode(() -> listener.onProductDeleted(payload)).doesNotThrowAnyException();

        verify(commandUseCase, never()).softDeleteAllByProduct(any());
    }

    @Test
    @DisplayName("U4: onProductDeleted - use case beklenmedik hata firlatirsa IllegalStateException'a sarilir (RETRY sinyali)")
    void onProductDeleted_WhenUseCaseThrows_ShouldWrapAsIllegalStateExceptionForRetry() {
        UUID productId = UUID.randomUUID();
        Message payload = jsonMessage(MediaTestFixtures.productDeletedPayload(productId));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(commandUseCase).softDeleteAllByProduct(productId);

        assertThatThrownBy(() -> listener.onProductDeleted(payload))
                .isInstanceOf(IllegalStateException.class);
    }
}
