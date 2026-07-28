import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSessionStore } from '../app/session/store';

export const TokenCatcher: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const setFromToken = useSessionStore((state) => state.setFromToken);
    const authToken = useSessionStore((state) => state.authToken);
    const role = useSessionStore((state) => state.role);

    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            try {
                setFromToken(token);
                // Doğrulama AdminGuard tarafından Route tarafında yapılacak.
                navigate('/admindashboard', { replace: true });
            } catch (err) {
                console.error('Token ayrıştırma hatası:', err);
            }
        } else if (authToken && role === 'ADMIN') {
            // Sayfa yenilenmiş, mevcut oturum zaten var — panele geri dön.
            navigate('/admindashboard', { replace: true });
        }
    }, [searchParams, setFromToken, navigate, authToken, role]);

    const hasToken = Boolean(searchParams.get('token'));
    if (!hasToken && !authToken) {
        const loginUrl = import.meta.env.VITE_AUTH_APP_URL ?? 'http://localhost:3000';
        return (
            <div className="flex flex-col items-center justify-center min-h-screen bg-canvas gap-5 px-6 text-center">
                <span className="text-xl font-bold tracking-tight">
                    <span className="text-ink-primary">Shop</span>
                    <span className="text-brand">Bridge</span>
                </span>
                <p className="text-sm text-ink-secondary max-w-sm">Yönetim paneline devam etmek için oturum açmanız gerekiyor.</p>
                <a
                    href={loginUrl}
                    className="inline-flex items-center justify-center h-10 px-6 rounded-full bg-brand text-white text-sm font-semibold hover:bg-brand-hover transition-colors"
                >
                    Giriş Yap
                </a>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-canvas">
            <div className="w-8 h-8 border-4 border-brand/30 border-t-brand rounded-full animate-spin"></div>
            <p className="mt-4 text-sm text-ink-secondary font-medium">Yönetici oturumu doğrulanıyor...</p>
        </div>
    );
};