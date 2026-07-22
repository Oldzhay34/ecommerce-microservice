package com.mediaservice.application.port.in;

import com.mediaservice.api.dto.MediaAssetResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MediaQueryUseCase {

    List<MediaAssetResponse> getProductMedia(UUID productId);

    Map<String, List<MediaAssetResponse>> getProductMediaBatch(List<UUID> productIds);
}