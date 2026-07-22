package com.mediaservice.infrastructure.persistence.repository;

import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Tum sorgular JPA parameter binding kullanir (SQL injection korumasi zorunlu).
 */
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAssetJpaEntity, UUID> {

    @Query("""
           SELECT m FROM MediaAssetJpaEntity m
           WHERE m.productId = :productId AND m.status = com.mediaservice.domain.model.MediaStatus.ACTIVE
           ORDER BY m.sortOrder ASC, m.createdAt ASC
           """)
    List<MediaAssetJpaEntity> findActiveByProductId(@Param("productId") UUID productId);

    @Query("""
           SELECT m FROM MediaAssetJpaEntity m
           WHERE m.productId IN :productIds AND m.status = com.mediaservice.domain.model.MediaStatus.ACTIVE
           ORDER BY m.productId ASC, m.sortOrder ASC, m.createdAt ASC
           """)
    List<MediaAssetJpaEntity> findActiveByProductIds(@Param("productIds") List<UUID> productIds);

    /**
     * [Karar C] Urun bazli pessimistic write lock.
     * Ayni product uzerindeki SetPrimary/Reorder/Delete islemlerini serilestirir.
     * Siralama sabit (sortOrder, id) oldugu icin deadlock uretmez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT m FROM MediaAssetJpaEntity m
           WHERE m.productId = :productId AND m.status = com.mediaservice.domain.model.MediaStatus.ACTIVE
           ORDER BY m.sortOrder ASC, m.id ASC
           """)
    List<MediaAssetJpaEntity> lockActiveByProductId(@Param("productId") UUID productId);

    @Query("""
           SELECT COUNT(m) FROM MediaAssetJpaEntity m
           WHERE m.productId = :productId AND m.status = com.mediaservice.domain.model.MediaStatus.ACTIVE
           """)
    long countActiveByProductId(@Param("productId") UUID productId);

    /**
     * SetPrimaryImage sirasinda ONCE bu calisir, SONRA hedef true yapilir.
     * Sira onemlidir: uq_media_asset_primary kismi unique indeksi ihlal edilmemelidir.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE MediaAssetJpaEntity m SET m.primary = false
           WHERE m.productId = :productId AND m.primary = true
           """)
    int clearPrimaryFlagForProduct(@Param("productId") UUID productId);

    @Query("""
           SELECT m FROM MediaAssetJpaEntity m
           WHERE m.productId = :productId
           ORDER BY m.sortOrder ASC
           """)
    List<MediaAssetJpaEntity> findAllByProductIdIncludingDeleted(@Param("productId") UUID productId);
}