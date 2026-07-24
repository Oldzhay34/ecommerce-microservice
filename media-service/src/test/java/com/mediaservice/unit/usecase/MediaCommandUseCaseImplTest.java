package com.mediaservice.unit.usecase;

import com.mediaservice.application.port.out.ImageConverterPort;
import com.mediaservice.application.port.out.MediaCachePort;
import com.mediaservice.application.port.out.MediaCommandPort;
import com.mediaservice.application.port.out.MediaQueryPort;
import com.mediaservice.application.port.out.StoragePort;
import com.mediaservice.application.usecase.MediaCommandUseCaseImpl;
import com.mediaservice.application.usecase.MediaEventFactory;
import com.mediaservice.application.usecase.MediaUploadTransactionalDelegate;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.exception.MediaAssetNotFoundException;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - yazma tarafi use case'leri, tum port'lar mock'lanir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaCommandUseCaseImpl")
class MediaCommandUseCaseImplTest {

    private static final int MAX_IMAGES = 10;

    @Mock private MediaCommandPort commandPort;
    @Mock private MediaQueryPort queryPort;
    @Mock private MediaCachePort cachePort;
    @Mock private StoragePort storagePort;
    @Mock private ImageConverterPort imageConverterPort;
    @Mock private MediaEventFactory eventFactory;
    @Mock private MediaUploadTransactionalDelegate uploadDelegate;

    private MediaCommandUseCaseImpl useCase;

    private UUID productId;
    private UUID storeId;
    private UUID otherStoreId;

    @BeforeEach
    void setUp() {
        useCase = new MediaCommandUseCaseImpl(commandPort, queryPort, cachePort, storagePort,
                imageConverterPort, eventFactory, uploadDelegate, MAX_IMAGES);
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        otherStoreId = UUID.randomUUID();
    }

    // ------------------------------------------------------------------
    // uploadProductImage
    // ------------------------------------------------------------------

    @Test
    @DisplayName("U1: uploadProductImage - Format gecersizse limit/donusum/upload HIC calismaz")
    void uploadProductImage_WhenFormatInvalid_ShouldPropagateAndSkipRest() {
        byte[] bytes = {1, 2, 3};
        org.mockito.Mockito.doThrow(new com.mediaservice.domain.exception.UnsupportedMediaFormatException("kotu format"))
                .when(imageConverterPort).validateFormat(anyString(), any());

        assertThatThrownBy(() -> useCase.uploadProductImage(productId, storeId, false, "image/png", bytes))
                .isInstanceOf(com.mediaservice.domain.exception.UnsupportedMediaFormatException.class);

        verifyNoInteractions(commandPort, storagePort, uploadDelegate, cachePort);
    }

    @Test
    @DisplayName("U2: uploadProductImage - On kontrolde limit doluysa MediaLimitExceededException ve donusum yapilmaz")
    void uploadProductImage_WhenPreCheckLimitReached_ShouldThrowBeforeConversion() {
        when(commandPort.countActiveByProductId(productId)).thenReturn((long) MAX_IMAGES);

        assertThatThrownBy(() -> useCase.uploadProductImage(productId, storeId, false, "image/png", new byte[]{1}))
                .isInstanceOf(MediaLimitExceededException.class);

        verify(imageConverterPort, never()).convertToWebpVariants(any());
        verifyNoInteractions(uploadDelegate);
    }

    @Test
    @DisplayName("U3: uploadProductImage - Basarili akis: 3 varyant storage'a yazilir, delegate cagrilir, cache invalidate edilir")
    void uploadProductImage_WhenValid_ShouldUploadAllVariantsAndInvalidateCache() {
        when(commandPort.countActiveByProductId(productId)).thenReturn(0L);
        Map<MediaVariant, ImageBinary> variants = Map.of(
                MediaVariant.THUMB, new ImageBinary(new byte[]{1}, 10, 10, MediaVariant.THUMB),
                MediaVariant.MEDIUM, new ImageBinary(new byte[]{2}, 20, 20, MediaVariant.MEDIUM),
                MediaVariant.ORIGINAL, new ImageBinary(new byte[]{3}, 30, 30, MediaVariant.ORIGINAL));
        when(imageConverterPort.convertToWebpVariants(any())).thenReturn(variants);
        when(storagePort.upload(anyString(), any())).thenReturn("https://cdn.test/x.webp");
        MediaAsset persisted = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        when(uploadDelegate.persist(eq(productId), eq(storeId), any(), any(), any(), any())).thenReturn(persisted);

        MediaAsset result = useCase.uploadProductImage(productId, storeId, false, "image/png", new byte[]{9, 9});

        assertThat(result).isEqualTo(persisted);
        verify(storagePort, times(3)).upload(anyString(), any());
        verify(cachePort).invalidate(productId);
    }

    // ------------------------------------------------------------------
    // deleteProductImage
    // ------------------------------------------------------------------

