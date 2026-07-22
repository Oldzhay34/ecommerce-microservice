const TRY_FORMATTER = new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

export function formatTRY(value: number): string {
    if (!Number.isFinite(value)) {
        return TRY_FORMATTER.format(0);
    }
    return TRY_FORMATTER.format(value);
}

export function shortId(id: string): string {
    if (typeof id !== 'string' || id.length === 0) {
        return '—';
    }
    if (id.length <= 8) {
        return id;
    }
    return `${id.slice(0, 8)}…`;
}