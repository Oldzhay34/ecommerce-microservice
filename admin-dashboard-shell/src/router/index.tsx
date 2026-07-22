import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { TokenCatcher } from '../components/TokenCatcher';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { useSessionStore } from '../app/session/store';

const AdminGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const authToken = useSessionStore((state) => state.authToken);
    const role = useSessionStore((state) => state.role);

    if (!authToken || role !== 'ADMIN') {
        return <Navigate to="/" replace />;
    }
    return <>{children}</>;
};

export const AppRouter: React.FC = () => {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<TokenCatcher />} />
                <Route
                    path="/admindashboard"
                    element={
                        <AdminGuard>
                            <AdminDashboardPage />
                        </AdminGuard>
                    }
                />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
};