import { importShared } from './__federation_fn_import-Ps2UjJ-J.js';
import { j as jsxRuntimeExports } from './jsx-runtime-CyoIsdjr.js';
import { a as axios, E as ErrorBanner, S as Skeleton, C as Card } from './components-CBrsan5h.js';

const TOKEN_KEY = "shopbridge_access_token";
function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}
function getUserIdFromToken() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.sub ?? null;
  } catch {
    return null;
  }
}

const apiClient = axios.create({
  baseURL: "http://localhost:8080"
  // [Api Gateway]
});
apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

const {useQueries} = await importShared('@tanstack/react-query');
function useProductNames(productIds) {
  const uniqueIds = [...new Set(productIds)];
  const results = useQueries({
    queries: uniqueIds.map((id) => ({
      queryKey: ["product", id],
      queryFn: async () => {
        const { data } = await apiClient.get(`/api/v1/products/${id}`);
        return data;
      },
      staleTime: 5 * 60 * 1e3
    }))
  });
  const nameMap = {};
  results.forEach((r, i) => {
    if (r.data) nameMap[uniqueIds[i]] = r.data.name;
  });
  const isLoading = results.some((r) => r.isLoading);
  return { nameMap, isLoading };
}

const {useQuery} = await importShared('@tanstack/react-query');
function ReviewsWidget() {
  const userId = getUserIdFromToken();
  const { data: reviews, isLoading, isError } = useQuery({
    queryKey: ["reviews", "me"],
    queryFn: async () => {
      const { data } = await apiClient.get("/api/reviews/me");
      return data;
    },
    enabled: !!userId
  });
  const { nameMap } = useProductNames(reviews?.map((r) => r.productId) ?? []);
  if (!userId) return /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: "Oturum bilgisi bulunamadı." });
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx(Skeleton, { height: "h-48" });
  if (isError) return /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: "Yorumlarınız yüklenemedi." });
  if (!reviews?.length)
    return /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { children: /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink-muted text-center", children: "Henüz bir ürün değerlendirmesi yapmadınız." }) });
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold text-lg mb-4 text-ink", children: "Son Değerlendirmelerim" }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("ul", { className: "space-y-4", children: reviews.map((review) => /* @__PURE__ */ jsxRuntimeExports.jsxs("li", { className: "border-b border-border pb-3 last:border-0 last:pb-0", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-between items-start mb-1", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "font-medium text-ink", children: nameMap[review.productId] ?? `Ürün: ${review.productId}` }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex text-warning", children: [...Array(5)].map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: `w-4 h-4 ${i < review.rating ? "fill-current" : "text-ink-faint fill-current"}`, viewBox: "0 0 20 20", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { d: "M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" }) }, i)) })
      ] }),
      review.comment && /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-ink-muted italic", children: [
        '"',
        review.comment,
        '"'
      ] }),
      review.storeReplyText && /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "mt-2 text-sm text-ink-muted bg-surface-hover rounded-sb p-2", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "font-medium", children: "Mağaza yanıtı:" }),
        " ",
        review.storeReplyText
      ] })
    ] }, review.id)) })
  ] });
}

export { ReviewsWidget as default };
