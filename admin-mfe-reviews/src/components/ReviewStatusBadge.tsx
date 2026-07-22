import React from 'react';

interface ReviewStatusBadgeProps {
    status: string;
}

export const ReviewStatusBadge: React.FC<ReviewStatusBadgeProps> = ({ status }) => {
    let classes = 'bg-gray-100 text-[#4B5563]';
    let label = status;

    switch (status) {
        case 'ACTIVE':
            classes = 'bg-[#D1FAE5] text-[#065F46]';
            label = 'Yayında';
            break;
        case 'HIDDEN':
            classes = 'bg-gray-100 text-[#4B5563]';
            label = 'Gizli';
            break;
    }

    return (
        <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`}>
      {label}
    </span>
    );
};