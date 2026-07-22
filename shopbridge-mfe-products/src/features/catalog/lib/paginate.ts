import type { Product } from '../types/product';

export const PAGE_SIZE = 8;

export interface PaginationResult<T> {
    pageItems: T[];
    currentPage: number;
    totalPages: number;
    totalItems: number;
    hasPrev: boolean;
    hasNext: boolean;
}

export function paginate<T>(
    items: T[],
    page: number,
    pageSize: number = PAGE_SIZE
): PaginationResult<T> {
    const totalItems = items.length;
    const totalPages = Math.ceil(totalItems / pageSize);

    if (totalItems === 0) {
        return { pageItems: [], currentPage: 1, totalPages: 0, totalItems: 0, hasPrev: false, hasNext: false };
    }

    const currentPage = Math.min(Math.max(1, page), totalPages);
    const start = (currentPage - 1) * pageSize;
    const pageItems = items.slice(start, start + pageSize);

    return {
        pageItems,
        currentPage,
        totalPages,
        totalItems,
        hasPrev: currentPage > 1,
        hasNext: currentPage < totalPages,
    };
}

export function sortStable(items: Product[]): Product[] {
    return [...items].sort((a, b) => a.name.localeCompare(b.name, 'tr'));
}