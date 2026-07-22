// Pill rozet. Durum string render edilir, haritaya göre renklenir.
const GREEN = ['DELIVERED', 'COMPLETED', 'PAID', 'SUCCESS', 'APPROVED'];
const BLUE = ['SHIPPED', 'CONFIRMED', 'PROCESSING'];
const AMBER = ['CREATED', 'PENDING', 'AWAITING'];
const RED = ['CANCELLED', 'REFUNDED', 'FAILED', 'REJECTED'];

function classesFor(status: string): string {
    const s = status.toUpperCase();
    if (GREEN.includes(s)) return 'bg-ok-bg text-ok-tx';
    if (BLUE.includes(s)) return 'bg-info-bg text-info-tx';
    if (AMBER.includes(s)) return 'bg-warn-bg text-warn-tx';
    if (RED.includes(s)) return 'bg-err-bg text-err-tx';
    return 'bg-neutral-bg text-neutral-tx';
}

export function StatusBadge({ status }: { status: string }) {
    return (
        <span
            className={`inline-flex items-center rounded-pill px-2.5 py-0.5 text-xs font-semibold ${classesFor(status)}`}
        >
      {status}
    </span>
    );
}