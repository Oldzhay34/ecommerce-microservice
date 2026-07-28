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
        <div className="w-full border-b border-line-card flex items-center space-x-2">
            <button
                onClick={() => onTabChange('orders')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'orders'
                        ? 'text-brand border-brand'
                        : 'text-ink-secondary border-transparent hover:text-ink-primary'
                }`}
            >
                <span>Siparişler</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'orders' ? 'bg-brand-soft text-brand' : 'bg-surface text-ink-secondary'
                }`}>
          {orderCountNode}
        </span>
            </button>

            <button
                onClick={() => onTabChange('payments')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'payments'
                        ? 'text-brand border-brand'
                        : 'text-ink-secondary border-transparent hover:text-ink-primary'
                }`}
            >
                <span>Ödemeler</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'payments' ? 'bg-brand-soft text-brand' : 'bg-surface text-ink-secondary'
                }`}>
          {refundCountNode}
        </span>
            </button>

            <button
                onClick={() => onTabChange('reviews')}
                className={`h-11 px-5 text-sm font-semibold border-b-2 transition-all flex items-center ${
                    activeTab === 'reviews'
                        ? 'text-brand border-brand'
                        : 'text-ink-secondary border-transparent hover:text-ink-primary'
                }`}
            >
                <span>Yorumlar</span>
                <span className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded-full ${
                    activeTab === 'reviews' ? 'bg-brand-soft text-brand' : 'bg-surface text-ink-secondary'
                }`}>
          {reviewCountNode}
        </span>
            </button>
        </div>
    );
};