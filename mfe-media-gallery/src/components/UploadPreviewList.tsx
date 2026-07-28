import type { PendingImage, UploadItemResult } from '../types';
import { formatBytes } from '../validation/validateImageFile';

/**
 * Seçilen görsellerin önizlemesi + yükleme durumu.
 * İlk sıradaki görsel KAPAK'tır — backend ilk yüklenen görseli primary = true yapar.
 */
export function UploadPreviewList({
                                      images,
                                      results,
                                      onRemove,
                                      onMoveUp,
                                      disabled,
                                  }: {
    images: PendingImage[];
    results: UploadItemResult[];
    onRemove: (id: string) => void;
    onMoveUp: (id: string) => void;
    disabled: boolean;
}) {
    if (images.length === 0) return null;

    const resultOf = (id: string) => results.find((r) => r.id === id);

    return (
        <ul className="mt-4 flex flex-col gap-2">
            {images.map((img, index) => {
                const result = resultOf(img.id);
                const isCover = index === 0;

                return (
                    <li
                        key={img.id}
                        className="flex items-center gap-3 border border-border rounded-sb px-3 py-2 bg-surface"
                    >
                        <img
                            src={img.previewUrl}
                            alt=""
                            className="w-12 h-12 object-cover rounded shrink-0"
                        />

                        <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                                <span className="text-ink text-sm truncate">{img.file.name}</span>
                                {isCover && (
                                    <span className="text-[10px] uppercase tracking-wide bg-brand text-white px-1.5 py-0.5 rounded shrink-0">
                                        Kapak
                                    </span>
                                )}
                            </div>
                            <StatusLine result={result} sizeBytes={img.file.size} />
                        </div>

                        {!disabled && (
                            <div className="flex items-center gap-1 shrink-0">
                                {index > 0 && (
                                    <button
                                        type="button"
                                        onClick={() => onMoveUp(img.id)}
                                        aria-label="Yukarı taşı"
                                        className="text-ink-muted hover:text-ink text-sm px-2 py-1"
                                    >
                                        ↑
                                    </button>
                                )}
                                <button
                                    type="button"
                                    onClick={() => onRemove(img.id)}
                                    aria-label="Kaldır"
                                    className="text-ink-muted hover:text-danger text-sm px-2 py-1"
                                >
                                    ✕
                                </button>
                            </div>
                        )}
                    </li>
                );
            })}
        </ul>
    );
}

function StatusLine({
                        result,
                        sizeBytes,
                    }: {
    result: UploadItemResult | undefined;
    sizeBytes: number;
}) {
    if (!result || result.status === 'pending') {
        return <p className="text-ink-faint text-xs mt-0.5">{formatBytes(sizeBytes)}</p>;
    }
    if (result.status === 'uploading') {
        return <p className="text-ink-muted text-xs mt-0.5">Yükleniyor…</p>;
    }
    if (result.status === 'success') {
        return <p className="text-xs mt-0.5 text-success">Yüklendi</p>;
    }
    return <p className="text-danger text-xs mt-0.5">{result.message}</p>;
}