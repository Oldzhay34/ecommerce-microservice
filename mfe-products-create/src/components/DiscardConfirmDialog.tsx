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
                className="bg-white border border-line rounded-card w-full max-w-md p-6"
                onClick={(e) => e.stopPropagation()}
            >
                <h3 id="discard-title" className="text-ink text-base font-semibold">
                    Girdiğiniz bilgiler kaydedilmedi. Çıkmak istediğinize emin misiniz?
                </h3>

                <div className="flex justify-end gap-3 mt-6">
                    <button
                        type="button"
                        onClick={onCancel}
                        className="h-10 rounded-btn font-semibold bg-white border border-line-strong text-ink px-4"
                    >
                        Vazgeç
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        className="h-10 rounded-btn font-semibold bg-err-tx text-white px-4"
                    >
                        Çık
                    </button>
                </div>
            </div>
        </div>
    );
}