/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
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
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
            },
            borderRadius: {
                sb: '10px',
                'sb-lg': '16px',
            },
            boxShadow: {
                sb: '0 1px 2px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.03)',
                'sb-lg': '0 12px 32px -8px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04)',
            },
            keyframes: {
                'sb-shimmer': {
                    '0%': { backgroundPosition: '100% 0' },
                    '100%': { backgroundPosition: '-100% 0' },
                },
            },
            animation: {
                'sb-shimmer': 'sb-shimmer 1.6s ease infinite',
            },
        },
    },
    plugins: [],
}