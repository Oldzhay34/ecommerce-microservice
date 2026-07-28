import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import { usePaginatedProducts } from './hooks/usePaginatedProducts';
import { FilterBar } from './components/FilterBar';
import { ProductGrid } from './components/ProductGrid';
import { PaginationBar } from './components/PaginationBar';
import { ProductGridSkeleton } from './components/ProductGridSkeleton';
import { ErrorBanner } from './components/ErrorBanner';

function CatalogInner() {
    const {
        pageItems,
        currentPage,
        totalPages,
        totalItems,
        hasPrev,
        hasNext,
        isLoading,
        isError,
        keyword,
        category,
        setPage,
        setFilter,
        resetFilter,
    } = usePaginatedProducts();

    return (
        <section className="w-full">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-lg text-ink">Ürünler</h3>
                {!isLoading && !isError && (
                    <span className="text-sm text-ink-faint">{totalItems} ürün</span>
                )}
            </div>

            <FilterBar
                keyword={keyword}
                category={category}
                onFilter={(f) => setFilter(f)}
                onReset={resetFilter}
            />

            {isLoading && <ProductGridSkeleton />}
            {isError && <ErrorBanner message="Ürünler yüklenemedi. Lütfen daha sonra tekrar deneyin." />}

            {!isLoading && !isError && totalItems === 0 && (
                <div className="p-8 text-center text-ink-muted border border-border rounded-sb-lg bg-surface">
                    Aramanıza uygun ürün bulunamadı. Filtreyi temizlemeyi deneyin.
                </div>
            )}

            {!isLoading && !isError && totalItems > 0 && (
                <>
                    <ProductGrid products={pageItems} />
                    <PaginationBar
                        currentPage={currentPage}
                        totalPages={totalPages}
                        hasPrev={hasPrev}
                        hasNext={hasNext}
                        onPageChange={setPage}
                    />
                </>
            )}
        </section>
    );
}

// Defensive: shell bir QueryClient sağlıyorsa onu kullan, yoksa kendi provider'ımızı sar.
let fallbackClient: QueryClient | null = null;

function HasQueryClientGuard({ children }: { children: React.ReactNode }) {
    try {
        useQueryClient(); // context varsa sorunsuz döner
        return <>{children}</>;
    } catch {
        if (!fallbackClient) fallbackClient = new QueryClient();
        return <QueryClientProvider client={fallbackClient}>{children}</QueryClientProvider>;
    }
}

export default function ProductCatalog() {
    return (
        <HasQueryClientGuard>
            <CatalogInner />
        </HasQueryClientGuard>
    );
}