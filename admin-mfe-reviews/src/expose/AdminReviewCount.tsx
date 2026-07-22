import React from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useReviewCount } from '../hooks/useReviewCount';

const InnerAdminReviewCount: React.FC = () => {
    const { data, isLoading, isError } = useReviewCount();

    if (isError) return <span>-</span>;
    if (isLoading) return <span className="sb-shimmer rounded h-4 w-6 inline-block" />;

    return <span>{data ?? 0}</span>;
};

const AdminReviewCount: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminReviewCount />
        </AuthTokenProvider>
    );
};

export default AdminReviewCount;