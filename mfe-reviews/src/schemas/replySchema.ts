import { z } from 'zod';

// Zod: en az 2 karakter
export const replySchema = z.object({
    reply: z.string().trim().min(2, 'Yanıt en az 2 karakter olmalıdır.'),
});

export type ReplyFormValues = z.infer<typeof replySchema>;