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
            style={{
        display: 'inline-block',
            backgroundColor: '#EFF6FF',
            color: '#1D4ED8',
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