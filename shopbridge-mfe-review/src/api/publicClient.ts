import axios from 'axios';

/**
 * Token'sız istemci — ürün detay ekranı public'tir.
 *
 * Neden ayrı: mevcut apiClient interceptor'da localStorage'dan token okur ve
 * Authorization header'ı ekler. Ürün detayı token'sız da çalışmalıdır (galeri gibi).
 * Token varsa da göndermeye gerek yok — endpoint public.
 */
export const publicApiClient = axios.create({
    baseURL: 'http://localhost:8080', // [Api Gateway]
});