import { Suspense, type ReactNode } from 'react';
import { RemoteErrorBoundary } from './RemoteErrorBoundary';
import { SectionSkeleton } from './SectionSkeleton';

/**
 * Her remote'u Suspense (yükleme) + ErrorBoundary (dayanıklılık) ile sarar.
 * Yüklenemezse standart fallback kartı gösterilir.
 */
export function RemoteMount({ children }: { children: ReactNode }) {
    return (
        <RemoteErrorBoundary
            fallback={
                <div
                    style={{
                        background: '#FFFFFF',
                        border: '1px solid #E5E7EB',
                        borderRadius: 12,
                        padding: '20px 24px',
                        color: '#6B7280',
                        fontSize: 14,
                    }}
                    role="alert"
                >
                    Bu bölüm şu anda yüklenemedi. Sayfayı yenileyin.
                </div>
            }
        >
            <Suspense fallback={<SectionSkeleton />}>{children}</Suspense>
        </RemoteErrorBoundary>
    );
}