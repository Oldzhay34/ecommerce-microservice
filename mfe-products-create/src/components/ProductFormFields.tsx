import type { FieldErrors, UseFormRegister } from 'react-hook-form';
import type { CreateProductFormValues } from '../schemas/createProductSchema';

const NAME_MAX = 120;

const inputCls = (hasError: boolean) =>
    [
        'w-full h-11 rounded-btn px-3 text-ink outline-none bg-surface',
        'border',
        hasError ? 'border-danger' : 'border-border-strong',
        'focus:border-brand focus:shadow-focus',
        'disabled:opacity-50 disabled:cursor-not-allowed',
    ].join(' ');

function RequiredMark() {
    return <span className="text-danger"> *</span>;
}

export function ProductFormFields({
                                      register,
                                      errors,
                                      nameLength,
                                      disabled,
                                  }: {
    register: UseFormRegister<CreateProductFormValues>;
    errors: FieldErrors<CreateProductFormValues>;
    nameLength: number;
    disabled: boolean;
}) {
    const nameErr = errors.name?.message;
    const catErr = errors.category?.message;
    const priceErr = errors.price?.message;
    const stockErr = errors.stock?.message;

    return (
        <div className="flex flex-col gap-5">
            {/* 1 — Ürün Adı */}
            <div>
                <label htmlFor="name" className="block text-sm font-semibold text-ink mb-1.5">
                    Ürün Adı<RequiredMark />
                </label>
                <input
                    id="name"
                    type="text"
                    disabled={disabled}
                    aria-invalid={!!nameErr}
                    className={inputCls(!!nameErr)}
                    {...register('name')}
                />
                <div className="flex justify-between gap-3">
                    {nameErr ? (
                        <p className="text-danger text-xs mt-1.5">{nameErr}</p>
                    ) : (
                        <p className="text-ink-muted text-xs mt-1.5">
                            Müşterinin arama sonuçlarında göreceği isim.
                        </p>
                    )}
                    <p
                        className={`text-xs mt-1.5 whitespace-nowrap ${
                            nameLength > NAME_MAX ? 'text-danger' : 'text-ink-faint'
                        }`}
                    >
                        {nameLength}/{NAME_MAX}
                    </p>
                </div>
            </div>

            {/* 2 — Kategori */}
            <div>
                <label htmlFor="category" className="block text-sm font-semibold text-ink mb-1.5">
                    Kategori<RequiredMark />
                </label>
                <input
                    id="category"
                    type="text"
                    disabled={disabled}
                    aria-invalid={!!catErr}
                    className={inputCls(!!catErr)}
                    {...register('category')}
                />
                {catErr ? (
                    <p className="text-danger text-xs mt-1.5">{catErr}</p>
                ) : (
                    <p className="text-ink-muted text-xs mt-1.5">
                        Örn. OTO BAKIM, TEMİZLİK. Büyük harfe çevrilir.
                    </p>
                )}
            </div>

            {/* 3 — Fiyat */}
            <div>
                <label htmlFor="price" className="block text-sm font-semibold text-ink mb-1.5">
                    Fiyat (₺)<RequiredMark />
                </label>
                <input
                    id="price"
                    type="number"
                    step="0.01"
                    min="0.01"
                    disabled={disabled}
                    aria-invalid={!!priceErr}
                    className={inputCls(!!priceErr)}
                    {...register('price', { valueAsNumber: true })}
                />
                {priceErr ? (
                    <p className="text-danger text-xs mt-1.5">{priceErr}</p>
                ) : (
                    <p className="text-ink-muted text-xs mt-1.5">
                        Sıfırdan büyük bir tutar girin. Örn. 149,90
                    </p>
                )}
            </div>

            {/* 4 — Stok Adedi */}
            <div>
                <label htmlFor="stock" className="block text-sm font-semibold text-ink mb-1.5">
                    Stok Adedi<RequiredMark />
                </label>
                <input
                    id="stock"
                    type="number"
                    step="1"
                    min="0"
                    disabled={disabled}
                    aria-invalid={!!stockErr}
                    className={inputCls(!!stockErr)}
                    {...register('stock', { valueAsNumber: true })}
                />
                {stockErr ? (
                    <p className="text-danger text-xs mt-1.5">{stockErr}</p>
                ) : (
                    <p className="text-ink-muted text-xs mt-1.5">
                        0 veya daha büyük bir tam sayı girin.
                    </p>
                )}
            </div>
        </div>
    );
}