import { createBrowserRouter, Navigate } from 'react-router-dom';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { RegisterPage } from '../features/auth/pages/RegisterPage';
import { OtpVerifyPage } from '../features/auth/pages/OtpVerifyPage';
import { ForgotPasswordPage } from '../features/auth/pages/ForgotPasswordPage'; // <-- Şifremi Unuttum sayfası import edildi

export const router = createBrowserRouter([
    {
        path: '/',
        element: <Navigate to="/login" replace />,
    },
    {
        path: '/login',
        element: <LoginPage />,
    },
    {
        path: '/register',
        element: <RegisterPage />,
    },
    {
        path: '/verify-otp',
        element: <OtpVerifyPage />,
    },
    {
        path: '/forgot-password', // <-- Rota eklendi
        element: <ForgotPasswordPage />,
    },
]);