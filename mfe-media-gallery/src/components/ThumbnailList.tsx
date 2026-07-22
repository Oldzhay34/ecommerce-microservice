import React from 'react';
import { MediaAssetResponse } from '../types';

interface ThumbnailListProps {
    assets: MediaAssetResponse[];
    activeId: string;
    onSelect: (asset: MediaAssetResponse) => void;
}

export const ThumbnailList: React.FC<ThumbnailListProps> = ({ assets, activeId, onSelect }) => {
    return (
        <div className="flex gap-3 overflow-x-auto pb-2 mt-4 custom-scrollbar">
            {assets.map((asset) => {
                // Backend DTO alan adı: assetId (id DEĞİL).
                const isActive = asset.assetId === activeId;
                return (
                    <div
                        key={asset.assetId}
                        onClick={() => onSelect(asset)}
                        className={`w-[72px] h-[72px] rounded-lg overflow-hidden shrink-0 cursor-pointer transition-colors border ${
                            isActive
                                ? 'border-[2px] border-[#1D4ED8]'
                                : 'border border-[#E5E7EB] hover:border-[#9CA3AF]'
                        }`}
                    >
                        <img
                            src={asset.thumbUrl}
                            alt="Thumbnail"
                            className="w-full h-full object-cover"
                            onError={(e) => {
                                // Sessizce kırık görsel yerine boş div gösterilir, CLS engellenir
                                e.currentTarget.style.display = 'none';
                            }}
                        />
                    </div>
                );
            })}
        </div>
    );
};