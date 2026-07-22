import axios from 'axios';
import { getAccessToken } from '../auth/readAuthContract';

export const apiClient = axios.create({
    baseURL: 'http://localhost:8080', // [API Gateway]
});

apiClient.interceptors.request.use((config) => {
    const token = getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});