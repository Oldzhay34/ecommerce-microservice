import type { Config } from 'tailwindcss';

export default {
    darkMode: 'class',
    content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
            },
            colors: {
                canvas: '#FFFFFF',
                surface: {
                    DEFAULT: '#FFFFFF',
                    hover: '#F7F8FA',
                    raised: '#FFFFFF',
                },
                border: {
                    DEFAULT: '#E5E7EB',
                    strong: '#D1D5DB',
                },
                ink: {
                    DEFAULT: '#0A0A0A',
                    muted: '#6B7280',
                    faint: '#9CA3AF',
                },
                brand: {
                    DEFAULT: '#1D4ED8',
                    hover: '#1E40AF',
                    soft: '#EFF6FF',
                },
                success: { DEFAULT: '#16A34A', soft: '#ECFDF5' },
                warning: { DEFAULT: '#D97706', soft: '#FFFBEB' },
                danger: { DEFAULT: '#DC2626', soft: '#FEF2F2' },
            },
            borderRadius: {
                sb: '12px',
                'sb-lg': '20px',
            },
            boxShadow: {
                sb: '0 1px 2px rgba(15,15,15,0.06), 0 1px 1px rgba(15,15,15,0.04)',
                'sb-lg': '0 16px 32px -12px rgba(15,15,15,0.14), 0 2px 4px rgba(15,15,15,0.04)',
                'sb-glow': '0 0 0 1px rgba(29,78,216,0.35), 0 8px 20px -6px rgba(29,78,216,0.25)',
            },
            keyframes: {
                'sb-shimmer': {
                    '0%': { backgroundPosition: '100% 0' },
                    '100%': { backgroundPosition: '-100% 0' },
                },
                'sb-fade-in': {
                    '0%': { opacity: '0', transform: 'translateY(4px)' },
                    '100%': { opacity: '1', transform: 'translateY(0)' },
                },
            },
            animation: {
                'sb-shimmer': 'sb-shimmer 1.6s ease infinite',
                'sb-fade-in': 'sb-fade-in 0.25s ease-out',
            },
        },
    },
    plugins: [],
} satisfies Config;
