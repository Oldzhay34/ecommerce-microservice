import React from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useRefundCount } from '../hooks/useRefundCount';

const InnerAdminRefundCount: React.FC = () => {
    const { data, isLoading, isError } = useRefundCount();

    if (isError) return <span>-</span>;
    if (isLoading) return <span className="sb-shimmer rounded h-4 w-6 inline-block" />;

    return <span>{data ?? 0}</span>;
};

const AdminRefundCount: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminRefundCount />
        </AuthTokenProvider>
    );
};

export default AdminRefundCount;