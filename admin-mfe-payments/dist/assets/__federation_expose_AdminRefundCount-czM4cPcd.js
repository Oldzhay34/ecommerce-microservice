import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makePaymentApi, A as AuthTokenProvider } from './paymentApi-DPmOhyia.js';
import { importShared } from './__federation_fn_import-BO9tszH7.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useRefundCount = () => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const paymentApi = makePaymentApi(client);
  return useQuery({
    queryKey: ["admin-refund-count"],
    queryFn: async () => {
      const data = await paymentApi.getPayments();
      return data.filter((payment) => payment.status === "REFUND_REQUESTED").length;
    }
  });
};

const InnerAdminRefundCount = () => {
  const { data, isLoading, isError } = useRefundCount();
  if (isError) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: "-" });
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "sb-shimmer rounded h-4 w-6 inline-block" });
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: data ?? 0 });
};
const AdminRefundCount = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminRefundCount, {}) });
};

export { AdminRefundCount as default };
