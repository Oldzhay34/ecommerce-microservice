import React from 'react';

interface PaginationBarProps {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    onPageChange: (page: number) => void;
    isFirst: boolean;
    isLast: boolean;
}

export const PaginationBar: React.FC<PaginationBarProps> = ({
                                                                currentPage,
                                                                totalPages,
                                                                totalElements,
                                                                onPageChange,
                                                                isFirst,
                                                                isLast,
                                                            }) => {
    if (totalPages === 0) return null;

    return (
        <div className="w-full border-t border-[#E5E7EB] pt-4 flex items-center justify-between">
      <span className="text-xs text-[#6B7280]">
        Toplam {totalElements} sipariş · Sayfa {currentPage + 1} / {totalPages}
      </span>
            <div className="flex items-center space-x-2">
                <button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={isFirst}
                    className="h-8 px-3 rounded bg-white border border-[#D1D5DB] text-xs font-semibold text-[#111827] hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                    ← Önceki
                </button>
                <button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={isLast}
                    className="h-8 px-3 rounded bg-white border border-[#D1D5DB] text-xs font-semibold text-[#111827] hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                    Sonraki →
                </button>
            </div>
        </div>
    );
};