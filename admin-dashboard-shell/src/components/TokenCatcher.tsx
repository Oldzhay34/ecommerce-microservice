import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSessionStore } from '../app/session/store';

export const TokenCatcher: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const setFromToken = useSessionStore((state) => state.setFromToken);

    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            try {
                setFromToken(token);
                // Doğrulama AdminGuard tarafından Route tarafında yapılacak.
                navigate('/admindashboard', { replace: true });
            } catch (err) {
                console.error('Token ayrıştırma hatası:', err);
                navigate('/', { replace: true });
            }
        } else {
            navigate('/', { replace: true });
        }
    }, [searchParams, setFromToken, navigate]);

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#F4F5F7]">
            <div className="w-8 h-8 border-4 border-[#1D4ED8]/30 border-t-[#1D4ED8] rounded-full animate-spin"></div>
            <p className="mt-4 text-sm text-[#6B7280] font-medium">Yönetici oturumu doğrulanıyor...</p>
        </div>
    );
};