package com.mediaservice.application.port.out;

import com.mediaservice.domain.model.MediaAsset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaQueryPort {

    Optional<MediaAsset> findById(UUID assetId);

    List<MediaAsset> findActiveByProductIdOrderBySortOrder(UUID productId);

    List<MediaAsset> findActiveByProductIdsOrderBySortOrder(List<UUID> productIds);
}