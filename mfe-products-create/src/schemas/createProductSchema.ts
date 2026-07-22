import { z } from 'zod';

// [Varsayım: mesajlar Türkçe; backend @NotBlank/@Positive/@PositiveOrZero ile hizalı]
export const createProductSchema = z.object({
    name: z
        .string()
        .trim()
        .min(2, 'Ürün adı en az 2 karakter olmalıdır.')
        .max(120, 'Ürün adı en fazla 120 karakter olabilir.'),
    category: z
        .string()
        .trim()
        .min(2, 'Kategori en az 2 karakter olmalıdır.')
        .max(60, 'Kategori en fazla 60 karakter olabilir.'),
    price: z
        .number({ invalid_type_error: 'Geçerli bir fiyat girin.' })
        .positive('Fiyat sıfırdan büyük olmalıdır.'),
    stock: z
        .number({ invalid_type_error: 'Geçerli bir stok adedi girin.' })
        .int('Stok tam sayı olmalıdır.')
        .min(0, '0 veya daha büyük bir tam sayı girin.'),
});

export type CreateProductFormValues = z.infer<typeof createProductSchema>;