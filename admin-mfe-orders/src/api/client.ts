import axios, { AxiosInstance } from 'axios';
import { ApiErrorResponse } from '../types';

export const createApiClient = (tokenGetter: () => string | null): AxiosInstance => {
    const instance = axios.create();

    instance.interceptors.request.use((config) => {
        const token = tokenGetter();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    });

    instance.interceptors.response.use(
        (response) => response,
        (error) => {
            let message = 'Bir hata oluştu.';
            if (error.response?.data) {
                const errorData = error.response.data as ApiErrorResponse;
                message = errorData.message || message;
            }
            return Promise.reject(new Error(message));
        }
    );

    return instance;
};