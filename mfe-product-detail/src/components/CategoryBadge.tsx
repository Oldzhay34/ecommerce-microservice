/**
 * Presenter · veri çekmez.
 * Kategori metni backend'den geldiği gibi gösterilir — büyük harfe çevrilmez.
 */
export interface CategoryBadgeProps {
    category: string;
}

export function CategoryBadge({ category }: CategoryBadgeProps) {
    return (
        <span
            className="bg-brand/10 text-brand"
        style={{
        display: 'inline-block',
            borderRadius: 999,
            padding: '4px 10px',
            fontSize: 12,
            fontWeight: 600,
            whiteSpace: 'nowrap',
            textTransform: 'none',
    }}
>
    {category}
    </span>
);
}