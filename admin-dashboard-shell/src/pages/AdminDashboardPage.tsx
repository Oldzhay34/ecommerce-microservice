import React, { useState } from 'react';
import { useSessionStore } from '../app/session/store';
import { TopBar } from '../components/TopBar';
import { TabBar } from '../components/TabBar';
import { RemoteMount } from '../components/RemoteMount';
import type { AdminSession } from '../types';

const AdminOrders = React.lazy(() => import('admin_mfe_orders/AdminOrders'));
const AdminOrderCount = React.lazy(() => import('admin_mfe_orders/AdminOrderCount'));

const AdminPayments = React.lazy(() => import('admin_mfe_payments/AdminPayments'));
const AdminRefundCount = React.lazy(() => import('admin_mfe_payments/AdminRefundCount'));

const AdminReviews = React.lazy(() => import('admin_mfe_reviews/AdminReviews'));
const AdminReviewCount = React.lazy(() => import('admin_mfe_reviews/AdminReviewCount'));

export const AdminDashboardPage: React.FC = () => {
    // Store'dan flat state'leri çek
    const authToken = useSessionStore((state) => state.authToken);
    const userId = useSessionStore((state) => state.userId);
    const role = useSessionStore((state) => state.role);

    const [activeTab, setActiveTab] = useState<'orders' | 'payments' | 'reviews'>('orders');

    // Guard zaten yakalayacak ama TS güvenlik önlemi:
    if (!authToken || !userId || role !== 'ADMIN') return null;

    // Remotelara iletmek üzere session objesini dinamik oluştur:
    const sessionProps: AdminSession = { authToken, userId, role };

    return (
        <div className="min-h-screen bg-canvas flex flex-col">
            <TopBar />
            <main className="flex-1 max-w-[1280px] w-full mx-auto px-6 md:px-8 py-12">
                <div className="mb-10">
                    <h1 className="text-3xl font-semibold tracking-tight text-ink-primary">Yönetim Paneli</h1>
                    <p className="text-base text-ink-secondary mt-2">Siparişleri, ödemeleri ve yorumları buradan yönetin.</p>
                </div>

                {/* Metrik Şeridi */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-10">
                    <RemoteMount>
                        <AdminOrderCount session={sessionProps} />
                    </RemoteMount>
                    <RemoteMount>
                        <AdminRefundCount session={sessionProps} />
                    </RemoteMount>
                    <RemoteMount>
                        <AdminReviewCount session={sessionProps} />
                    </RemoteMount>
                </div>

                {/* Sekme Seçici */}
                <TabBar
                    activeTab={activeTab}
                    onTabChange={setActiveTab}
                    orderCountNode={
                        <RemoteMount>
                            <AdminOrderCount session={sessionProps} />
                        </RemoteMount>
                    }
                    refundCountNode={
                        <RemoteMount>
                            <AdminRefundCount session={sessionProps} />
                        </RemoteMount>
                    }
                    reviewCountNode={
                        <RemoteMount>
                            <AdminReviewCount session={sessionProps} />
                        </RemoteMount>
                    }
                />

                {/* Aktif Sekme Paneli */}
                <div className="mt-6 bg-surface rounded-sb-lg p-6 md:p-8 shadow-sb">
                    {activeTab === 'orders' && (
                        <RemoteMount>
                            <AdminOrders session={sessionProps} />
                        </RemoteMount>
                    )}
                    {activeTab === 'payments' && (
                        <RemoteMount>
                            <AdminPayments session={sessionProps} />
                        </RemoteMount>
                    )}
                    {activeTab === 'reviews' && (
                        <RemoteMount>
                            <AdminReviews session={sessionProps} />
                        </RemoteMount>
                    )}
                </div>
            </main>
        </div>
    );
};