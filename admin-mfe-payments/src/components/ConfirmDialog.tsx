import React from 'react';

interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    description: string;
    onConfirm: () => void;
    onCancel: () => void;
    confirmLabel: string;
    isDanger?: boolean;
    isLoading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
                                                                isOpen,
                                                                title,
                                                                description,
                                                                onConfirm,
                                                                onCancel,
                                                                confirmLabel,
                                                                isDanger = false,
                                                                isLoading = false,
                                                            }) => {
    if (!isOpen) return null;

    return (
        <div
            onClick={onCancel}
            className="fixed inset-0 z-50 bg-gray-900/45 flex items-center justify-center p-4"
            role="dialog"
            aria-modal="true"
        >
            <div
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-[420px] bg-white border border-[#E5E7EB] rounded-xl p-6 md:p-7 shadow-xl"
            >
                <h3 className="text-base font-semibold text-[#111827]">{title}</h3>
                <p className="text-sm text-[#6B7280] mt-2 leading-relaxed">{description}</p>
                <div className="flex items-center justify-end space-x-3 mt-6">
                    <button
                        onClick={onCancel}
                        disabled={isLoading}
                        className="h-10 px-4 rounded-lg bg-white border border-[#D1D5DB] text-[#111827] text-sm font-semibold hover:bg-gray-50 transition-colors disabled:opacity-50"
                    >
                        Vazgeç
                    </button>
                    <button
                        onClick={onConfirm}
                        disabled={isLoading}
                        className={`h-10 px-4 rounded-lg text-white text-sm font-semibold flex items-center justify-center space-x-2 transition-colors disabled:opacity-50 ${
                            isDanger ? 'bg-[#B91C1C] hover:bg-[#991B1B]' : 'bg-[#1D4ED8] hover:bg-[#1E40AF]'
                        }`}
                    >
                        <span>{confirmLabel}</span>
                    </button>
                </div>
            </div>
        </div>
    );
};