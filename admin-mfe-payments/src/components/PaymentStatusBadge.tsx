import React from 'react';

interface PaymentStatusBadgeProps {
    status: string | null;
}

export const PaymentStatusBadge: React.FC<PaymentStatusBadgeProps> = ({ status }) => {
    if (!status) {
        return (
            <span className="inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase bg-gray-100 text-[#4B5563]">
        --
      </span>
        );
    }

    let classes = 'bg-gray-100 text-[#4B5563]';
    let label = status;

    switch (status) {
        case 'PENDING':
            classes = 'bg-[#FEF3C7] text-[#92400E]';
            label = 'Beklemede';
            break;
        case 'COMPLETED':
            classes = 'bg-[#D1FAE5] text-[#065F46]';
            label = 'Tamamlandı';
            break;
        case 'FAILED':
            classes = 'bg-[#FEE2E2] text-[#991B1B]';
            label = 'Başarısız';
            break;
        case 'REFUND_REQUESTED':
            classes = 'bg-[#FFEDD5] text-[#9A3412]';
            label = 'İade Talebi';
            break;
        case 'REFUNDED':
            classes = 'bg-[#E0E7FF] text-[#3730A3]';
            label = 'İade Edildi';
            break;
    }

    return (
        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`}>
      {label}
    </span>
    );
};