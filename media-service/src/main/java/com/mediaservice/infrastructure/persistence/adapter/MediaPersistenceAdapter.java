package com.mediaservice.infrastructure.persistence.adapter;

import com.mediaservice.application.port.out.MediaCommandPort;
import com.mediaservice.application.port.out.MediaQueryPort;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.mapper.MediaEntityMapper;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MediaPersistenceAdapter implements MediaCommandPort, MediaQueryPort {

    private final MediaAssetRepository repository;
    private final MediaEntityMapper mapper;

    public MediaPersistenceAdapter(MediaAssetRepository repository, MediaEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MediaAsset save(MediaAsset asset) {
        asset.validateWebpInvariant();
        MediaAssetJpaEntity entity = repository.findById(asset.getId())
                .orElseGet(MediaAssetJpaEntity::new);
        mapper.applyToEntity(asset, entity);
        if (entity.getVersion() == null) {
            entity.setVersion(0L);
        }
        MediaAssetJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<MediaAsset> saveAll(List<MediaAsset> assets) {
        List<MediaAsset> result = new ArrayList<>(assets.size());
        for (MediaAsset asset : assets) {
            result.add(save(asset));
        }
        return result;
    }

    @Override
    @Transactional
    public List<MediaAsset> lockActiveByProductIdForUpdate(UUID productId) {
        return mapper.toDomainList(repository.lockActiveByProductId(productId));
    }

    @Override
    @Transactional
    public void clearPrimaryFlagForProduct(UUID productId) {
        repository.clearPrimaryFlagForProduct(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByProductId(UUID productId) {
        return repository.countActiveByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaAsset> findById(UUID assetId) {
        return repository.findById(assetId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaAsset> findActiveByProductIdOrderBySortOrder(UUID productId) {
        return mapper.toDomainList(repository.findActiveByProductId(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaAsset> findActiveByProductIdsOrderBySortOrder(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mapper.toDomainList(repository.findActiveByProductIds(productIds));
    }
}