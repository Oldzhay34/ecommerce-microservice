import type { Product } from '../types/product';
import { formatPrice } from '../lib/formatPrice';

export function ProductCard({ product }: { product: Product }) {
    const outOfStock = product.stock <= 0;

    return (
        <a
            href={`#/product/${product.id}`}
            aria-label={`${product.name} ürün detayına git`}
            className="h-full p-4 rounded-lg border border-gray-200 bg-white flex flex-col justify-between no-underline text-inherit cursor-pointer hover:shadow-md hover:border-gray-300 transition"
        >
            <div>
                <p className="text-xs text-gray-400 uppercase tracking-wide">{product.category}</p>
                <h4 className="font-semibold text-gray-900 mt-1 mb-2 line-clamp-2">{product.name}</h4>
            </div>

            <div className="flex items-end justify-between mt-2">
                <span className="text-lg font-bold text-blue-700">{formatPrice(product.price)}</span>
                <span className={`text-xs ${outOfStock ? 'text-red-500' : 'text-green-600'}`}>
                    {outOfStock ? 'Tükendi' : `Stokta ${product.stock} adet`}
                </span>
            </div>
        </a>
    );
}