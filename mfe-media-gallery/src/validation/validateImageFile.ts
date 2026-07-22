import { MEDIA_LIMITS } from '../api/endpoints';

/**
 * İstemci tarafı ön doğrulama. Backend'in magic byte doğrulamasının YERİNE GEÇMEZ —
 * yalnızca kullanıcıya erken geri bildirim verir. Nihai karar backend'indir (415/413).
 */
export function validateImageFile(file: File): string | null {
    const accepted = MEDIA_LIMITS.acceptedMimeTypes as readonly string[];
    if (!accepted.includes(file.type)) {
        return 'Yalnızca PNG, JPEG ve WebP formatları kabul edilir.';
    }
    if (file.size > MEDIA_LIMITS.maxFileSizeBytes) {
        return 'Dosya boyutu 5MB sınırını aşıyor.';
    }
    return null;
}

export function formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}