    @Test
    @DisplayName("U4: deleteProductImage - Asset bulunamazsa MediaAssetNotFoundException")
    void deleteProductImage_WhenNotFound_ShouldThrow() {
        UUID assetId = UUID.randomUUID();
        when(queryPort.findById(assetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deleteProductImage(assetId, storeId, false))
                .isInstanceOf(MediaAssetNotFoundException.class);
    }

    @Test
    @DisplayName("U5: deleteProductImage - Baska magazanin gorseli silinemez (IDOR)")
    void deleteProductImage_WhenNotOwner_ShouldThrowUnauthorized() {
        UUID assetId = UUID.randomUUID();
        MediaAsset asset = MediaTestFixtures.activeAsset(assetId, productId, storeId, 0, false);
        when(queryPort.findById(assetId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> useCase.deleteProductImage(assetId, otherStoreId, false))
                .isInstanceOf(UnauthorizedMediaAccessException.class);

        verify(commandPort, never()).lockActiveByProductIdForUpdate(any());
    }

    @Test
    @DisplayName("U6: deleteProductImage - ADMIN sahiplik kontrolunden muaftir")
    void deleteProductImage_WhenAdmin_ShouldBypassOwnershipCheck() {
        UUID assetId = UUID.randomUUID();
        MediaAsset asset = MediaTestFixtures.activeAsset(assetId, productId, storeId, 0, false);
        when(queryPort.findById(assetId)).thenReturn(Optional.of(asset));
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(asset));

        useCase.deleteProductImage(assetId, otherStoreId, true);

        verify(commandPort).save(any());
    }

    @Test
    @DisplayName("U7: deleteProductImage - Zaten silinmis (ACTIVE olmayan) asset icin 404 davranisi")
    void deleteProductImage_WhenAlreadyDeleted_ShouldThrowNotFound() {
        UUID assetId = UUID.randomUUID();
        MediaAsset asset = MediaTestFixtures.deletedAsset(assetId, productId, storeId);
        when(queryPort.findById(assetId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> useCase.deleteProductImage(assetId, storeId, false))
                .isInstanceOf(MediaAssetNotFoundException.class);
    }

    @Test
    @DisplayName("U8: deleteProductImage - Primary silinirse kalanlardan sortOrder en kucuk olan yeni primary olur")
    void deleteProductImage_WhenPrimaryDeleted_ShouldHandOffToLowestSortOrder() {
        UUID assetId = UUID.randomUUID();
        MediaAsset target = MediaTestFixtures.activeAsset(assetId, productId, storeId, 0, true);
        MediaAsset remaining1 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 2, false);
        MediaAsset remaining2 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 1, false);

        when(queryPort.findById(assetId)).thenReturn(Optional.of(target));
        when(commandPort.lockActiveByProductIdForUpdate(productId))
                .thenReturn(List.of(target, remaining1, remaining2));

        useCase.deleteProductImage(assetId, storeId, false);

        ArgumentCaptor<MediaAsset> savedCaptor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(commandPort, times(2)).save(savedCaptor.capture());

        MediaAsset newPrimary = savedCaptor.getAllValues().get(1);
        assertThat(newPrimary.getId()).isEqualTo(remaining2.getId());
        assertThat(newPrimary.isPrimary()).isTrue();
        verify(cachePort).invalidate(productId);
    }

    @Test
    @DisplayName("U9: deleteProductImage - Primary degilse hicbir devir islemi yapilmaz (tek save)")
    void deleteProductImage_WhenNotPrimary_ShouldOnlySaveDeletedAsset() {
        UUID assetId = UUID.randomUUID();
        MediaAsset target = MediaTestFixtures.activeAsset(assetId, productId, storeId, 1, false);
        MediaAsset other = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);

