/**
 * Presenter · veri çekmez.
 * Skeleton, yüklenen içerikle AYNI yerleşimdedir — görsel kutusu dahil.
 * İçerik gelince zıplama olmaz (Pressman · System Response Time).
 */
export function DetailSkeleton() {
    const blocks: Array<{ width: string; height: number }> = [
        { width: '96px', height: 20 },
        { width: '70%', height: 28 },
        { width: '40%', height: 32 },
        { width: '100%', height: 48 },
    ];

    return (
        <div
            aria-busy="true"
            aria-live="polite"
            aria-label="Ürün yükleniyor"
            className="sb-detail-grid"
        >
            {/* Sol kolon — kare kutu */}
            <div
                className="sb-shimmer"
                style={{ width: '100%', aspectRatio: '1 / 1', borderRadius: 12 }}
            />

            {/* Sağ kolon — 4 blok */}
            <div>
                {blocks.map((block, index) => (
                    <div
                        key={`${block.width}-${block.height}`}
                        className="sb-shimmer"
                        style={{
                            width: block.width,
                            height: block.height,
                            borderRadius: 8,
                            marginTop: index === 0 ? 0 : 16,
                        }}
                    />
                ))}
            </div>
        </div>
    );
}