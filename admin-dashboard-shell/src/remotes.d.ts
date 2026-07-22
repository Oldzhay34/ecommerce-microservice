declare module 'admin_mfe_orders/AdminOrders' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminOrders: React.ComponentType<{ session: AdminSession }>;
    export default AdminOrders;
}

declare module 'admin_mfe_orders/AdminOrderCount' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminOrderCount: React.ComponentType<{ session: AdminSession }>;
    export default AdminOrderCount;
}

declare module 'admin_mfe_payments/AdminPayments' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminPayments: React.ComponentType<{ session: AdminSession }>;
    export default AdminPayments;
}

declare module 'admin_mfe_payments/AdminRefundCount' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminRefundCount: React.ComponentType<{ session: AdminSession }>;
    export default AdminRefundCount;
}

declare module 'admin_mfe_reviews/AdminReviews' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminReviews: React.ComponentType<{ session: AdminSession }>;
    export default AdminReviews;
}

declare module 'admin_mfe_reviews/AdminReviewCount' {
    import React from 'react';
    interface AdminSession {
        authToken: string;
        userId: string;
        role: 'ADMIN';
    }
    const AdminReviewCount: React.ComponentType<{ session: AdminSession }>;
    export default AdminReviewCount;
}