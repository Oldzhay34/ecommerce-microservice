import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { replySchema, type ReplyFormValues } from '../schemas/replySchema';
import { useReplyReview } from '../hooks/useReplyReview';

export function ReplyForm({
                              reviewId, productId, onSuccess,
                          }: {
    reviewId: string;
    productId: string;
    onSuccess: (m: string) => void;
}) {
    const {
        register, handleSubmit, reset, formState: { errors },
    } = useForm<ReplyFormValues>({ resolver: zodResolver(replySchema) });

    const { mutate, isPending, error } = useReplyReview(productId);

    const onSubmit = (values: ReplyFormValues) => {
        mutate(
            { id: reviewId, reply: values.reply },
            {
                onSuccess: () => { onSuccess('Yanıt gönderildi.'); reset(); },
            },
        );
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="mt-3">
    <label className="block text-sm font-semibold text-ink mb-1">Yanıtınız</label>
        <textarea
    {...register('reply')}
    rows={3}
    className="w-full rounded-btn border border-line-strong px-3 py-2 text-ink bg-surface outline-none focus:border-brand resize-y"
    />
    <p className="text-muted text-xs mt-1">
        Müşteriye kibar ve çözüm odaklı bir yanıt yazın.
    </p>
    {errors.reply && <p className="text-err-tx text-xs mt-1">{errors.reply.message}</p>}
        {error && <p className="text-err-tx text-xs mt-2">{error.message}</p>}

            <div className="mt-3">
        <button
            type="submit"
            disabled={isPending}
            className="h-10 rounded-full font-semibold bg-brand text-white px-4 hover:bg-brand-dark disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
                >
                {isPending && (
                    <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                )}
            Yanıtla
            </button>
            </div>
            </form>
        );
        }