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

    @Test
    @DisplayName("U1: onProductDeleted - Gecerli payload icin softDeleteAllByProduct dogru productId ile cagirilir")
    void onProductDeleted_WhenValidPayload_ShouldSoftDeleteByProduct() {
        UUID productId = UUID.randomUUID();
        byte[] payload = MediaTestFixtures.productDeletedPayload(productId).getBytes(StandardCharsets.UTF_8);

        listener.onProductDeleted(payload, "msg-1");

        verify(commandUseCase).softDeleteAllByProduct(productId);
    }

    @Test
    @DisplayName("U2: onProductDeleted - productId eksikse mesaj sessizce atlanir, use case cagrilmaz, exception firlamaz")
    void onProductDeleted_WhenProductIdMissing_ShouldSkipSilently() {
        byte[] payload = MediaTestFixtures.productDeletedPayloadMissingProductId().getBytes(StandardCharsets.UTF_8);

        assertThatCode(() -> listener.onProductDeleted(payload, "msg-2")).doesNotThrowAnyException();

        verify(commandUseCase, never()).softDeleteAllByProduct(any());
    }

    @Test
    @DisplayName("U3: onProductDeleted - productId gecersiz UUID ise mesaj DUSURULUR (retry anlamsiz), exception firlamaz")
    void onProductDeleted_WhenProductIdInvalid_ShouldDropWithoutThrowing() {
        byte[] payload = MediaTestFixtures.productDeletedPayloadInvalidProductId().getBytes(StandardCharsets.UTF_8);

        assertThatCode(() -> listener.onProductDeleted(payload, "msg-3")).doesNotThrowAnyException();

        verify(commandUseCase, never()).softDeleteAllByProduct(any());
    }

    @Test
    @DisplayName("U4: onProductDeleted - use case beklenmedik hata firlatirsa IllegalStateException'a sarilir (RETRY sinyali)")
    void onProductDeleted_WhenUseCaseThrows_ShouldWrapAsIllegalStateExceptionForRetry() {
        UUID productId = UUID.randomUUID();
        byte[] payload = MediaTestFixtures.productDeletedPayload(productId).getBytes(StandardCharsets.UTF_8);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(commandUseCase).softDeleteAllByProduct(productId);

        assertThatThrownBy(() -> listener.onProductDeleted(payload, "msg-4"))
                .isInstanceOf(IllegalStateException.class);
    }
}
