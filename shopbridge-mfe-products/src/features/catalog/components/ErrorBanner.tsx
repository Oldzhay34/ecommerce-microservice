export function ErrorBanner({ message }: { message: string }) {
    return (
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm text-center">
            {message}
        </div>
    );
}