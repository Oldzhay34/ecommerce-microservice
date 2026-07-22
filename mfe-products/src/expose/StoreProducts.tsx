import { useState } from 'react';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useStoreProducts } from '../hooks/useStoreProducts';
import { ProductList } from '../components/ProductList';
import { AddProductButton } from '../components/AddProductButton';
import type { StoreSession } from '../types';

// Container: veri/mantık. StoreSession prop alır.
function StoreProductsInner({
                                session,
                                onNavigate,
                            }: {
    session: StoreSession;
    onNavigate?: (path: string) => void;
}) {
    const [keyword, setKeyword] = useState('');
    const [category, setCategory] = useState('');
    const { data, isLoading, isError } = useStoreProducts(session.storeId, {
        keyword: keyword || undefined,
        category: category || undefined,
    });

    return (
        <div>
            <div className="flex flex-wrap items-center gap-3 mb-4">
                <input
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    placeholder="Ürün ara..."
                    className="h-10 rounded-btn border border-line-strong px-3 text-ink outline-none focus:border-brand min-w-[200px]"
                />
                <input
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    placeholder="Kategori"
                    className="h-10 rounded-btn border border-line-strong px-3 text-ink outline-none focus:border-brand min-w-[160px]"
                />
                <div className="ml-auto">
                    <AddProductButton onNavigate={onNavigate} />
                </div>
            </div>

            <ProductList
                products={data ?? []}
                storeId={session.storeId}
                isLoading={isLoading}
                isError={isError}
            />
        </div>
    );
}

// Expose: token'ı iç context'e köprüler.
export default function StoreProducts({
                                          session,
                                          onNavigate,
                                      }: {
    session: StoreSession;
    onNavigate?: (path: string) => void;
}) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <StoreProductsInner session={session} onNavigate={onNavigate} />
        </AuthTokenProvider>
    );
}