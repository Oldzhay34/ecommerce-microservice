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
                    className="bg-surface rounded-sb-lg px-6 py-5 text-ink-muted text-sm shadow-sb"
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