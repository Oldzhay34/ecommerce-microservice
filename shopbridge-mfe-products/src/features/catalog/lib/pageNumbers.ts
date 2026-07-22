// 7'den fazla sayfada kısaltma deseni: 1 … 4 5 6 … 20
export function buildPageNumbers(current: number, total: number): (number | '…')[] {
    if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i + 1);
    }
    const pages: (number | '…')[] = [];
    const left = Math.max(2, current - 1);
    const right = Math.min(total - 1, current + 1);

    pages.push(1);
    if (left > 2) pages.push('…');
    for (let i = left; i <= right; i++) pages.push(i);
    if (right < total - 1) pages.push('…');
    pages.push(total);

    return pages;
}