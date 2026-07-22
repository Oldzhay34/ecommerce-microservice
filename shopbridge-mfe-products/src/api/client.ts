import axios from 'axios';
import { API_BASE_URL } from './endpoints';

export const apiClient = axios.create({
    baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
    try {
        const token = localStorage.getItem('shopbridge_access_token');
        if (token) config.headers.Authorization = `Bearer ${token}`;
    } catch {
        // localStorage erişilemezse sessizce geç
    }
    return config;
});