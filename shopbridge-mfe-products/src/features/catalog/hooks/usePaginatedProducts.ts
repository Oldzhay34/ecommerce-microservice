import { useProducts } from './useProducts';
import { useCatalogUiStore } from '../store/catalogUiStore';
import { paginate, sortStable, PAGE_SIZE } from '../lib/paginate';

export function usePaginatedProducts() {
    const { page, keyword, category, setPage, setFilter, resetFilter } = useCatalogUiStore();
    const { data, isLoading, isError } = useProducts(keyword, category);

    const sorted = sortStable(data ?? []);
    const pageData = paginate(sorted, page, PAGE_SIZE);

    return {
        ...pageData,
        isLoading,
        isError,
        keyword,
        category,
        setPage,
        setFilter,
        resetFilter,
    };
}