package com.mediaservice.unit.usecase;

import com.mediaservice.application.port.out.OutboxPort;
import com.mediaservice.application.usecase.MediaEventFactory;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Katman: UNIT - outbox payload'inin TEK kaynagi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaEventFactory")
class MediaEventFactoryTest {

    @Mock
    private OutboxPort outboxPort;

    private MediaEventFactory eventFactory;

    private UUID productId;
    private UUID assetId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        eventFactory = new MediaEventFactory(outboxPort);
        productId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U1: append - primary olan asset varsa payload'da primaryUrl/primaryThumbUrl doldurulur")
    void append_WhenPrimaryExists_ShouldFillPrimaryUrls() {
        MediaAsset primary = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        MediaAsset secondary = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 1, false);

        eventFactory.append(MediaEventFactory.RK_UPLOADED, MediaEventFactory.EVENT_UPLOADED,
                productId, assetId, List.of(primary, secondary));

        Map<String, Object> payload = capturePayload();
        assertThat(payload.get("primaryUrl")).isEqualTo(primary.getUrl());
        assertThat(payload.get("primaryThumbUrl")).isEqualTo(primary.getThumbUrl());
        assertThat(payload.get("imageCount")).isEqualTo(2);
    }

    @Test
    @DisplayName("U2: append - primary yoksa (veya deleted primary) primaryUrl/primaryThumbUrl null olur")
    void append_WhenNoPrimary_ShouldLeavePrimaryUrlsNull() {
        MediaAsset nonPrimary = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, false);

        eventFactory.append(MediaEventFactory.RK_DELETED, MediaEventFactory.EVENT_DELETED,
                productId, assetId, List.of(nonPrimary));

        Map<String, Object> payload = capturePayload();
        assertThat(payload.get("primaryUrl")).isNull();
        assertThat(payload.get("primaryThumbUrl")).isNull();
    }

    @Test
    @DisplayName("U3: append - imageCount yalnizca ACTIVE asset'leri sayar, DELETED olanlari saymaz")
    void append_ImageCount_ShouldCountOnlyActiveAssets() {
        MediaAsset active = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, false);
        MediaAsset deleted = MediaTestFixtures.deletedAsset(UUID.randomUUID(), productId, storeId);

        eventFactory.append(MediaEventFactory.RK_UPDATED, MediaEventFactory.EVENT_UPDATED,
                productId, assetId, List.of(active, deleted));

        Map<String, Object> payload = capturePayload();
        assertThat(payload.get("imageCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("U4: append - eventId/eventType/productId/assetId alanlari dogru doldurulur")
    void append_ShouldFillCoreFields() {
        eventFactory.append(MediaEventFactory.RK_UPLOADED, MediaEventFactory.EVENT_UPLOADED,
                productId, assetId, List.of());

        Map<String, Object> payload = capturePayload();
        assertThat(payload.get("eventType")).isEqualTo(MediaEventFactory.EVENT_UPLOADED);
        assertThat(payload.get("productId")).isEqualTo(productId.toString());
        assertThat(payload.get("assetId")).isEqualTo(assetId.toString());
        assertThat((String) payload.get("eventId")).isNotBlank();
        assertThat(UUID.fromString((String) payload.get("eventId"))).isNotNull();

        verify(outboxPort).append(eq(MediaEventFactory.AGGREGATE_TYPE), eq(productId),
                eq(MediaEventFactory.EVENT_UPLOADED), eq(MediaEventFactory.RK_UPLOADED), any());
    }

    @Test
    @DisplayName("U5: append - assetId null olabilir (softDeleteAllByProduct akisi)")
    void append_WhenAssetIdNull_ShouldPutNullInPayload() {
        eventFactory.append(MediaEventFactory.RK_DELETED, MediaEventFactory.EVENT_DELETED,
                productId, null, List.of());

        Map<String, Object> payload = capturePayload();
        assertThat(payload.get("assetId")).isNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturePayload() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPort).append(any(), any(), any(), any(), captor.capture());
        return (Map<String, Object>) captor.getValue();
    }
}
