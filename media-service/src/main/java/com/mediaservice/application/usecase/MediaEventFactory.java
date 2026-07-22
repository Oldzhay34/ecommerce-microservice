package com.mediaservice.application.usecase;

import com.mediaservice.application.port.out.OutboxPort;
import com.mediaservice.domain.model.MediaAsset;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox event payload'inin TEK kaynagi.
 *
 * Neden ayri sinif: upload akisi delegate icinde, diger akislar use case icinde transaction
 * aciyor. Event olusturma kodu iki yere kopyalanirsa sema zamanla sessizce ikiye ayrilir.
 * Burasi tek nokta.
 *
 * Sozlesme (SABIT):
 *  - primaryUrl / primaryThumbUrl: HER ZAMAN islem SONRASI gecerli primary gorselin URL'i,
 *    yoksa null. Boylece Product servisi tek alan okuyup ES'e yazar, ek sorgu yapmaz.
 *  - imageCount [Karar 4]: yuklenen varyant sayisi DEGIL, urunun islem sonrasi toplam
 *    ACTIVE asset sayisi.
 */
@Component
public class MediaEventFactory {

    public static final String AGGREGATE_TYPE = "MediaAsset";

    public static final String EVENT_UPLOADED = "MediaUploadedEvent";
    public static final String EVENT_DELETED = "MediaDeletedEvent";
    public static final String EVENT_UPDATED = "MediaUpdatedEvent";

    public static final String RK_UPLOADED = "media.uploaded";
    public static final String RK_DELETED = "media.deleted";
    public static final String RK_UPDATED = "media.updated";

    private final OutboxPort outboxPort;

    public MediaEventFactory(OutboxPort outboxPort) {
        this.outboxPort = outboxPort;
    }

    /**
     * Cagiran akisin transaction'i ZORUNLUDUR (OutboxPort.append -> Propagation.MANDATORY).
     */
    public void append(String routingKey,
                       String eventType,
                       UUID productId,
                       UUID assetId,
                       List<MediaAsset> assetsAfterOperation) {

        MediaAsset primary = resolvePrimary(assetsAfterOperation);

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("occurredAt", Instant.now().toString());
        payload.put("productId", productId.toString());
        payload.put("assetId", assetId == null ? null : assetId.toString());
        payload.put("primaryUrl", primary == null ? null : primary.getUrl());
        payload.put("primaryThumbUrl", primary == null ? null : primary.getThumbUrl());
        payload.put("imageCount", countActive(assetsAfterOperation));

        outboxPort.append(AGGREGATE_TYPE, productId, eventType, routingKey, payload);
    }

    private MediaAsset resolvePrimary(List<MediaAsset> assets) {
        for (MediaAsset asset : assets) {
            if (asset.isActive() && asset.isPrimary()) {
                return asset;
            }
        }
        return null;
    }

    private int countActive(List<MediaAsset> assets) {
        int count = 0;
        for (MediaAsset asset : assets) {
            if (asset.isActive()) {
                count++;
            }
        }
        return count;
    }
}