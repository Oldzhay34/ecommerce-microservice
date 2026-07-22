/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{ts,tsx}'],
    theme: {
        extend: {
            colors: {
                brand: '#1D4ED8',
                'brand-dark': '#1E40AF',
                ink: '#111827',
                muted: '#6B7280',
                faint: '#9CA3AF',
                line: '#E5E7EB',
                'line-strong': '#D1D5DB',
                canvas: '#F4F5F7',
                'err-tx': '#B91C1C',
                'err-bg': '#FEF2F2',
                'err-line': '#FCA5A5',
            },
            borderRadius: {
                card: '12px',
                btn: '8px',
            },
            boxShadow: {
                focus: '0 0 0 3px rgba(29,78,216,.12)',
            },
        },
    },
    plugins: [],
};