export function ProductPreviewCard({
                                       name,
                                       category,
                                       price,
                                       stock,
                                       coverPreviewUrl,
                                   }: {
    name: string;
    category: string;
    price: number | undefined;
    stock: number | undefined;
    coverPreviewUrl?: string;
}) {
    const hasName = name.trim().length > 0;
    const hasCategory = category.trim().length > 0;
    const hasPrice = typeof price === 'number' && Number.isFinite(price);
    const hasStock = typeof stock === 'number' && Number.isFinite(stock);

    return (
        <div className="bg-surface-raised border border-border-strong shadow-sb-lg rounded-card px-7 py-6">
            <div className="text-ink-muted text-[13px] font-semibold uppercase tracking-[0.06em] mb-4">
                Önizleme
            </div>

            <div className="border border-border rounded-btn overflow-hidden bg-surface">
                {/* Kapak: seçilen ilk görsel. Yükleme sonrası backend bunu primary yapar. */}
                <div className="w-full aspect-[4/3] bg-canvas flex items-center justify-center">
                    {coverPreviewUrl ? (
                        <img src={coverPreviewUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                        <span className="text-ink-faint text-xs">Görsel yok</span>
                    )}
                </div>

                <div className="px-4 py-4">
                    <div className="text-[11px] uppercase tracking-wide text-ink-faint">
                        {hasCategory ? category.trim().toUpperCase() : 'KATEGORİ'}
                    </div>

                    <div
                        className={`font-semibold text-base mt-0.5 break-words ${
                            hasName ? 'text-ink' : 'text-ink-faint'
                        }`}
                    >
                        {hasName ? name.trim() : 'Ürün adı'}
                    </div>

                    <div className="mt-2 flex items-center justify-between gap-4">
                        <span className={`font-bold ${hasPrice ? 'text-ink' : 'text-ink-faint'}`}>
                            {hasPrice ? priceFmt.format(price as number) : '₺0,00'}
                        </span>
                        <span className={`text-sm ${hasStock ? 'text-ink-muted' : 'text-ink-faint'}`}>
                            Stokta {hasStock ? stock : 0} adet
                        </span>
                    </div>
                </div>
            </div>

            <p className="text-ink-muted text-xs mt-4">
                Bu kart, ürününüzün müşteri listelerinde nasıl görüneceğini gösterir.
            </p>
        </div>
    );
}

const priceFmt = new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});