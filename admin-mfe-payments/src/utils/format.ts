export const formatTRY = (amount: number): string => {
    return new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency: 'TRY',
    }).format(amount);
};

export const shortId = (id: string): string => {
    if (!id) return '';
    return `${id.substring(0, 8)}...`;
};