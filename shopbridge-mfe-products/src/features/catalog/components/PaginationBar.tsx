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
                className="px-3 py-1 rounded-sb text-sm border border-border disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-hover text-ink-muted"
            >
                Önceki
            </button>

            {numbers.map((n, i) =>
                n === '…' ? (
                    <span key={`e${i}`} className="px-2 text-ink-faint">…</span>
                ) : (
                    <button
                        key={n}
                        onClick={() => onPageChange(n)}
                        className={`px-3 py-1 rounded-sb text-sm border ${
                            n === currentPage
                                ? 'bg-brand text-white border-brand'
                                : 'border-border text-ink-muted hover:bg-surface-hover'
                        }`}
                    >
                        {n}
                    </button>
                )
            )}

            <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={!hasNext}
                className="px-3 py-1 rounded-sb text-sm border border-border disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-hover text-ink-muted"
            >
                Sonraki
            </button>
        </div>
    );
}