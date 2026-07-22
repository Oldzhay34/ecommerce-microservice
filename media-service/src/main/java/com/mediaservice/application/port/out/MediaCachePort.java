package com.mediaservice.application.port.out;

import com.mediaservice.api.dto.MediaAssetResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MediaCachePort {

    List<MediaAssetResponse> get(UUID productId);

    void put(UUID productId, List<MediaAssetResponse> items);

    Map<UUID, List<MediaAssetResponse>> multiGet(List<UUID> productIds);

    void putAll(Map<UUID, List<MediaAssetResponse>> entries);

    void invalidate(UUID productId);
}