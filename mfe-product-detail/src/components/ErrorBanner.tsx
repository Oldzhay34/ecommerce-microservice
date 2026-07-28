import type { ReactNode } from 'react';

/**
 * Presenter · veri çekmez.
 * Pressman · Error Information Handling: hata, ilgili eylemin yanında gösterilir.
 */
export interface ErrorBannerProps {
    message: string;
    children?: ReactNode;
}

export function ErrorBanner({ message, children }: ErrorBannerProps) {
    return (
        <div>
            <div
                role="alert"
                className="bg-danger/10 text-danger"
                style={{
                    border: '1px solid rgba(241,80,60,0.4)',
                    borderRadius: 10,
                    padding: '12px 16px',
                    fontSize: 14,
                    lineHeight: 1.45,
                    wordBreak: 'break-word',
                }}
            >
                {message}
            </div>
            {children ? <div style={{ marginTop: 12 }}>{children}</div> : null}
        </div>
    );
}