/**
 * Presenter · veri çekmez.
 * Mutasyon sırasında butonun içinde, etiketin solunda gösterilir.
 */
export function Spinner() {
    return (
        <span
            aria-hidden="true"
            className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin"
        />
    );
}