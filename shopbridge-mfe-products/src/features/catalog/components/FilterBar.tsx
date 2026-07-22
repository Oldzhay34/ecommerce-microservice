import { useState } from 'react';
import { Button } from '../../../shared/ui/Button';
import { Input } from '../../../shared/ui/Input';

interface Props {
    keyword: string;
    category: string;
    onFilter: (f: { keyword: string; category: string }) => void;
    onReset: () => void;
}

export function FilterBar({ keyword, category, onFilter, onReset }: Props) {
    const [kw, setKw] = useState(keyword);
    const [cat, setCat] = useState(category);

    const submit = () => onFilter({ keyword: kw, category: cat });

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            submit();
        }
    };

    return (
        <div className="flex flex-wrap gap-2 mb-4 items-center">
            <Input
                placeholder="Ürün ara..."
                value={kw}
                onChange={(e) => setKw(e.target.value)}
                onKeyDown={handleKeyDown}
                className="flex-1 min-w-[160px]"
            />
            <Input
                placeholder="Kategori"
                value={cat}
                onChange={(e) => setCat(e.target.value)}
                onKeyDown={handleKeyDown}
                className="w-40"
            />
            <Button onClick={submit}>Filtrele</Button>
            <button
                onClick={() => {
                    setKw('');
                    setCat('');
                    onReset();
                }}
                className="px-4 py-2 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-100 transition"
            >
                Filtreyi Temizle
            </button>
        </div>
    );
}