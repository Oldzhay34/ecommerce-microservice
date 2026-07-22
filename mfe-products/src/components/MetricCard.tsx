// Presenter: üstte gri başlık, altında büyük değer.
export function MetricCard({
                               title,
                               value,
                           }: {
    title: string;
    value: string | number;
}) {
    return (
        <div className="bg-white border border-line rounded-card px-6 py-5">
            <div className="text-muted text-[13px] font-semibold uppercase tracking-[0.06em]">
                {title}
            </div>
            <div className="text-ink text-[28px] font-bold mt-2">{value}</div>
        </div>
    );
}