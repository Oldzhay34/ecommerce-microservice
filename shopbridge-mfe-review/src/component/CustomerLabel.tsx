/**
 * Yorum sahibinin gösterimi.
 *
 * [Karar] Gerçek isim GÖSTERİLMEZ. ReviewResponse yalnızca customerId (UUID) taşır;
 * isim için Auth servisine çağrı gerekirdi. Bu iki soruna yol açar:
 *   1. N+1: her yorum için ayrı istek.
 *   2. Sızıntı: "A**** M****" maskesi frontend'de yapılır, yani tam isim tarayıcıya iner
 *      ve DevTools'ta görünür. Maskeleme kozmetik olur, gizlilik sağlamaz.
 *
 * Gerçek maskeleme backend'de yapılmalıdır (ReviewResponse'a maskelenmiş customerName
 * eklenmesi) — Review servisi DOKUNULMAZ olduğu için ayrı prompt gerekir.
 */
export function CustomerLabel({ index }: { index: number }) {
    return <span className="font-medium text-gray-900">Müşteri #{index + 1}</span>;
}