        when(queryPort.findById(assetId)).thenReturn(Optional.of(target));
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(target, other));

        useCase.deleteProductImage(assetId, storeId, false);

        verify(commandPort, times(1)).save(any());
    }

    // ------------------------------------------------------------------
    // setPrimaryImage
    // ------------------------------------------------------------------

    @Test
    @DisplayName("U10: setPrimaryImage - Once digerleri temizlenir, SONRA hedef primary yapilir")
    void setPrimaryImage_ShouldClearThenSetTargetPrimary() {
        UUID assetId = UUID.randomUUID();
        MediaAsset before = MediaTestFixtures.activeAsset(assetId, productId, storeId, 1, false);
        MediaAsset afterClear = MediaTestFixtures.activeAsset(assetId, productId, storeId, 1, false);

        when(queryPort.findById(assetId)).thenReturn(Optional.of(before)).thenReturn(Optional.of(afterClear));
        when(commandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(queryPort.findActiveByProductIdOrderBySortOrder(productId)).thenReturn(List.of(afterClear));

        MediaAsset result = useCase.setPrimaryImage(assetId, storeId, false);

        var inOrder = org.mockito.Mockito.inOrder(commandPort);
        inOrder.verify(commandPort).clearPrimaryFlagForProduct(productId);
        inOrder.verify(commandPort).save(any());

        assertThat(result.isPrimary()).isTrue();
        verify(cachePort).invalidate(productId);
    }

    @Test
    @DisplayName("U11: setPrimaryImage - Baska magazanin gorseli icin IDOR reddi")
    void setPrimaryImage_WhenNotOwner_ShouldThrow() {
        UUID assetId = UUID.randomUUID();
        MediaAsset asset = MediaTestFixtures.activeAsset(assetId, productId, storeId, 0, false);
        when(queryPort.findById(assetId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> useCase.setPrimaryImage(assetId, otherStoreId, false))
                .isInstanceOf(UnauthorizedMediaAccessException.class);
    }

    // ------------------------------------------------------------------
    // reorderProductImages
    // ------------------------------------------------------------------

    @Test
    @DisplayName("U12: reorderProductImages - Bos urun icin InvalidReorderRequestException")
    void reorderProductImages_WhenNoActiveImages_ShouldThrow() {
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.reorderProductImages(productId, List.of(UUID.randomUUID()), storeId, false))
                .isInstanceOf(InvalidReorderRequestException.class);
    }

    @Test
    @DisplayName("U13: reorderProductImages - Listede tekrar eden id varsa reddedilir")
    void reorderProductImages_WhenDuplicateIds_ShouldThrow() {
        UUID id1 = UUID.randomUUID();
        MediaAsset asset1 = MediaTestFixtures.activeAsset(id1, productId, storeId, 0, true);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(asset1));

        assertThatThrownBy(() -> useCase.reorderProductImages(productId, List.of(id1, id1), storeId, false))
                .isInstanceOf(InvalidReorderRequestException.class);
    }

    @Test
    @DisplayName("U14: reorderProductImages - Liste urunun gercek gorselleriyle BIREBIR eslesmezse reddedilir")
    void reorderProductImages_WhenSetMismatch_ShouldThrow() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        MediaAsset asset1 = MediaTestFixtures.activeAsset(id1, productId, storeId, 0, true);
        MediaAsset asset2 = MediaTestFixtures.activeAsset(id2, productId, storeId, 1, false);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(asset1, asset2));

        assertThatThrownBy(() -> useCase.reorderProductImages(productId, List.of(id1), storeId, false))
                .isInstanceOf(InvalidReorderRequestException.class);
    }

    @Test
    @DisplayName("U15: reorderProductImages - Basarili siralama: sortOrder verilen sirayla 0..n-1 atanir")
    void reorderProductImages_WhenValid_ShouldAssignSequentialSortOrder() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        MediaAsset asset1 = MediaTestFixtures.activeAsset(id1, productId, storeId, 0, true);
        MediaAsset asset2 = MediaTestFixtures.activeAsset(id2, productId, storeId, 1, false);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(asset1, asset2));
        when(commandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MediaAsset> result = useCase.reorderProductImages(productId, List.of(id2, id1), storeId, false);

        assertThat(result).extracting(MediaAsset::getId).containsExactly(id2, id1);
        assertThat(result.get(0).getSortOrder()).isEqualTo(0);
        assertThat(result.get(1).getSortOrder()).isEqualTo(1);
        verify(cachePort).invalidate(productId);
    }

    @Test
    @DisplayName("U16: reorderProductImages - Baska magazanin gorseli listede varsa IDOR reddi")
    void reorderProductImages_WhenContainsForeignAsset_ShouldThrowUnauthorized() {
        UUID id1 = UUID.randomUUID();
        MediaAsset foreignAsset = MediaTestFixtures.activeAsset(id1, productId, otherStoreId, 0, true);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(foreignAsset));

        assertThatThrownBy(() -> useCase.reorderProductImages(productId, List.of(id1), storeId, false))
                .isInstanceOf(UnauthorizedMediaAccessException.class);
    }

    // ------------------------------------------------------------------
    // softDeleteAllByProduct
    // ------------------------------------------------------------------

    @Test
    @DisplayName("U17: softDeleteAllByProduct - ACTIVE gorsel yoksa hicbir sey yapmaz")
    void softDeleteAllByProduct_WhenNoActiveImages_ShouldNoOp() {
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of());

        useCase.softDeleteAllByProduct(productId);

        verify(commandPort, never()).save(any());
        verifyNoInteractions(eventFactory, cachePort);
    }

    @Test
    @DisplayName("U18: softDeleteAllByProduct - Tum ACTIVE gorseller soft delete edilir ve cache invalidate olur")
    void softDeleteAllByProduct_WhenActiveImagesExist_ShouldDeleteAllAndInvalidateCache() {
        MediaAsset a1 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        MediaAsset a2 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 1, false);
        when(commandPort.lockActiveByProductIdForUpdate(productId)).thenReturn(List.of(a1, a2));

        useCase.softDeleteAllByProduct(productId);

        verify(commandPort, times(2)).save(any());
        assertThat(a1.getStatus()).isEqualTo(com.mediaservice.domain.model.MediaStatus.DELETED);
        assertThat(a2.getStatus()).isEqualTo(com.mediaservice.domain.model.MediaStatus.DELETED);
        verify(eventFactory).append(eq(MediaEventFactory.RK_DELETED), eq(MediaEventFactory.EVENT_DELETED),
                eq(productId), eq(productId), eq(List.of()));
        verify(cachePort).invalidate(productId);
    }
}
