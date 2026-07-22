import axios from 'axios';
import { getAccessToken } from '../auth/readAuthContract';

export const apiClient = axios.create({
    // Artık istekler direkt API Gateway'e gidiyor
    baseURL: 'http://localhost:8080',
});

apiClient.interceptors.request.use((config) => {
    const token = getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});