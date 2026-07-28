import React from 'react';

export const PlaceholderBox: React.FC = () => {
    return (
        <div
            className="w-full aspect-square bg-surface border border-border rounded-sb-lg flex flex-col items-center justify-center gap-2"
            style={{ aspectRatio: '1 / 1' }}
        >
            <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                stroke="#6C6C78"
                strokeWidth="1.5"
                fill="none"
                strokeLinecap="round"
                strokeLinejoin="round"
            >
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
            </svg>
            <span className="text-[13px] text-ink-faint text-center px-4">
        Görsel yakında eklenecek
      </span>
        </div>
    );
};