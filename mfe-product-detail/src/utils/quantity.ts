/**
 * Adet sınır kontrolü — TEK NOKTA.
 * Bu ekranda serbest metin girişi yoktur; quantity yalnızca stepper ile değişir.
 * [Varsayım: tek seferde sepete eklenebilecek üst sınır backend'de tanımlı değil;
 *  UI 10 ile sınırlar. Backend farklı bir sınır uygularsa 400 döner ve
 *  ErrorBanner gösterilir; sınır öğrenilirse MAX_PER_ORDER güncellenmelidir.]
 */

export const MIN_QUANTITY = 1;
export const MAX_PER_ORDER = 10;

/** Verilen stok için seçilebilecek en yüksek adet. */
export function maxQuantityFor(stock: number): number {
    const safeStock = Number.isFinite(stock) ? Math.trunc(stock) : 0;
    if (safeStock <= 0) {
        return MIN_QUANTITY;
    }
    return Math.min(safeStock, MAX_PER_ORDER);
}

/** value'yu [1, min(stock, 10)] aralığına çeker. */
export function clampQuantity(value: number, stock: number): number {
    const max = maxQuantityFor(stock);
    const safeValue = Number.isFinite(value) ? Math.trunc(value) : MIN_QUANTITY;

    if (safeValue < MIN_QUANTITY) {
        return MIN_QUANTITY;
    }
    if (safeValue > max) {
        return max;
    }
    return safeValue;
}