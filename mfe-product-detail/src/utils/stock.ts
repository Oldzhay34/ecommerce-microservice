export interface StockView {
    label: string;
    bg: string;
    text: string;
    note: string;
    isOutOfStock: boolean;
}

export function getStockView(stock: number): StockView {
    const safeStock = Number.isFinite(stock) ? Math.trunc(stock) : 0;

    if (safeStock <= 0) {
        return {
            label: 'Tükendi',
            bg: '#FEE2E2',
            text: '#991B1B',
            note: 'Bu ürün şu anda stokta yok.',
            isOutOfStock: true,
        };
    }

    if (safeStock <= 5) {
        return {
            label: `Son ${safeStock} adet`,
            bg: '#FEF3C7',
            text: '#92400E',
            note: 'Stok azalıyor.',
            isOutOfStock: false,
        };
    }

    return {
        label: 'Stokta',
        bg: '#D1FAE5',
        text: '#065F46',
        note: `${safeStock} adet mevcut.`,
        isOutOfStock: false,
    };
}