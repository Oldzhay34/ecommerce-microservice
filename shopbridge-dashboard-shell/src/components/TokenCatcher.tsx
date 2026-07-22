import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { applyLogin } from '../app/auth/login';
import { useSessionStore } from '../app/session/store';

/**
 * Login projesi kullanıcıyı http://localhost:3001/?token=xxx adresine atar.
 * Bu bileşen token'ı URL'den alır, session'ı doldurur ve
 * rol STORE ise /storedashboard'a yönlendirir.
 */
export function TokenCatcher() {
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();
    const authToken = useSessionStore((s) => s.authToken);

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

    // Token yoksa ama session zaten varsa (yenileme sonrası) dashboard'a
    useEffect(() => {
        if (!searchParams.get('token') && authToken) {
            navigate('/storedashboard', { replace: true });
        }
    }, [authToken, navigate, searchParams]);

    return (
        <div style={{ maxWidth: 1120, margin: '0 auto', padding: 24 }}>
            <div style={{ fontWeight: 800, fontSize: 22 }}>
                <span style={{ color: '#000' }}>Shop</span>
                <span style={{ color: '#1D4ED8' }}>Bridge</span>
            </div>
            <p style={{ color: '#6B7280', marginTop: 12 }}>Oturum hazırlanıyor…</p>
        </div>
    );
}