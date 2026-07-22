import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makeOrderApi, A as AuthTokenProvider } from './orderApi-CS_nH2Q2.js';
import { importShared } from './__federation_fn_import-BO9tszH7.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useAdminOrderCount = () => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const orderApi = makeOrderApi(client);
  return useQuery({
    queryKey: ["admin-order-count"],
    queryFn: async () => {
      const data = await orderApi.getOrders(0, 1);
      return data.totalElements;
    }
  });
};

const InnerAdminOrderCount = () => {
  const { data, isLoading, isError } = useAdminOrderCount();
  if (isError) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "-" });
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "sb-shimmer rounded h-4 w-6 inline-block" });
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: data ?? 0 });
};
const AdminOrderCount = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminOrderCount, {}) });
};

export { AdminOrderCount as default };
