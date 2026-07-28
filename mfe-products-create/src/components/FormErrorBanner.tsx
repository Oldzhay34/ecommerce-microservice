export function FormErrorBanner({ message }: { message: string }) {
    return (
        <div
            role="alert"
            className="bg-danger/10 border border-danger/30 text-danger rounded-btn px-4 py-3 text-sm mb-6"
        >
            {message}
        </div>
    );
}