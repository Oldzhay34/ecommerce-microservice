export function Toast({ message }: { message: string }) {
    return (
        <div className="fixed bottom-6 right-6 z-50 bg-surface-raised text-ink text-sm px-4 py-2 rounded-btn border border-line-strong shadow-sb-lg">
            {message}
        </div>
    );
}