import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { applyLogin } from '../app/auth/login';
import { useSessionStore } from '../app/session/store';
import { Logo } from './Logo';

/**
 * Login projesi kullanıcıyı http://localhost:3001/?token=xxx adresine atar.
 * Bu bileşen token'ı URL'den alır, session'ı doldurur ve
 * rol STORE ise /storedashboard'a yönlendirir.
 */
export function TokenCatcher() {
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();
    const { authToken, role } = useSessionStore();

    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            const { redirectTo } = applyLogin({ accessToken: token });
            // token'ı URL'den temizle (adres çubuğunda kalmasın)
            searchParams.delete('token');
            setSearchParams(searchParams, { replace: true });
            navigate(redirectTo, { replace: true });
        }
    }, [searchParams, setSearchParams, navigate]);

    // Token yoksa ama session zaten varsa (yenileme sonrası) role'e göre dashboard'a
    useEffect(() => {
        if (!searchParams.get('token') && authToken) {
            navigate(role === 'STORE' ? '/storedashboard' : '/dashboard', { replace: true });
        }
    }, [authToken, role, navigate, searchParams]);

    const hasToken = Boolean(searchParams.get('token'));
    if (!hasToken && !authToken) {
        const loginUrl = import.meta.env.VITE_AUTH_APP_URL ?? 'http://localhost:3000';
        return (
            <div className="min-h-screen bg-canvas flex flex-col items-center justify-center gap-5 px-6 text-center">
                <Logo size={26} />
                <p className="text-ink-muted text-sm max-w-sm">
                    Devam etmek için oturum açmanız gerekiyor.
                </p>
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
        <div className="min-h-screen bg-canvas flex flex-col items-center justify-center gap-4">
            <Logo size={26} />
            <div className="flex items-center gap-2 text-ink-muted text-sm">
                <span className="h-3.5 w-3.5 rounded-full border-2 border-border-strong border-t-brand animate-spin" />
                Oturum hazırlanıyor…
            </div>
        </div>
    );
}