package com.mediaservice.application.usecase;

import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.application.port.in.MediaQueryUseCase;
import com.mediaservice.application.port.out.MediaCachePort;
import com.mediaservice.application.port.out.MediaQueryPort;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.model.MediaAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Okuma tarafi. Elasticsearch KULLANILMAZ [Varsayim 5]: media sorgulari her zaman productId ile
 * exact match yapilir; full-text/faceted arama yoktur. Redis + idx_media_asset_product_status_sort
 * yeterlidir.
 */
@Service
public class MediaQueryUseCaseImpl implements MediaQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(MediaQueryUseCaseImpl.class);
    private static final int MAX_BATCH_SIZE = 100;

    private final MediaQueryPort queryPort;
    private final MediaCachePort cachePort;

    public MediaQueryUseCaseImpl(MediaQueryPort queryPort, MediaCachePort cachePort) {
        this.queryPort = queryPort;
        this.cachePort = cachePort;
    }

    /**
     * Redis -> miss ise PostgreSQL -> Redis'e doldur -> doner.
     * Gorseli olmayan urun icin BOS DIZI doner, 404 DONDURULMEZ.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getProductMedia(UUID productId) {
        List<MediaAssetResponse> cached = cachePort.get(productId);
        if (cached != null) {
            return cached;
        }
        List<MediaAsset> assets = queryPort.findActiveByProductIdOrderBySortOrder(productId);
        List<MediaAssetResponse> response = toResponseList(assets);
        cachePort.put(productId, response);
        return response;
    }

    /**
     * Redis multiGet -> miss olanlar TEK sorguda PostgreSQL'den -> Redis'e yazilir.
     * Sorgulanan ama gorseli olmayan productId icin bos dizi doner (anahtar yine bulunur).
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<MediaAssetResponse>> getProductMediaBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        if (productIds.size() > MAX_BATCH_SIZE) {
            throw new InvalidReorderRequestException("En fazla " + MAX_BATCH_SIZE + " urun sorgulanabilir.");
        }

        List<UUID> distinctIds = new ArrayList<>(new LinkedHashSet<>(productIds));

        Map<UUID, List<MediaAssetResponse>> cached = cachePort.multiGet(distinctIds);

        List<UUID> missing = new ArrayList<>();
        for (UUID id : distinctIds) {
            if (!cached.containsKey(id)) {
                missing.add(id);
            }
        }

        Map<UUID, List<MediaAssetResponse>> fetched = new HashMap<>();
        if (!missing.isEmpty()) {
            // N+1 yok: tek sorgu, IN (...) ile.
            List<MediaAsset> assets = queryPort.findActiveByProductIdsOrderBySortOrder(missing);

            Map<UUID, List<MediaAsset>> grouped = new HashMap<>();
            for (UUID id : missing) {
                grouped.put(id, new ArrayList<>());
            }
            for (MediaAsset asset : assets) {
                grouped.get(asset.getProductId()).add(asset);
            }
            for (Map.Entry<UUID, List<MediaAsset>> entry : grouped.entrySet()) {
                fetched.put(entry.getKey(), toResponseList(entry.getValue()));
            }
            cachePort.putAll(fetched);
            log.debug("Batch cache miss. istenen={}, miss={}", distinctIds.size(), missing.size());
        }

        Map<String, List<MediaAssetResponse>> result = new LinkedHashMap<>();
        for (UUID id : distinctIds) {
            List<MediaAssetResponse> value = cached.containsKey(id) ? cached.get(id) : fetched.get(id);
            result.put(id.toString(), value == null ? List.of() : value);
        }
        return result;
    }

    private List<MediaAssetResponse> toResponseList(List<MediaAsset> assets) {
        List<MediaAssetResponse> result = new ArrayList<>(assets.size());
        for (MediaAsset asset : assets) {
            result.add(toResponse(asset));
        }
        return result;
    }

    static MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getProductId(),
                asset.getUrl(),
                asset.getThumbUrl(),
                asset.getOriginalUrl(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getSortOrder(),
                asset.isPrimary(),
                asset.getCreatedAt()
        );
    }
}