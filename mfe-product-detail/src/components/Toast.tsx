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
                backgroundColor: '#111827',
                color: '#FFFFFF',
                fontSize: 14,
                padding: '8px 16px',
                borderRadius: 8,
                boxShadow: '0 10px 15px -3px rgba(0,0,0,0.25), 0 4px 6px -4px rgba(0,0,0,0.25)',
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
                    color: '#93C5FD',
                    textDecoration: 'underline',
                    cursor: 'pointer',
                }}
            >
                {actionLabel}
            </button>
        </div>
    );
}