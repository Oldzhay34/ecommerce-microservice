import React from 'react';
import { OrderStatus } from '../types';

interface OrderStatusBadgeProps {
    status: OrderStatus;
}

export const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({ status }) => {
    let classes = 'bg-gray-100 text-[#4B5563]';
    let label = status as string;

    switch (status) {
        case 'PENDING':
            classes = 'bg-[#FEF3C7] text-[#92400E]';
            label = 'Beklemede';
            break;
        case 'APPROVED':
            classes = 'bg-[#DBEAFE] text-[#1E40AF]';
            label = 'Onaylandı';
            break;
        case 'SHIPPED':
            classes = 'bg-[#D1FAE5] text-[#065F46]';
            label = 'Kargolandı';
            break;
        case 'CANCELLED':
            classes = 'bg-[#FEE2E2] text-[#991B1B]';
            label = 'İptal Edildi';
            break;
    }

    return (
        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`}>
      {label}
    </span>
    );
};