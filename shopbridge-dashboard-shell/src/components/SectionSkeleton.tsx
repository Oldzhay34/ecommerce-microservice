/**
 * Remote yüklenirken gösterilen bölüm iskeleti (Suspense fallback).
 */
export function SectionSkeleton() {
    return (
        <div
            style={{
                background: '#FFFFFF',
                border: '1px solid #E5E7EB',
                borderRadius: 12,
                padding: '20px 24px',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
            }}
            aria-busy="true"
            aria-live="polite"
        >
            {[0, 1, 2].map((i) => (
                <div
                    key={i}
                    style={{
                        height: 44,
                        borderRadius: 8,
                        background:
                            'linear-gradient(90deg,#F3F4F6 25%,#E5E7EB 37%,#F3F4F6 63%)',
                        backgroundSize: '400% 100%',
                        animation: 'sb-shimmer 1.4s ease infinite',
                    }}
                />
            ))}
            <style>{`@keyframes sb-shimmer{0%{background-position:100% 0}100%{background-position:-100% 0}}`}</style>
        </div>
    );
}