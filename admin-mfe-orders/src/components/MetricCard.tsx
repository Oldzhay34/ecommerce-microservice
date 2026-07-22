import React from 'react';

interface MetricCardProps {
    title: string;
    value: string | number;
    isLoading?: boolean;
}

export const MetricCard: React.FC<MetricCardProps> = ({ title, value, isLoading = false }) => {
    return (
        <div className="bg-white border border-[#E5E7EB] rounded-xl p-5 md:p-6 shadow-sm min-h-[108px] flex flex-col justify-center">
            <h3 className="text-[13px] font-semibold tracking-wider text-[#6B7280] uppercase">
                {title}
            </h3>
            {isLoading ? (
                <div className="h-8 w-24 sb-shimmer rounded mt-2" />
            ) : (
                <p className="text-2xl font-bold text-[#111827] mt-2 leading-none">{value}</p>
            )}
        </div>
    );
};