// Geri dönülebilir onay (kullanıcıyı kontrolde tut).
export function ConfirmDialog({
                                  message, confirmLabel, danger, isPending, onConfirm, onCancel,
                              }: {
    message: string;
    confirmLabel: string;
    danger?: boolean;
    isPending: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4" role="dialog" aria-modal="true">
    <div className="bg-white border border-line rounded-card w-full max-w-sm p-6">
    <p className="text-ink text-sm">{message}</p>
        <div className="flex justify-end gap-3 mt-6">
    <button
        onClick={onCancel}
    disabled={isPending}
    className="h-10 rounded-btn font-semibold bg-white border border-line-strong text-ink px-4 disabled:opacity-50 disabled:cursor-not-allowed"
        >
        Vazgeç
        </button>
        <button
    onClick={onConfirm}
    disabled={isPending}
    className={`h-10 rounded-btn font-semibold px-4 inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed ${
        danger
            ? 'bg-white border border-danger-line text-err-tx'
            : 'bg-brand text-white hover:bg-brand-dark'
    }`}
>
    {isPending && (
        <span className="w-4 h-4 border-2 border-current/40 border-t-current rounded-full animate-spin" />
    )}
    {confirmLabel}
    </button>
    </div>
    </div>
    </div>
);
}