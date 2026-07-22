/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: '#1D4ED8', 'brand-dark': '#1E40AF',
        ink: '#111827', muted: '#6B7280', faint: '#9CA3AF',
        canvas: '#F4F5F7', line: '#E5E7EB', 'line-strong': '#D1D5DB',
        'ok-bg': '#ECFDF5', 'ok-tx': '#047857',
        'info-bg': '#EFF6FF', 'info-tx': '#1D4ED8',
        'warn-bg': '#FFFBEB', 'warn-tx': '#B45309',
        'err-bg': '#FEF2F2', 'err-tx': '#B91C1C',
        'neutral-bg': '#F3F4F6', 'neutral-tx': '#6B7280',
        'danger-line': '#FCA5A5',
        star: '#F59E0B',
      },
      borderRadius: { card: '12px', btn: '8px', pill: '999px' },
      maxWidth: { content: '1120px' },
    },
  },
  plugins: [],
};