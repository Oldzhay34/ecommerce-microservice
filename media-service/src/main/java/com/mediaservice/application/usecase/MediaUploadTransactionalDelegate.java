package com.mediaservice.application.usecase;

import com.mediaservice.application.port.out.MediaCommandPort;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.domain.model.MediaStatus;
import com.mediaservice.domain.model.MediaVariant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Upload akisinin YALNIZCA transactional kismi: media_asset satiri + outbox_event yazimi.
 *
 * Neden ayri sinif: @Transactional Spring AOP proxy uzerinden calisir; ayni sinif icinden
 * yapilan cagri (self-invocation) proxy'yi ATLAR ve transaction ACILMAZ. O durumda
 * OutboxAdapter'in Propagation.MANDATORY'si patlardi ve media_asset ile outbox_event yazimi
 * atomik olmazdi. Delegasyon bunu garanti eder.
 *
 * Donusum + storage upload BU SINIFA GIRMEDEN once tamamlanir; transaction yalnizca DB
 * yazimini kapsar, CPU-yogun is ve ag I/O boyunca DB baglantisi tutulmaz.
 *
 * ONEMLI: outbox yazimi persist() ile AYNI transaction icindedir (Transactional Outbox Pattern).
 * Ayri transaction'a alinsaydi DB commit olur, outbox yazilamazsa event sessizce kaybolurdu.
 */
@Component
public class MediaUploadTransactionalDelegate {

    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final MediaCommandPort commandPort;
    private final MediaEventFactory eventFactory;
    private final int maxImagesPerProduct;

    public MediaUploadTransactionalDelegate(MediaCommandPort commandPort,
                                            MediaEventFactory eventFactory,
                                            @Value("${media.max-images-per-product}") int maxImagesPerProduct) {
        this.commandPort = commandPort;
        this.eventFactory = eventFactory;
        this.maxImagesPerProduct = maxImagesPerProduct;
    }

    /**
     * Urun bazli PESSIMISTIC_WRITE lock altinda:
     *  - limit kesin kontrolu (transaction disi on kontrol yarisi kapatilir)
     *  - sortOrder tahsisi
     *  - ilk gorselse primary = true, sortOrder = 0
     *  - media_asset + outbox_event ayni transaction'da
     */
    @Transactional
    public MediaAsset persist(UUID productId,
                              UUID actorUserId,
                              UUID assetId,
                              Map<MediaVariant, ImageBinary> variants,
                              Map<MediaVariant, String> storageKeys,
                              Map<MediaVariant, String> publicUrls) {

        List<MediaAsset> existing = commandPort.lockActiveByProductIdForUpdate(productId);

        if (existing.size() >= maxImagesPerProduct) {
            throw new MediaLimitExceededException(
                    "Bu urun icin maksimum " + maxImagesPerProduct + " gorsel yuklenebilir.");
        }

        boolean isFirstImage = existing.isEmpty();
        int nextSortOrder = isFirstImage ? 0 : maxSortOrder(existing) + 1;

        ImageBinary medium = variants.get(MediaVariant.MEDIUM);

        MediaAsset asset = new MediaAsset(
                assetId,
                productId,
                actorUserId,
                publicUrls.get(MediaVariant.MEDIUM),
                publicUrls.get(MediaVariant.THUMB),
                publicUrls.get(MediaVariant.ORIGINAL),
                storageKeys.get(MediaVariant.MEDIUM),
                storageKeys.get(MediaVariant.THUMB),
                storageKeys.get(MediaVariant.MEDIUM),
                storageKeys.get(MediaVariant.ORIGINAL),
                WEBP_CONTENT_TYPE,
                medium.getSizeBytes(),
                medium.getWidth(),
                medium.getHeight(),
                nextSortOrder,
                isFirstImage,
                MediaStatus.ACTIVE,
                Instant.now(),
                null
        );
        asset.validateWebpInvariant();

        MediaAsset saved = commandPort.save(asset);

        List<MediaAsset> afterUpload = new ArrayList<>(existing);
        afterUpload.add(saved);

        eventFactory.append(MediaEventFactory.RK_UPLOADED, MediaEventFactory.EVENT_UPLOADED,
                productId, saved.getId(), afterUpload);

        return saved;
    }

    private int maxSortOrder(List<MediaAsset> assets) {
        int max = -1;
        for (MediaAsset asset : assets) {
            max = Math.max(max, asset.getSortOrder());
        }
        return max;
    }
}