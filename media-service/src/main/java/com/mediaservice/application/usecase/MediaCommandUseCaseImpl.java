package com.mediaservice.application.usecase;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.application.port.out.ImageConverterPort;
import com.mediaservice.application.port.out.MediaCachePort;
import com.mediaservice.application.port.out.MediaCommandPort;
import com.mediaservice.application.port.out.MediaQueryPort;
import com.mediaservice.application.port.out.StoragePort;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.exception.MediaAssetNotFoundException;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.domain.model.MediaVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Yazma tarafi use case'leri.
 *
 * Depolama saglayicisi (MinIO / local hostPath / AWS S3) BU SINIFI ILGILENDIRMEZ - yalnizca
 * StoragePort gorulur. Saglayici degisimi tek bir adapter degisikligidir; bu dosya degismez.
 *
 * Concurrency [Karar C]: mutasyon yapan her akis once
 * MediaCommandPort.lockActiveByProductIdForUpdate(productId) ile urun bazli PESSIMISTIC_WRITE
 * lock alir. "Urun basina tek primary" invaryanti uq_media_asset_primary kismi indeksiyle DB
 * seviyesinde garanti edilir; @Version farkli satirlar arasindaki yarisi yakalayamayacagi icin
 * lock zorunludur.
 *
 * Cache [Karar A/B]: cache-aside. Once PostgreSQL commit, SONRA invalidate. Redis'te yalnizca
 * metadata JSON tutulur; WebP byte[] Redis'e yazilmaz.
 */
