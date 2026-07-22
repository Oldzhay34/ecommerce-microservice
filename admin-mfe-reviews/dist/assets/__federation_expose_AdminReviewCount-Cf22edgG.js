import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makeReviewApi, A as AuthTokenProvider } from './reviewApi-DUzXSoC8.js';
import { importShared } from './__federation_fn_import-BO9tszH7.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useReviewCount = () => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const reviewApi = makeReviewApi(client);
  return useQuery({
    queryKey: ["admin-review-count"],
    queryFn: async () => {
      const data = await reviewApi.getReviews();
      return data.length;
    }
  });
};

const InnerAdminReviewCount = () => {
  const { data, isLoading, isError } = useReviewCount();
  if (isError) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "-" });
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "sb-shimmer rounded h-4 w-6 inline-block" });
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: data ?? 0 });
};
const AdminReviewCount = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminReviewCount, {}) });
};

export { AdminReviewCount as default };
