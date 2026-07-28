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

    /**
     * Bekleyen degisiklikleri DB'ye YAZDIRIR (transaction'i commit ETMEZ).
     * <p>
     * uq_media_asset_primary, (product_id) uzerinde "WHERE is_primary AND status='ACTIVE'"
     * kismi UNIQUE indeksidir ve ertelenemez (deferrable degil): her UPDATE'ten sonra ayri
     * ayri kontrol edilir. Iki satirin primary bayragi ayni transaction'da yer degistirdiginde
     * (bkz. deleteProductImage'daki devir), Hibernate bu UPDATE'leri commit aninda TEK batch'te
     * ve KENDI sectigi sirada calistirir; promosyon UPDATE'i once giderse urunun kisa sureligine
     * iki ACTIVE primary'si varmis gibi gorunur ve indeks ihlal edilir. Bu metot, use case'in
     * zaten ifade ettigi sirayi SQL'e tasimak icin vardir.
     */
    void flush();

    long countActiveByProductId(UUID productId);
}