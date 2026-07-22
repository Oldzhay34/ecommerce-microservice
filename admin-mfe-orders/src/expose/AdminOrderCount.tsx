import React from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useAdminOrderCount } from '../hooks/useAdminOrderCount';

const InnerAdminOrderCount: React.FC = () => {
    const { data, isLoading, isError } = useAdminOrderCount();

    if (isError) return <span>-</span>;
    if (isLoading) return <span className="sb-shimmer rounded h-4 w-6 inline-block" />;

    return <span>{data ?? 0}</span>;
};

const AdminOrderCount: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminOrderCount />
        </AuthTokenProvider>
    );
};

export default AdminOrderCount;