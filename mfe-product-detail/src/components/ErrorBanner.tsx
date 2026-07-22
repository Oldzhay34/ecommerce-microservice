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
                style={{
                    backgroundColor: '#FEF2F2',
                    border: '1px solid #FCA5A5',
                    color: '#B91C1C',
                    borderRadius: 8,
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