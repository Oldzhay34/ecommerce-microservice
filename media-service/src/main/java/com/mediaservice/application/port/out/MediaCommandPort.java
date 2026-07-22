package com.mediaservice.application.port.out;

import com.mediaservice.domain.model.MediaAsset;

import java.util.List;
import java.util.UUID;

public interface MediaCommandPort {

    MediaAsset save(MediaAsset asset);

    List<MediaAsset> saveAll(List<MediaAsset> assets);

    /**
     * [Karar C] Urun bazli pessimistic lock: ayni product_id uzerindeki tum ACTIVE satirlari
     * PESSIMISTIC_WRITE ile kilitler. SetPrimary/Reorder/Delete yarislarini deadlock uretmeden
     * serilestirir (tek ve tutarli siralama: sort_order ASC, id ASC).
     */
    List<MediaAsset> lockActiveByProductIdForUpdate(UUID productId);

    void clearPrimaryFlagForProduct(UUID productId);

    long countActiveByProductId(UUID productId);
}