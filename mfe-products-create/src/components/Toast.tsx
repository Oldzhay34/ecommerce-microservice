export function Toast({ message }: { message: string }) {
    return (
        <div
            role="status"
            className="fixed bottom-6 right-6 z-50 bg-ink text-white text-sm px-4 py-2 rounded-btn shadow-lg"
        >
            {message}
        </div>
    );
}