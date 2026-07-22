import { z } from 'zod';

// Zod: 0 veya daha büyük tam sayı
export const stockSchema = z.object({
    stock: z
        .number({ invalid_type_error: '0 veya daha büyük bir tam sayı girin.' })
        .int('0 veya daha büyük bir tam sayı girin.')
        .min(0, '0 veya daha büyük bir tam sayı girin.'),
});

export type StockFormValues = z.infer<typeof stockSchema>;