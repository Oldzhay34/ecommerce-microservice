// Ortak oturum sözleşmesi (shell ve mfe-products ile birebir aynı).
export interface StoreSession {
    authToken: string;
    storeId: string;
    role: 'STORE';
}

// [Varsayım] Product yanıtı storeId içerir (mfe-products/types.ts ile birebir)
export interface Product {
    id: string;
    name: string;
    category: string;
    price: number;
    stock: number;
    storeId: string;
    description?: string;
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    // [Varsayım] Spring @Valid ihlallerinde alan bazlı harita; yoksa undefined gelir.
    errors?: Record<string, string>;
}

// CreateProductRequest — backend record ile birebir (4 alan, storeId YOK).
// @NotBlank String name / @NotBlank String category
// @NotNull @Positive BigDecimal price / @NotNull @PositiveOrZero Integer stock
export interface CreateProductRequest {
    name: string;
    category: string;
    price: number;
    stock: number;
}
