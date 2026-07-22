import { buildPageNumbers } from '../lib/pageNumbers';

interface Props {
    currentPage: number;
    totalPages: number;
    hasPrev: boolean;
    hasNext: boolean;
    onPageChange: (page: number) => void;
}

export function PaginationBar({ currentPage, totalPages, hasPrev, hasNext, onPageChange }: Props) {
    if (totalPages <= 1) return null;
    const numbers = buildPageNumbers(currentPage, totalPages);

    return (
        <div className="flex items-center justify-center gap-1 mt-6">
            <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={!hasPrev}
                className="px-3 py-1 rounded-md text-sm border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
            >
                Önceki
            </button>

            {numbers.map((n, i) =>
                n === '…' ? (
                    <span key={`e${i}`} className="px-2 text-gray-400">…</span>
                ) : (
                    <button
                        key={n}
                        onClick={() => onPageChange(n)}
                        className={`px-3 py-1 rounded-md text-sm border ${
                            n === currentPage
                                ? 'bg-blue-700 text-white border-blue-700'
                                : 'border-gray-300 hover:bg-gray-50'
                        }`}
                    >
                        {n}
                    </button>
                )
            )}

            <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={!hasNext}
                className="px-3 py-1 rounded-md text-sm border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
            >
                Sonraki
            </button>
        </div>
    );
}