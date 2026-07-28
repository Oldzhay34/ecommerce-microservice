import type { Product } from '../types/product';
import { formatPrice } from '../lib/formatPrice';

export function ProductCard({ product }: { product: Product }) {
    const outOfStock = product.stock <= 0;

    return (
        <a
            // NOT: Eski hash-tabanli router (#/product/:id) kaldirildi — shell artik
            // react-router-dom ile gercek path kullaniyor (/product/:id). Bu widget
            // federation shared listesinde react-router-dom paylasmadigi icin useNavigate()
            // kullanamiyor; gercek path'e giden duz <a> linki dogru hedefe ulasir (tam
            // sayfa yenilemesiyle) — hash link ise router hic tetiklemedigi icin HICBIR
            // sey yapmiyordu.
            href={`/product/${product.id}`}
            aria-label={`${product.name} ürün detayına git`}
            className="h-full p-4 rounded-sb-lg border border-border bg-surface flex flex-col justify-between no-underline text-inherit cursor-pointer shadow-sb hover:shadow-sb-lg hover:border-border-strong hover:-translate-y-0.5 transition"
        >
            <div>
                <p className="text-xs text-ink-faint uppercase tracking-wide">{product.category}</p>
                <h4 className="font-semibold text-ink mt-1 mb-2 line-clamp-2">{product.name}</h4>
            </div>

            <div className="flex items-end justify-between mt-2">
                <span className="text-lg font-bold text-brand">{formatPrice(product.price)}</span>
                <span className={`text-xs ${outOfStock ? 'text-danger' : 'text-success'}`}>
                    {outOfStock ? 'Tükendi' : `Stokta ${product.stock} adet`}
                </span>
            </div>
        </a>
    );
}