/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // ShopBridge premium light palette — values updated in place,
        // existing token names kept so component classNames stay unchanged.
        brand: '#1D4ED8', 'brand-dark': '#1E40AF',
        surface: { DEFAULT: '#FFFFFF', hover: '#F7F8FA', raised: '#FFFFFF' },
        ink: '#0A0A0A', muted: '#6B7280', faint: '#9CA3AF',
        canvas: '#FFFFFF', line: '#E5E7EB', 'line-strong': '#D1D5DB',
        'ok-bg': '#ECFDF5', 'ok-tx': '#16A34A',
        'info-bg': '#EFF6FF', 'info-tx': '#1D4ED8',
        'warn-bg': '#FFFBEB', 'warn-tx': '#D97706',
        'err-bg': '#FEF2F2', 'err-tx': '#DC2626',
        'neutral-bg': '#F3F4F6', 'neutral-tx': '#6B7280',
        'danger-line': 'rgba(220,38,38,0.4)',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: { card: '12px', btn: '8px', pill: '999px' },
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
      maxWidth: { content: '1120px' },
    },
  },
  plugins: [],
};