import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import './index.css';

// --- TOKEN KÖPRÜSÜ ---
// Login uygulamasi (3000) token'i URL query param'i ile gonderir.
// Dashboard (5000) ayri origin oldugu icin localStorage paylasilmaz;
// token'i URL'den alip kendi localStorage'imiza yaziyoruz, sonra URL'i temizliyoruz.
const params = new URLSearchParams(window.location.search);
const tokenFromUrl = params.get('token');
if (tokenFromUrl) {
    localStorage.setItem('shopbridge_access_token', tokenFromUrl);
    // Token'i adres cubugundan/gecmisten temizle
    window.history.replaceState({}, document.title, window.location.pathname);
}
// --- /TOKEN KÖPRÜSÜ ---

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);