// 1–5 yıldız. Dolu/boş gösterim.
export function StarRating({ rating }: { rating: number }) {
    const rounded = Math.round(rating);
    return (
        <span className="inline-flex items-center" aria-label={`${rating} / 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
          <svg
              key={n}
              viewBox="0 0 20 20"
              className="w-4 h-4"
              fill={n <= rounded ? '#F59E0B' : 'none'}
              stroke="#F59E0B"
              strokeWidth="1.5"
          >
              <path d="M10 1.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L10 15l-5.2 2.6 1-5.8L1.5 7.7l5.9-.9L10 1.5z" />
          </svg>
      ))}
    </span>
    );
}