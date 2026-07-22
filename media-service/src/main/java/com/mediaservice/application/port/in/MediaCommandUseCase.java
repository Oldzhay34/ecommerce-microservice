package com.mediaservice.application.port.in;

import com.mediaservice.domain.model.MediaAsset;

import java.util.List;
import java.util.UUID;

public interface MediaCommandUseCase {

    MediaAsset uploadProductImage(UUID productId,
                                  UUID actorUserId,
                                  boolean actorIsAdmin,
                                  String declaredContentType,
                                  byte[] fileBytes);

    void deleteProductImage(UUID assetId, UUID actorUserId, boolean actorIsAdmin);

    MediaAsset setPrimaryImage(UUID assetId, UUID actorUserId, boolean actorIsAdmin);

    List<MediaAsset> reorderProductImages(UUID productId,
                                          List<UUID> assetIds,
                                          UUID actorUserId,
                                          boolean actorIsAdmin);

    void softDeleteAllByProduct(UUID productId);
}