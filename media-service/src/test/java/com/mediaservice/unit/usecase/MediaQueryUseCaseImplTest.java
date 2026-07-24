package com.mediaservice.unit.usecase;

import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.application.port.out.MediaCachePort;
import com.mediaservice.application.port.out.MediaQueryPort;
import com.mediaservice.application.usecase.MediaQueryUseCaseImpl;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - okuma tarafi use case'i, cache-aside davranisi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaQueryUseCaseImpl")
class MediaQueryUseCaseImplTest {

    @Mock private MediaQueryPort queryPort;
    @Mock private MediaCachePort cachePort;

    private MediaQueryUseCaseImpl useCase;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        useCase = new MediaQueryUseCaseImpl(queryPort, cachePort);
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U1: getProductMedia - Cache hit ise Postgres'e HIC gidilmez")
    void getProductMedia_WhenCacheHit_ShouldNotQueryDatabase() {
        List<MediaAssetResponse> cached = List.of();
        when(cachePort.get(productId)).thenReturn(cached);

        List<MediaAssetResponse> result = useCase.getProductMedia(productId);

        assertThat(result).isSameAs(cached);
        verify(queryPort, never()).findActiveByProductIdOrderBySortOrder(any());
    }

    @Test
    @DisplayName("U2: getProductMedia - Cache miss ise Postgres'ten okunur ve Redis'e doldurulur")
    void getProductMedia_WhenCacheMiss_ShouldQueryDatabaseAndPopulateCache() {
        when(cachePort.get(productId)).thenReturn(null);
        MediaAsset asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        when(queryPort.findActiveByProductIdOrderBySortOrder(productId)).thenReturn(List.of(asset));

        List<MediaAssetResponse> result = useCase.getProductMedia(productId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssetId()).isEqualTo(asset.getId());
        verify(cachePort).put(eq(productId), any());
    }

    @Test
    @DisplayName("U3: getProductMedia - Gorseli olmayan urun icin bos dizi doner (404 DEGIL)")
    void getProductMedia_WhenNoAssets_ShouldReturnEmptyList() {
        when(cachePort.get(productId)).thenReturn(null);
        when(queryPort.findActiveByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

        List<MediaAssetResponse> result = useCase.getProductMedia(productId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("U4: getProductMediaBatch - Bos/null liste icin sorgusuz Map.of() doner")
    void getProductMediaBatch_WhenEmptyOrNull_ShouldReturnEmptyMap() {
        assertThat(useCase.getProductMediaBatch(List.of())).isEmpty();
        assertThat(useCase.getProductMediaBatch(null)).isEmpty();
        verify(cachePort, never()).multiGet(any());
    }

    @Test
    @DisplayName("U5: getProductMediaBatch - 100'den fazla id istenirse InvalidReorderRequestException")
    void getProductMediaBatch_WhenOverLimit_ShouldThrow() {
        List<UUID> tooMany = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());

        assertThatThrownBy(() -> useCase.getProductMediaBatch(tooMany))
                .isInstanceOf(InvalidReorderRequestException.class);
    }

    @Test
    @DisplayName("U6: getProductMediaBatch - Tekrar eden productId'ler tek sorguya indirgenir (dedup)")
    void getProductMediaBatch_ShouldDeduplicateRequestedIds() {
        when(cachePort.multiGet(any())).thenReturn(Map.of());
        when(queryPort.findActiveByProductIdsOrderBySortOrder(any())).thenReturn(List.of());

        useCase.getProductMediaBatch(List.of(productId, productId, productId));

        org.mockito.ArgumentCaptor<List<UUID>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(cachePort).multiGet(captor.capture());
        assertThat(captor.getValue()).containsExactly(productId);
    }

    @Test
    @DisplayName("U7: getProductMediaBatch - Cache'te olmayanlar TEK sorguda (IN) DB'den cekilir ve Redis'e yazilir")
    void getProductMediaBatch_WhenPartialCacheMiss_ShouldFetchMissingWithSingleQuery() {
        UUID cachedProductId = UUID.randomUUID();
        UUID missingProductId = UUID.randomUUID();
        List<MediaAssetResponse> cachedList = List.of();
        when(cachePort.multiGet(any())).thenReturn(Map.of(cachedProductId, cachedList));

        MediaAsset assetForMissing = MediaTestFixtures.activeAsset(UUID.randomUUID(), missingProductId, storeId, 0, true);
        when(queryPort.findActiveByProductIdsOrderBySortOrder(List.of(missingProductId)))
                .thenReturn(List.of(assetForMissing));

        Map<String, List<MediaAssetResponse>> result =
                useCase.getProductMediaBatch(new ArrayList<>(List.of(cachedProductId, missingProductId)));

        assertThat(result).containsKeys(cachedProductId.toString(), missingProductId.toString());
        assertThat(result.get(missingProductId.toString())).hasSize(1);
        verify(cachePort).putAll(any());
    }

    @Test
    @DisplayName("U8: getProductMediaBatch - Sorgulanan ama gorseli olmayan urun icin bos dizi (anahtar yine bulunur)")
    void getProductMediaBatch_WhenProductHasNoAssets_ShouldStillIncludeEmptyList() {
        when(cachePort.multiGet(any())).thenReturn(Map.of());
        when(queryPort.findActiveByProductIdsOrderBySortOrder(any())).thenReturn(List.of());

        Map<String, List<MediaAssetResponse>> result = useCase.getProductMediaBatch(List.of(productId));

        assertThat(result).containsKey(productId.toString());
        assertThat(result.get(productId.toString())).isEmpty();
    }
}
