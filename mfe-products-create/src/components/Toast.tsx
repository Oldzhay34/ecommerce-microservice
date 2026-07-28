export function Toast({ message }: { message: string }) {
    return (
        <div
            role="status"
            className="fixed bottom-6 right-6 z-50 bg-surface-raised border border-border text-ink text-sm px-4 py-2 rounded-btn shadow-sb-lg"
        >
            {message}
        </div>
    );
}