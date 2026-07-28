import { useRef, useState, type DragEvent } from 'react';
import { MEDIA_LIMITS } from '../api/endpoints';

export function ImageDropzone({
                                  onFilesSelected,
                                  disabled,
                                  remainingSlots,
                              }: {
    onFilesSelected: (files: File[]) => void;
    disabled: boolean;
    remainingSlots: number;
}) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [isDragging, setIsDragging] = useState(false);

    const isFull = remainingSlots <= 0;
    const blocked = disabled || isFull;

    const handleFiles = (fileList: FileList | null) => {
        if (!fileList || blocked) return;
        onFilesSelected(Array.from(fileList));
        if (inputRef.current) inputRef.current.value = '';
    };

    const onDrop = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(false);
        handleFiles(e.dataTransfer.files);
    };

    const onDragOver = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        if (!blocked) setIsDragging(true);
    };

    return (
        <div>
            <div
                onDrop={onDrop}
                onDragOver={onDragOver}
                onDragLeave={() => setIsDragging(false)}
                onClick={() => !blocked && inputRef.current?.click()}
                role="button"
                tabIndex={blocked ? -1 : 0}
                aria-disabled={blocked}
                onKeyDown={(e) => {
                    if ((e.key === 'Enter' || e.key === ' ') && !blocked) {
                        e.preventDefault();
                        inputRef.current?.click();
                    }
                }}
                className={[
                    'w-full rounded-sb border border-dashed px-4 py-8 text-center',
                    'transition-colors',
                    blocked
                        ? 'border-border bg-canvas cursor-not-allowed opacity-60'
                        : 'cursor-pointer hover:border-brand',
                    isDragging ? 'border-brand bg-brand/10' : 'border-border-strong bg-surface',
                ].join(' ')}
            >
                <p className="text-ink text-sm font-semibold">
                    {isFull
                        ? `En fazla ${MEDIA_LIMITS.maxImagesOnCreate} görsel ekleyebilirsiniz.`
                        : 'Görselleri sürükleyin veya seçmek için tıklayın'}
                </p>
                {!isFull && (
                    <p className="text-ink-muted text-xs mt-1.5">
                        PNG, JPEG veya WebP · tek dosya en fazla 5MB · {remainingSlots} görsel daha
                        eklenebilir
                    </p>
                )}
            </div>

            <input
                ref={inputRef}
                type="file"
                multiple
                accept={MEDIA_LIMITS.acceptedMimeTypes.join(',')}
                disabled={blocked}
                onChange={(e) => handleFiles(e.target.files)}
                className="hidden"
            />
        </div>
    );
}