@Service
public class MediaCommandUseCaseImpl implements MediaCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(MediaCommandUseCaseImpl.class);

    private final MediaCommandPort commandPort;
    private final MediaQueryPort queryPort;
    private final MediaCachePort cachePort;
    private final StoragePort storagePort;
    private final ImageConverterPort imageConverterPort;
    private final MediaEventFactory eventFactory;
    private final MediaUploadTransactionalDelegate uploadDelegate;
    private final int maxImagesPerProduct;

    public MediaCommandUseCaseImpl(MediaCommandPort commandPort,
                                   MediaQueryPort queryPort,
                                   MediaCachePort cachePort,
                                   StoragePort storagePort,
                                   ImageConverterPort imageConverterPort,
                                   MediaEventFactory eventFactory,
                                   MediaUploadTransactionalDelegate uploadDelegate,
                                   @Value("${media.max-images-per-product}") int maxImagesPerProduct) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.cachePort = cachePort;
        this.storagePort = storagePort;
        this.imageConverterPort = imageConverterPort;
        this.eventFactory = eventFactory;
        this.uploadDelegate = uploadDelegate;
        this.maxImagesPerProduct = maxImagesPerProduct;
    }

    // ------------------------------------------------------------------
    // 1) UploadProductImage - ROLE_STORE
    // ------------------------------------------------------------------

    /**
     * Sira: Content-Type dogrula -> magic byte dogrula -> limit on kontrolu -> 3 varyant WebP
     * (paralel) -> storage'a yaz -> media_asset + outbox_event ayni transaction'da (delegate).
     *
     * Donusum ve upload TRANSACTION DISINDA yapilir. Storage'a yazilip DB'ye yazilamayan nesne
     * yetim kalir (dev'de kabul; fiziksel temizlik ayri bir job'in isidir - [Varsayim 9]).
     */
    @Override
    public MediaAsset uploadProductImage(UUID productId,
                                         UUID actorUserId,
                                         boolean actorIsAdmin,
                                         String declaredContentType,
                                         byte[] fileBytes) {

        // 415: Content-Type kabul listesi disi VEYA magic byte uyusmazligi.
        // Uzantiya/header'a guvenilmez -> UnsupportedMediaFormatException.
        imageConverterPort.validateFormat(declaredContentType, fileBytes);

        // Hizli on kontrol (transaction disi, gereksiz donusumu erken keser).
        // Kesin kontrol delegate icinde lock altinda TEKRARLANIR.
        if (commandPort.countActiveByProductId(productId) >= maxImagesPerProduct) {
            throw new MediaLimitExceededException(
                    "Bu urun icin maksimum " + maxImagesPerProduct + " gorsel yuklenebilir.");
        }

        UUID assetId = UUID.randomUUID();

        // CPU-yogun donusum: 3 varyant paralel, hepsi WebP.
        // Orijinal PNG/JPEG hicbir yere yazilmaz.
        Map<MediaVariant, ImageBinary> variants = imageConverterPort.convertToWebpVariants(fileBytes);

        Map<MediaVariant, String> storageKeys = new HashMap<>();
        Map<MediaVariant, String> publicUrls = new HashMap<>();
        for (MediaVariant variant : MediaVariant.values()) {
            String storageKey = buildStorageKey(productId, assetId, variant);
            // StoragePort: MinIO / local / S3 - use case saglayiciyi BILMEZ.
            String publicUrl = storagePort.upload(storageKey, variants.get(variant));
            storageKeys.put(variant, storageKey);
            publicUrls.put(variant, publicUrl);
        }

        MediaAsset saved = uploadDelegate.persist(
                productId, actorUserId, assetId, variants, storageKeys, publicUrls);

        // Delegate'in transaction'i commit oldu; cache-aside geregi simdi invalidate edilir.
        cachePort.invalidate(productId);

        log.info("Gorsel yuklendi. productId={}, assetId={}, sortOrder={}, primary={}",
                productId, saved.getId(), saved.getSortOrder(), saved.isPrimary());
        return saved;
    }

    // ------------------------------------------------------------------
    // 2) DeleteProductImage - ROLE_STORE (kendi) / ROLE_ADMIN
    // ------------------------------------------------------------------

    /**
     * Soft delete (status = DELETED). Fiziksel silme YAPILMAZ.
     * Silinen gorsel primary ise, kalan ACTIVE gorsellerden sortOrder en kucuk olan primary olur.
     * Hic kalmadiysa urunun primary gorseli yoktur.
     * [Karar 3] Silinen satirin is_primary bayragi da false yapilir (MediaAsset.markDeleted).
     * [Varsayim 9] sortOrder yeniden numaralandirilmaz - bosluk kalir.
     */
    @Override
    @Transactional
    public void deleteProductImage(UUID assetId, UUID actorUserId, boolean actorIsAdmin) {
        MediaAsset asset = queryPort.findById(assetId)
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));

        assertOwnership(asset, actorUserId, actorIsAdmin);

        if (!asset.isActive()) {
            throw new MediaAssetNotFoundException(assetId);
        }

        UUID productId = asset.getProductId();
        List<MediaAsset> locked = commandPort.lockActiveByProductIdForUpdate(productId);

        boolean wasPrimary = asset.isPrimary();
        asset.markDeleted();
        commandPort.save(asset);
        if (wasPrimary) {
            // Devirden ONCE yazdirilmali: uq_media_asset_primary ertelenemez bir kismi
            // UNIQUE indekstir, iki UPDATE tek batch'te ters sirada giderse ihlal edilir
            // (bkz. MediaCommandPort.flush).
            commandPort.flush();
        }

        List<MediaAsset> remaining = new ArrayList<>();
        for (MediaAsset candidate : locked) {
            if (!candidate.getId().equals(assetId)) {
                remaining.add(candidate);
            }
        }
        remaining.sort((a, b) -> {
            int cmp = Integer.compare(a.getSortOrder(), b.getSortOrder());
            return cmp != 0 ? cmp : a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        if (wasPrimary && !remaining.isEmpty()) {
            MediaAsset newPrimary = remaining.get(0);
            newPrimary.setPrimary(true);
            commandPort.save(newPrimary);
            log.info("Primary devredildi. productId={}, yeniPrimary={}", productId, newPrimary.getId());
        }

        eventFactory.append(MediaEventFactory.RK_DELETED, MediaEventFactory.EVENT_DELETED,
                productId, assetId, remaining);
        invalidateAfterCommit(productId);

        log.info("Gorsel soft delete edildi. productId={}, assetId={}, wasPrimary={}",
                productId, assetId, wasPrimary);
    }

    // ------------------------------------------------------------------
    // 3) SetPrimaryImage - ROLE_STORE (kendi) / ROLE_ADMIN
    // ------------------------------------------------------------------

    /**
     * SIRA ONEMLI: once ayni urunun tum asset'leri primary = false yapilir (flush),
     * SONRA hedef true yapilir. Aksi halde uq_media_asset_primary kismi unique indeksi ihlal edilir.
     */
    @Override
    @Transactional
    public MediaAsset setPrimaryImage(UUID assetId, UUID actorUserId, boolean actorIsAdmin) {
        MediaAsset asset = queryPort.findById(assetId)
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));

        assertOwnership(asset, actorUserId, actorIsAdmin);

        if (!asset.isActive()) {
            throw new MediaAssetNotFoundException(assetId);
        }

        UUID productId = asset.getProductId();
        commandPort.lockActiveByProductIdForUpdate(productId);

        // 1. adim: digerlerini temizle (clearAutomatically + flushAutomatically ile DB'ye yansir)
        commandPort.clearPrimaryFlagForProduct(productId);

        // 2. adim: hedefi primary yap (persistence context temizlendigi icin yeniden okunur)
        MediaAsset target = queryPort.findById(assetId)
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));
        target.setPrimary(true);
        MediaAsset saved = commandPort.save(target);

        List<MediaAsset> current = queryPort.findActiveByProductIdOrderBySortOrder(productId);
        eventFactory.append(MediaEventFactory.RK_UPDATED, MediaEventFactory.EVENT_UPDATED,
                productId, saved.getId(), current);
        invalidateAfterCommit(productId);

        log.info("Primary ayarlandi. productId={}, assetId={}", productId, assetId);
        return saved;
    }

    // ------------------------------------------------------------------
    // 4) ReorderProductImages - ROLE_STORE (kendi) / ROLE_ADMIN
    // ------------------------------------------------------------------

    /**
     * Verilen assetId listesi sirasina gore sortOrder 0..n-1 atanir.
     * Liste urunun ACTIVE gorselleriyle BIREBIR eslesmelidir; eksik/fazla/tekrar -> 400.
     */
    @Override
    @Transactional
    public List<MediaAsset> reorderProductImages(UUID productId,
                                                 List<UUID> assetIds,
                                                 UUID actorUserId,
                                                 boolean actorIsAdmin) {

        List<MediaAsset> locked = commandPort.lockActiveByProductIdForUpdate(productId);

        if (locked.isEmpty()) {
            throw new InvalidReorderRequestException("Siralama listesi urunun gorselleriyle eslesmiyor.");
        }

        for (MediaAsset asset : locked) {
            assertOwnership(asset, actorUserId, actorIsAdmin);
        }

        LinkedHashSet<UUID> requested = new LinkedHashSet<>(assetIds);
        if (requested.size() != assetIds.size()) {
            throw new InvalidReorderRequestException("Siralama listesi urunun gorselleriyle eslesmiyor.");
        }

        Map<UUID, MediaAsset> byId = new HashMap<>();
        for (MediaAsset asset : locked) {
            byId.put(asset.getId(), asset);
        }

        if (requested.size() != byId.size() || !byId.keySet().containsAll(requested)) {
            throw new InvalidReorderRequestException("Siralama listesi urunun gorselleriyle eslesmiyor.");
        }

        List<MediaAsset> reordered = new ArrayList<>(requested.size());
        int index = 0;
        for (UUID id : requested) {
            MediaAsset asset = byId.get(id);
            asset.setSortOrder(index++);
            reordered.add(commandPort.save(asset));
        }

        UUID primaryId = resolvePrimaryId(reordered);
        eventFactory.append(MediaEventFactory.RK_UPDATED, MediaEventFactory.EVENT_UPDATED,
                productId, primaryId, reordered);
        invalidateAfterCommit(productId);

        log.info("Gorseller yeniden siralandi. productId={}, adet={}", productId, reordered.size());
        return reordered;
    }

    // ------------------------------------------------------------------
    // 5) Product silindiginde toplu soft delete (ProductDeletedEventListener icin)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void softDeleteAllByProduct(UUID productId) {
        List<MediaAsset> locked = commandPort.lockActiveByProductIdForUpdate(productId);
        if (locked.isEmpty()) {
            log.debug("Silinecek ACTIVE gorsel yok. productId={}", productId);
            return;
        }
        for (MediaAsset asset : locked) {
            asset.markDeleted();
            commandPort.save(asset);
        }
        eventFactory.append(MediaEventFactory.RK_DELETED, MediaEventFactory.EVENT_DELETED,
                productId, productId, List.of());
        invalidateAfterCommit(productId);
        log.info("Urun silindi, gorseller soft delete edildi. productId={}, adet={}",
                productId, locked.size());
    }

    // ------------------------------------------------------------------
    // Yardimcilar
    // ------------------------------------------------------------------

    /**
     * IDOR korumasi: JWT'den cikarilan actorUserId ile asset.storeId karsilastirilir.
     * Request body veya path'ten storeId KABUL EDILMEZ. ROLE_ADMIN bu kisittan muaftir.
     * [Karar E] Sistemde ayri bir storeId attribute'u yoktur; ROLE_STORE tasiyan kullanicinin
     * userId'si magaza kimligidir.
     */
    private void assertOwnership(MediaAsset asset, UUID actorUserId, boolean actorIsAdmin) {
        if (actorIsAdmin) {
            return;
        }
        if (actorUserId == null || !asset.isOwnedBy(actorUserId)) {
            throw new UnauthorizedMediaAccessException("Bu gorsel uzerinde islem yetkiniz yok.");
        }
    }

    private String buildStorageKey(UUID productId, UUID assetId, MediaVariant variant) {
        return "products/" + productId + "/" + assetId + "_" + variant.getSuffix() + ".webp";
    }

    private UUID resolvePrimaryId(List<MediaAsset> assets) {
        for (MediaAsset asset : assets) {
            if (asset.isPrimary()) {
                return asset.getId();
            }
        }
        return assets.isEmpty() ? null : assets.get(0).getId();
    }

    /**
     * Cache-aside: write-through YAPILMAZ. Invalidate yalnizca PostgreSQL commit BASARILI
     * olduktan sonra calisir; rollback durumunda cache'e dokunulmaz.
     */
    private void invalidateAfterCommit(UUID productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cachePort.invalidate(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cachePort.invalidate(productId);
            }
        });
    }
}