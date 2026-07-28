/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // ShopBridge premium light palette (mapped onto this package's existing token names)
        brand: '#1D4ED8', 'brand-dark': '#1E40AF',
        ink: '#0A0A0A', muted: '#6B7280', faint: '#9CA3AF',
        canvas: '#FFFFFF', line: '#E5E7EB', 'line-strong': '#D1D5DB',
        surface: {
          DEFAULT: '#FFFFFF',
          hover: '#F7F8FA',
          raised: '#FFFFFF',
        },
        success: { DEFAULT: '#16A34A', soft: 'rgba(22,163,74,0.12)' },
        warning: { DEFAULT: '#D97706', soft: 'rgba(217,119,6,0.12)' },
        danger: { DEFAULT: '#DC2626', soft: 'rgba(220,38,38,0.12)' },
        'ok-bg': 'rgba(22,163,74,0.12)', 'ok-tx': '#16A34A',
        'info-bg': 'rgba(29,78,216,0.12)', 'info-tx': '#1D4ED8',
        'warn-bg': 'rgba(217,119,6,0.12)', 'warn-tx': '#D97706',
        'err-bg': 'rgba(220,38,38,0.12)', 'err-tx': '#DC2626',
        'neutral-bg': '#F3F4F6', 'neutral-tx': '#6B7280',
        'danger-line': '#DC2626',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: { card: '12px', btn: '8px', pill: '999px', sb: '10px', 'sb-lg': '16px' },
      boxShadow: {
        sb: '0 1px 2px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.03)',
        'sb-lg': '0 12px 32px -8px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04)',
      },
      maxWidth: { content: '1120px' },
    },
  },
  plugins: [],
};