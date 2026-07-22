export function FormErrorBanner({ message }: { message: string }) {
    return (
        <div
            role="alert"
            className="bg-err-bg border border-err-line text-err-tx rounded-btn px-4 py-3 text-sm mb-6"
        >
            {message}
        </div>
    );
}