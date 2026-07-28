// Primary buton; tıklanınca shell route'una yönlendirir (onNavigate).
export function AddProductButton({
                                     onNavigate,
                                 }: {
    onNavigate?: (path: string) => void;
}) {
    return (
        <button
            onClick={() => onNavigate?.('/store/products/new')}
            className="h-10 rounded-full font-semibold bg-brand text-white px-4 hover:bg-brand-dark"
        >
            + Ürün Ekle
        </button>
    );
}