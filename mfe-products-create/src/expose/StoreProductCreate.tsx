import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { ProductCreateForm } from '../features/ProductCreateForm';
import type { StoreSession } from '../types';

// Expose: token'ı iç context'e köprüler.
// Kendi QueryClientProvider'ını KURMAZ — shell'in singleton cache'ini kullanır.
export default function StoreProductCreate({
                                               session,
                                               onNavigate,
                                           }: {
    session: StoreSession;
    onNavigate?: (path: string) => void;
}) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <ProductCreateForm session={session} onNavigate={onNavigate} />
        </AuthTokenProvider>
    );
}