import { create } from 'zustand';

interface CatalogUiState {
    page: number;
    keyword: string;
    category: string;
    setPage: (page: number) => void;
    setFilter: (filter: { keyword?: string; category?: string }) => void;
    resetFilter: () => void;
}

export const useCatalogUiStore = create<CatalogUiState>((set) => ({
    page: 1,
    keyword: '',
    category: '',
    setPage: (page) => set({ page }),
    setFilter: (filter) =>
        set((state) => ({
            keyword: filter.keyword ?? state.keyword,
            category: filter.category ?? state.category,
            page: 1, // filtre değişince sayfa resetlenir
        })),
    resetFilter: () => set({ keyword: '', category: '', page: 1 }),
}));