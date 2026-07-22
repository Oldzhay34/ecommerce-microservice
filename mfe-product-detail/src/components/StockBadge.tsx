import { getStockView } from '../utils/stock';

/**
 * Presenter · veri çekmez.
 * Eşik mantığı utils/stock.ts içindedir; burada if/else zinciri yoktur.
 * Theo Mandel · Bellek Yükünü Azalt: renkli rozet + açık metin birlikte verilir.
 */
export interface StockBadgeProps {
    stock: number;
}

export function StockBadge({ stock }: StockBadgeProps) {
    const view = getStockView(stock);

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
      <span
          style={{
              display: 'inline-block',
              backgroundColor: view.bg,
              color: view.text,
              borderRadius: 999,
              padding: '4px 10px',
              fontSize: 12,
              fontWeight: 600,
              whiteSpace: 'nowrap',
          }}
      >
        {view.label}
      </span>
            <span style={{ fontSize: 13, color: '#6B7280' }}>{view.note}</span>
        </div>
    );
}