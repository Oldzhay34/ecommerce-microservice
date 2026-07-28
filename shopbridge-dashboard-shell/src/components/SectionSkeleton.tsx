/**
 * Remote yüklenirken gösterilen bölüm iskeleti (Suspense fallback).
 */
export function SectionSkeleton() {
    return (
        <div
            className="bg-surface rounded-sb-lg p-6 flex flex-col gap-3 shadow-sb"
            aria-busy="true"
            aria-live="polite"
        >
            {[0, 1, 2].map((i) => (
                <div
                    key={i}
                    className="h-11 rounded-sb bg-gradient-to-r from-surface-hover via-surface-raised to-surface-hover bg-[length:400%_100%] animate-sb-shimmer"
                />
            ))}
        </div>
    );
}
