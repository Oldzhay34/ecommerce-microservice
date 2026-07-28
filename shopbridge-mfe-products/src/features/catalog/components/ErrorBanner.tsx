export function ErrorBanner({ message }: { message: string }) {
    return (
        <div className="p-4 rounded-sb-lg bg-danger/10 border border-danger/20 text-danger text-sm text-center">
            {message}
        </div>
    );
}