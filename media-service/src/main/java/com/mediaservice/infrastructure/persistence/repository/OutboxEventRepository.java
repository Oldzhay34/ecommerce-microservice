package com.mediaservice.infrastructure.persistence.repository;

import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * FOR UPDATE SKIP LOCKED: birden fazla publisher instance'i ayni satiri iki kez basmaz.
     * idx_outbox_event_unprocessed (processed, created_at) indeksi kullanilir.
     */
    @Query(value = """
                   SELECT * FROM outbox_event
                   WHERE processed = false
                   ORDER BY created_at ASC
                   LIMIT :batchSize
                   FOR UPDATE SKIP LOCKED
                   """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockUnprocessedBatch(@Param("batchSize") int batchSize);

    @Modifying
    @Query("""
           UPDATE OutboxEventJpaEntity o
           SET o.processed = true, o.processedAt = :processedAt
           WHERE o.id IN :ids
           """)
    int markProcessed(@Param("ids") List<UUID> ids, @Param("processedAt") Instant processedAt);
}