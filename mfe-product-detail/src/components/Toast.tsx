import { useEffect } from 'react';

/**
 * Presenter · veri çekmez.
 * Theo Mandel · Kullanıcıyı Kontrolde Tut: otomatik yönlendirme yoktur;
 * kullanıcı isterse "Sepete Git" bağlantısıyla gider.
 */
export interface ToastProps {
    message: string;
    actionLabel: string;
    onAction: () => void;
    onDismiss: () => void;
    durationMs?: number;
}

export function Toast({
                          message,
                          actionLabel,
                          onAction,
                          onDismiss,
                          durationMs = 2500,
                      }: ToastProps) {
    useEffect(() => {
        const timer = window.setTimeout(() => {
            onDismiss();
        }, durationMs);

        return () => {
            window.clearTimeout(timer);
        };
    }, [durationMs, onDismiss]);

    return (
        <div
            role="status"
            aria-live="polite"
            style={{
                position: 'fixed',
                bottom: 24,
                right: 24,
                zIndex: 50,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                backgroundColor: '#1E1E25',
                color: '#F2F2F5',
                fontSize: 14,
                padding: '8px 16px',
                borderRadius: 10,
                border: '1px solid #2E2E36',
                boxShadow: '0 12px 32px -8px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04)',
            }}
        >
            <span>{message}</span>
            <button
                type="button"
                onClick={onAction}
                style={{
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    fontSize: 14,
                    fontWeight: 600,
                    color: '#5C87F8',
                    textDecoration: 'underline',
                    cursor: 'pointer',
                }}
            >
                {actionLabel}
            </button>
        </div>
    );
}