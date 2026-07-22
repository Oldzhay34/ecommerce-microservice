/**
 * ShopBridge · mfe-product-detail — tip sözleşmeleri.
 * Alan adları backend DTO'larından türetilmiştir; [Varsayım] etiketleri korunur.
 */

/** Shell → Remote oturum sözleşmesi (SABİT). */
export interface CustomerSession {
    authToken: string;
    userId: string;
    role: 'CUSTOMER';
}

/**
 * Product — backend domain modeli.
 * [KESİN alanlar: CreateProductRequest ve controller imzasından doğrulandı]
 * name, category, price, stock alanları CreateProductRequest ile birebir.
 * id alanı controller @PathVariable UUID id ve @GetMapping("/{id}") ile doğrulandı.
 */
export interface Product {
    id: string;
    name: string; // @NotBlank
    category: string; // @NotBlank
    price: number; // BigDecimal → JSON number · @Positive
    stock: number; // Integer · @PositiveOrZero
    /** [Varsayım: domain modelde var; createProduct(request, storeId) imzasından çıkarıldı] */
    storeId?: string | null;
    /**
     * [Varsayım: Backend Product modelinde görsel alanı henüz YOK.
     *  Alan eklendiğinde bu iki alandan biri dolacak; UI şimdiden ikisini de destekler.
     *  Backend farklı bir isim kullanırsa (ör. thumbnailUrl) yalnızca resolveImageUrl güncellenir.]
     */
    imageUrl?: string | null;
    imageUrls?: string[] | null;
}

/** [Varsayım: Cart servisi doğrulanmadı — controller kodu verilmemiştir.] */
export interface AddToCartRequest {
    productId: string;
    quantity: number;
    // DÜZELTME BURADA: TypeScript'in kızmaması için price alanını ekliyoruz
    price: number;
}

/** ApiErrorResponse (gateway + servisler ortak şekli). */
export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}