import { j as jsxRuntimeExports } from './jsx-runtime-CyoIsdjr.js';
import { importShared } from './__federation_fn_import-Ps2UjJ-J.js';
import { a as axios, S as Skeleton, E as ErrorBanner } from './components-CBrsan5h.js';

const publicApiClient = axios.create({
  baseURL: "http://localhost:8080"
  // [Api Gateway]
});

const {useQuery} = await importShared('@tanstack/react-query');
function useProductReviews(productId) {
  return useQuery({
    queryKey: ["product-reviews", productId],
    queryFn: async () => {
      const { data } = await publicApiClient.get(
        `/api/reviews/product/${productId}`
      );
      return data;
    },
    enabled: Boolean(productId),
    staleTime: 6e4,
    retry: 1
  });
}

function StarRating({
  rating,
  size = "sm"
}) {
  const sizeCls = size === "lg" ? "w-5 h-5" : size === "md" ? "w-4 h-4" : "w-3.5 h-3.5";
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex text-warning", role: "img", "aria-label": `${rating} / 5 yıldız`, children: [...Array(5)].map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx(
    "svg",
    {
      className: `${sizeCls} ${i < rating ? "fill-current" : "text-ink-faint fill-current"}`,
      viewBox: "0 0 20 20",
      "aria-hidden": "true",
      children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { d: "M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" })
    },
    i
  )) });
}

function RatingSummary({
  averageRating,
  totalCount
}) {
  const rounded = Math.round(averageRating * 10) / 10;
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-2xl font-bold text-ink", children: rounded.toFixed(1) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(StarRating, { rating: Math.round(averageRating), size: "md" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-xs text-ink-muted mt-0.5", children: [
        totalCount,
        " değerlendirme"
      ] })
    ] })
  ] });
}

function CustomerLabel({ index }) {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "font-medium text-ink", children: [
    "Müşteri #",
    index + 1
  ] });
}

const dateFmt = new Intl.DateTimeFormat("tr-TR", {
  day: "numeric",
  month: "long",
  year: "numeric"
});
function formatDate(iso) {
  try {
    return dateFmt.format(new Date(iso));
  } catch {
    return "";
  }
}
function ReviewItem({ review, index }) {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("li", { className: "border-b border-border pb-5 last:border-0 last:pb-0", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-start gap-4 mb-1.5", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2 min-w-0", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx(CustomerLabel, { index }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-ink-faint", children: "·" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs text-ink-muted whitespace-nowrap", children: formatDate(review.createdAt) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(StarRating, { rating: review.rating })
    ] }),
    review.comment && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-ink-muted leading-relaxed", children: review.comment }),
    review.storeReplyText && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-3 ml-4 border-l-2 border-brand/40 pl-3 py-2 bg-brand-soft rounded-r-sb", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2 mb-1", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs font-semibold text-brand", children: "Mağaza yanıtı" }),
        review.storeRepliedAt && /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs text-ink-muted", children: formatDate(review.storeRepliedAt) })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-ink-muted leading-relaxed", children: review.storeReplyText })
    ] })
  ] });
}

function ProductReviews({ productId }) {
  const { data, isLoading, isError } = useProductReviews(productId);
  if (isLoading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsxs("section", { className: "bg-surface border border-border rounded-sb-lg p-6 mt-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(Skeleton, { height: "h-6" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mt-4", children: /* @__PURE__ */ jsxRuntimeExports.jsx(Skeleton, { height: "h-32" }) })
    ] });
  }
  if (isError) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("section", { className: "bg-surface border border-border rounded-sb-lg p-6 mt-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: "Değerlendirmeler yüklenemedi." }) });
  }
  const reviews = data?.reviews ?? [];
  const averageRating = data?.averageRating ?? 0;
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("section", { className: "bg-surface border border-border rounded-sb-lg p-6 mt-6", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-baseline justify-between gap-4 flex-wrap", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-lg font-semibold text-ink", children: "Değerlendirmeler" }),
      reviews.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx(RatingSummary, { averageRating, totalCount: reviews.length })
    ] }),
    reviews.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink-muted text-sm text-center py-10", children: "Bu ürün için henüz değerlendirme yapılmamış." }) : /* @__PURE__ */ jsxRuntimeExports.jsx("ul", { className: "space-y-5 mt-6", children: reviews.map((review, index) => /* @__PURE__ */ jsxRuntimeExports.jsx(ReviewItem, { review, index }, review.id)) })
  ] });
}

export { ProductReviews as default };
