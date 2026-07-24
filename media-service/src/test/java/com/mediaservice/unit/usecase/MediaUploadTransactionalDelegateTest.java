package com.mediaservice.unit.usecase;

import com.mediaservice.application.port.out.MediaCommandPort;
import com.mediaservice.application.usecase.MediaEventFactory;
import com.mediaservice.application.usecase.MediaUploadTransactionalDelegate;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.domain.model.MediaVariant;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - upload akisinin transactional kismi (media_asset + outbox_event).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaUploadTransactionalDelegate")
class MediaUploadTransactionalDelegateTest {

    private static final int MAX_IMAGES = 3;

    @Mock
    private MediaCommandPort commandPort;

    @Mock
    private MediaEventFactory eventFactory;

    private MediaUploadTransactionalDelegate delegate;

    private UUID productId;
    private UUID actorUserId;
    private UUID assetId;
    private Map<MediaVariant, ImageBinary> variants;
    private Map<MediaVariant, String> storageKeys;
    private Map<MediaVariant, String> publicUrls;

    @BeforeEach
    void setUp() {
        delegate = new MediaUploadTransactionalDelegate(commandPort, eventFactory, MAX_IMAGES);
        productId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        variants = new EnumMap<>(MediaVariant.class);
        storageKeys = new EnumMap<>(MediaVariant.class);
        publicUrls = new EnumMap<>(MediaVariant.class);
        for (MediaVariant variant : MediaVariant.values()) {
            variants.put(variant, new ImageBinary(new byte[]{1, 2, 3}, 100, 80, variant));
            storageKeys.put(variant, "products/" + productId + "/" + assetId + "_" + variant.getSuffix() + ".webp");
            publicUrls.put(variant, "https://cdn.test/" + assetId + "_" + variant.getSuffix() + ".webp");
        }
    }

    @Test
    @DisplayName("U1: persist - Urunun ilk gorseli ise sortOrder=0 ve primary=true olarak kaydedilir")
    void persist_WhenFirstImage_ShouldBePrimaryWithSortOrderZero() {
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of());
        when(commandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MediaAsset saved = delegate.persist(productId, actorUserId, assetId, variants, storageKeys, publicUrls);

        assertThat(saved.getSortOrder()).isEqualTo(0);
        assertThat(saved.isPrimary()).isTrue();
        assertThat(saved.getContentType()).isEqualTo("image/webp");
        assertThat(saved.getStoreId()).isEqualTo(actorUserId);
    }

    @Test
    @DisplayName("U2: persist - Ikinci gorsel mevcut en yuksek sortOrder+1 alir ve primary olmaz")
    void persist_WhenSecondImage_ShouldIncrementSortOrderAndNotBePrimary() {
        MediaAsset existing = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, actorUserId, 0, true);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(existing));
        when(commandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MediaAsset saved = delegate.persist(productId, actorUserId, assetId, variants, storageKeys, publicUrls);

        assertThat(saved.getSortOrder()).isEqualTo(1);
        assertThat(saved.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("U3: persist - Kilit altinda limit dolu ise MediaLimitExceededException firlatir ve KAYDETMEZ")
    void persist_WhenLimitReachedUnderLock_ShouldThrowAndNotSave() {
        List<MediaAsset> atLimit = List.of(
                MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, actorUserId, 0, true),
                MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, actorUserId, 1, false),
                MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, actorUserId, 2, false));
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(atLimit);

        assertThatThrownBy(() ->
                delegate.persist(productId, actorUserId, assetId, variants, storageKeys, publicUrls))
                .isInstanceOf(MediaLimitExceededException.class);

        verify(commandPort, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(eventFactory);
    }

    @Test
    @DisplayName("U4: persist - outbox event'i MediaEventFactory uzerinden RK_UPLOADED ile eklenir")
    void persist_ShouldAppendUploadedEventWithAllAssetsAfterUpload() {
        MediaAsset existing = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, actorUserId, 0, true);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(existing));
        when(commandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MediaAsset saved = delegate.persist(productId, actorUserId, assetId, variants, storageKeys, publicUrls);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MediaAsset>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventFactory).append(
                org.mockito.ArgumentMatchers.eq(MediaEventFactory.RK_UPLOADED),
                org.mockito.ArgumentMatchers.eq(MediaEventFactory.EVENT_UPLOADED),
                org.mockito.ArgumentMatchers.eq(productId),
                org.mockito.ArgumentMatchers.eq(saved.getId()),
                listCaptor.capture());

        assertThat(listCaptor.getValue()).hasSize(2).contains(existing, saved);
    }
}
