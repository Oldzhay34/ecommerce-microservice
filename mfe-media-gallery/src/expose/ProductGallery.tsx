import React, { useState, useEffect } from 'react';
import { ProductGalleryProps, MediaAssetResponse } from '../types';
import { useProductMedia } from '../hooks/useProductMedia';
import { PlaceholderBox } from '../components/PlaceholderBox';
import { SkeletonGallery } from '../components/SkeletonGallery';
import { MainImage } from '../components/MainImage';
import { ThumbnailList } from '../components/ThumbnailList';

const ProductGallery: React.FC<ProductGalleryProps> = ({ productId }) => {
    const { data: assets, isLoading, isError } = useProductMedia(productId);
    const [activeAsset, setActiveAsset] = useState<MediaAssetResponse | null>(null);

    useEffect(() => {
        if (assets && assets.length > 0) {
            const primary = assets.find((a) => a.primary) || assets[0];
            setActiveAsset(primary);
        } else {
            setActiveAsset(null);
        }
    }, [assets]);

    // Loading state
    if (isLoading) {
        return <SkeletonGallery />;
    }

    // Error veya boş veri durumu
    if (isError || !assets || assets.length === 0 || !activeAsset) {
        return <PlaceholderBox />;
    }

    return (
        <div className="w-full">
            <MainImage asset={activeAsset} />

            {assets.length > 1 && (
                <ThumbnailList
                    assets={assets}
                    activeId={activeAsset.assetId}
                    onSelect={setActiveAsset}
                />
            )}
        </div>
    );
};

export default ProductGallery;