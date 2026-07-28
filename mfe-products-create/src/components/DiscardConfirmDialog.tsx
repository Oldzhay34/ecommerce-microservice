export function DiscardConfirmDialog({
                                         open,
                                         onCancel,
                                         onConfirm,
                                     }: {
    open: boolean;
    onCancel: () => void;
    onConfirm: () => void;
}) {
    if (!open) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
            role="dialog"
            aria-modal="true"
            aria-labelledby="discard-title"
            onClick={onCancel}
        >
            <div
                className="bg-surface-raised border border-border-strong shadow-sb-lg rounded-card w-full max-w-md p-6"
                onClick={(e) => e.stopPropagation()}
            >
                <h3 id="discard-title" className="text-ink text-base font-semibold">
                    Girdiğiniz bilgiler kaydedilmedi. Çıkmak istediğinize emin misiniz?
                </h3>

                <div className="flex justify-end gap-3 mt-6">
                    <button
                        type="button"
                        onClick={onCancel}
                        className="h-10 rounded-btn font-semibold bg-surface border border-border-strong text-ink px-4 hover:bg-surface-hover"
                    >
                        Vazgeç
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        className="h-10 rounded-btn font-semibold bg-danger text-white px-4 hover:brightness-110"
                    >
                        Çık
                    </button>
                </div>
            </div>
        </div>
    );
}