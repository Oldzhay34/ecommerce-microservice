import { useCallback } from 'react';
import { MEDIA_LIMITS } from '../api/endpoints';
import { validateImageFile } from '../validation/validateImageFile';
import { ImageDropzone } from '../components/ImageDropzone';
import { UploadPreviewList } from '../components/UploadPreviewList';
import type { PendingImage, UploadItemResult } from '../types';

/**
 * KONTROLLÜ bileşen: seçilen dosyalar host'un (mfe-products-create) state'inde durur,
 * upload'ı da host tetikler.
 *
 * Neden: (b) akışında upload, product create'in onSuccess'inde başlar ve productId o ana
 * kadar YOKTUR. Bileşen kendi içinde upload edemez.
 */
export function ProductImageUploader({
                                         images,
                                         onChange,
                                         results,
                                         disabled,
                                         maxImages = MEDIA_LIMITS.maxImagesOnCreate,
                                     }: {
    images: PendingImage[];
    onChange: (next: PendingImage[]) => void;
    results: UploadItemResult[];
    disabled: boolean;
    maxImages?: number;
}) {
    const remainingSlots = maxImages - images.length;

    const handleFilesSelected = useCallback(
        (files: File[]) => {
            const accepted: PendingImage[] = [];

            for (const file of files) {
                if (accepted.length >= remainingSlots) break;
                if (validateImageFile(file) !== null) continue; // sessizce ele; hata satırda gösterilmez
                accepted.push({
                    id: `${file.name}-${file.size}-${file.lastModified}-${Math.random().toString(36).slice(2, 8)}`,
                    file,
                    previewUrl: URL.createObjectURL(file),
                });
            }

            if (accepted.length > 0) onChange([...images, ...accepted]);
        },
        [images, onChange, remainingSlots],
    );

    const handleRemove = useCallback(
        (id: string) => {
            const target = images.find((i) => i.id === id);
            if (target) URL.revokeObjectURL(target.previewUrl);
            onChange(images.filter((i) => i.id !== id));
        },
        [images, onChange],
    );

    const handleMoveUp = useCallback(
        (id: string) => {
            const index = images.findIndex((i) => i.id === id);
            if (index <= 0) return;
            const next = [...images];
            [next[index - 1], next[index]] = [next[index], next[index - 1]];
            onChange(next);
        },
        [images, onChange],
    );

    return (
        <div>
            <ImageDropzone
                onFilesSelected={handleFilesSelected}
                disabled={disabled}
                remainingSlots={remainingSlots}
            />

            <UploadPreviewList
                images={images}
                results={results}
                onRemove={handleRemove}
                onMoveUp={handleMoveUp}
                disabled={disabled}
            />

            {images.length > 0 && (
                <p className="text-muted text-xs mt-3">
                    İlk sıradaki görsel ürünün kapak görseli olur. Sıralamayı ↑ ile
                    değiştirebilirsiniz.
                </p>
            )}
        </div>
    );
}