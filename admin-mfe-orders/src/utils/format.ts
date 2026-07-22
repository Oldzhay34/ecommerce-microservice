export const formatTRY = (amount: number): string => {
    return new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency: 'TRY',
    }).format(amount);
};

export const formatDate = (isoString: string): string => {
    return new Intl.DateTimeFormat('tr-TR', {
        dateStyle: 'short',
        timeStyle: 'short',
    }).format(new Date(isoString));
};

export const shortId = (id: string): string => {
    if (!id) return '';
    return `${id.substring(0, 8)}...`;
};