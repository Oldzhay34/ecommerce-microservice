import { useQuery } from '@tanstack/react-query';
import { getUserIdFromToken } from '../auth/readAuthContract';
import { apiClient } from '../api/client';
import { Card, ErrorBanner, Skeleton } from '../ui/components';

export default function CartWidget() {
    const userId = getUserIdFromToken();

    // 1. ADIM: Cart (Sepet) bilgisini çek (Gateway -> Cart Service)
    const { data: cartData, isLoading: isCartLoading, isError: isCartError } = useQuery({
        queryKey: ['cart', userId],
        queryFn: async () => {
            const response = await apiClient.get(`/api/carts/${userId}`);
            return response.data;
        },
        enabled: !!userId,
    });

    const productIds = Array.from(new Set(cartData?.items?.map((item: any) => item.productId) || []));

    // 2. ADIM: Sepetteki ürünlerin detaylarını çek (Gateway -> Product Service)
    const { data: productsData, isLoading: isProductsLoading } = useQuery({
        queryKey: ['products', productIds],
        queryFn: async () => {
            // NOT: Backend ProductController'da GET /api/v1/products/{id} yazılmış olmalıdır!
            const promises = productIds.map(id =>
                apiClient.get(`/api/v1/products/${id}`).then(res => res.data)
            );
            const products = await Promise.all(promises);

            const productMap: Record<string, any> = {};
            products.forEach(p => { productMap[p.id] = p; });
            return productMap;
        },
        enabled: !!cartData && productIds.length > 0,
    });

    if (!userId) return <ErrorBanner message="Lütfen sepetinizi görmek için giriş yapın." />;
    if (isCartLoading) return <Skeleton height="h-48" />;
    if (isCartError) return <ErrorBanner message="Sepet bilgisi yüklenirken bir hata oluştu." />;

    if (!cartData?.items || cartData.items.length === 0) {
        return (
            <Card>
                <p className="text-gray-500 text-center py-6">Sepetiniz şu an boş.</p>
            </Card>
        );
    }

    if (isProductsLoading) return <Skeleton height="h-48" />;

    // 3. ADIM: Cart ve Product datalarını birleştir (BFF Mantığı)
    const enrichedItems = cartData.items.map((cartItem: any) => {
        const productDetails = productsData ? productsData[cartItem.productId] : null;
        return {
            ...cartItem,
            name: productDetails?.name || 'Bilinmeyen Ürün',
            category: productDetails?.category || 'Kategori Yok'
        };
    });

    const totalItems = enrichedItems.reduce((acc: number, item: any) => acc + item.quantity, 0);
    const totalPrice = cartData.totalAmount || 0;

    return (
        <Card className="flex flex-col py-6 px-4">
            <h3 className="font-semibold text-xl mb-4 text-gray-800 border-b pb-2">Sepetim ({totalItems} Ürün)</h3>

            <div className="flex flex-col space-y-3 mb-6 max-h-64 overflow-y-auto pr-2">
                {enrichedItems.map((item: any, index: number) => (
                    <div key={`${item.productId}-${index}`} className="flex justify-between items-center bg-gray-50 p-3 rounded">
                        <div>
                            <div className="font-medium text-gray-800">{item.name}</div>
                            <div className="text-xs text-gray-500">{item.category}</div>
                        </div>
                        <div className="text-right">
                            <div className="font-semibold text-blue-600">₺{Number(item.price).toFixed(2)}</div>
                            <div className="text-sm text-gray-600">Adet: {item.quantity}</div>
                        </div>
                    </div>
                ))}
            </div>

            <div className="flex justify-between items-center pt-4 border-t border-gray-200">
                <span className="text-lg text-gray-600">Ara Toplam:</span>
                <span className="text-2xl font-bold text-gray-900">₺{Number(totalPrice).toFixed(2)}</span>
            </div>

            <button className="mt-4 w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded transition duration-200">
                Alışverişi Tamamla
            </button>
        </Card>
    );
}