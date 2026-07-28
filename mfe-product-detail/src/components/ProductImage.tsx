import { useEffect, useState } from 'react';

/**
 * Presenter · veri çekmez.
 *
 * Kutu, src null iken de aynı boyuttadır (aspect-ratio: 1/1).
 * Boyut asla içeriğe göre hesaplanmaz — görsel alanı ileride eklendiğinde
 * sayfa zıplamaz (CLS = 0). Bu bilinçli bir karardır
 * (Pressman · System Response Time · Theo Mandel · Arayüzü Tutarlı Yap).
 */
export interface ProductImageProps {
    src: string | null;
    alt: string;
}

export function ProductImage({ src, alt }: ProductImageProps) {
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        setFailed(false);
    }, [src]);

    const showPlaceholder = src === null || failed;

    return (
        <div
            style={{
                width: '100%',
                aspectRatio: '1 / 1',
                backgroundColor: '#F7F8FA',
                border: '1px solid #E5E7EB',
                borderRadius: 16,
                overflow: 'hidden',
                position: 'relative',
            }}
            {...(showPlaceholder
                ? { role: 'img', 'aria-label': `${alt} — görsel henüz eklenmedi` }
                : {})}
        >
            {showPlaceholder ? (
                <div
                    style={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        gap: 8,
                        padding: 16,
                    }}
                >
                    <svg
                        width="48"
                        height="48"
                        viewBox="0 0 48 48"
                        fill="none"
                        stroke="#9CA3AF"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        aria-hidden="true"
                        focusable="false"
                    >
                        <rect x="4" y="8" width="40" height="32" rx="4" />
                        <circle cx="16" cy="19" r="3.5" />
                        <path d="M6 34 L18 23 L28 32 L34 27 L42 34" />
                    </svg>
                    <span style={{ fontSize: 13, color: '#9CA3AF', textAlign: 'center' }}>
            Görsel yakında eklenecek
          </span>
                </div>
            ) : (
                <img
                    src={src ?? undefined}
                    alt={alt}
                    loading="lazy"
                    decoding="async"
                    onError={() => setFailed(true)}
                    style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        display: 'block',
                    }}
                />
            )}
        </div>
    );
}