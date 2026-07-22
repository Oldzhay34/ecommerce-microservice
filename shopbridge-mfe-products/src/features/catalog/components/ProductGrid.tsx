import type { Product } from '../types/product';
import { ProductCard } from './ProductCard';

export function ProductGrid({ products }: { products: Product[] }) {
    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {products.map((p) => (
                <ProductCard key={p.id} product={p} />
            ))}
        </div>
    );
}