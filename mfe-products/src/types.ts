// [Varsayım] Product yanıtı storeId içerir (yoksa /api/v1/products/store/{storeId} gerekir — Açık Sorular #1)
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
}

// Ortak oturum sözleşmesi (shell ile birebir)
export interface StoreSession {
    authToken: string;
    storeId: string;
    role: 'STORE';
}
// ---- YENİ ----
// CreateProductRequest — backend record ile birebir (4 alan, storeId YOK)
// @NotBlank String name / @NotBlank String category
// @NotNull @Positive BigDecimal price / @NotNull @PositiveOrZero Integer stock
export interface CreateProductRequest {
    name: string;
    category: string;
    price: number;
    stock: number;
}

// ---- DÜZELTİLDİ ----
// [Varsayım kaldırıldı: backend DTO'su doğrulandı]
// public record UpdateStockRequest(@NotNull @PositiveOrZero Integer newStock) {}
export interface UpdateStockRequest {
    newStock: number; // ÖNCE: stock — YANLIŞTI
}