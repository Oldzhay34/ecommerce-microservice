import React from 'react';

interface TabBarProps {
    activeTab: 'orders' | 'payments' | 'reviews';
    onTabChange: (tab: 'orders' | 'payments' | 'reviews') => void;
    orderCountNode: React.ReactNode;
    refundCountNode: React.ReactNode;
    reviewCountNode: React.ReactNode;
}

export const TabBar: React.FC<TabBarProps> = ({
                                                  activeTab,
                                                  onTabChange,
                                                  orderCountNode,
                                                  refundCountNode,
                                                  reviewCountNode,
                                              }) => {
    return (
        <div className="w-full border-b border-[#E5E7EB] flex items-center space-x-2">
            <button
                onClick={() => onTabChange('orders')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'orders'
                        ? 'text-[#1D4ED8] border-[#1D4ED8]'
                        : 'text-[#6B7280] border-transparent hover:text-[#111827]'
                }`}
            >
                <span>Siparişler</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'orders' ? 'bg-[#EFF6FF] text-[#1D4ED8]' : 'bg-gray-100 text-[#6B7280]'
                }`}>
          {orderCountNode}
        </span>
            </button>

            <button
                onClick={() => onTabChange('payments')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'payments'
                        ? 'text-[#1D4ED8] border-[#1D4ED8]'
                        : 'text-[#6B7280] border-transparent hover:text-[#111827]'
                }`}
            >
                <span>Ödemeler</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'payments' ? 'bg-[#EFF6FF] text-[#1D4ED8]' : 'bg-gray-100 text-[#6B7280]'
                }`}>
          {refundCountNode}
        </span>
            </button>

            <button
                onClick={() => onTabChange('reviews')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'reviews'
                        ? 'text-[#1D4ED8] border-[#1D4ED8]'
                        : 'text-[#6B7280] border-transparent hover:text-[#111827]'
                }`}
            >
                <span>Yorumlar</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'reviews' ? 'bg-[#EFF6FF] text-[#1D4ED8]' : 'bg-gray-100 text-[#6B7280]'
                }`}>
          {reviewCountNode}
        </span>
            </button>
        </div>
    );
};