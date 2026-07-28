export function ProductGridSkeleton() {
    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[...Array(8)].map((_, i) => (
                <div key={i} className="h-40 bg-surface animate-pulse rounded-sb-lg border border-border" />
            ))}
        </div>
    );
}