export function ProductGridSkeleton() {
    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[...Array(8)].map((_, i) => (
                <div key={i} className="h-40 bg-gray-200 animate-pulse rounded-lg border border-gray-100" />
            ))}
        </div>
    );
}