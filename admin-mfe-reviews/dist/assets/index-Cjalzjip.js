import { importShared } from './__federation_fn_import-BO9tszH7.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { r as reactDomExports } from './__federation_shared_react-dom-DFP2NIW6.js';
import AdminReviews from './__federation_expose_AdminReviews-BOBkaH4L.js';

var client = {};

var m = reactDomExports;
{
  client.createRoot = m.createRoot;
  client.hydrateRoot = m.hydrateRoot;
}

const React = await importShared('react');
const {QueryClient,QueryClientProvider} = await importShared('@tanstack/react-query');
const queryClient = new QueryClient();
const mockSession = {
  authToken: "standalone-mock-token",
  userId: "mock-admin-id",
  role: "ADMIN"
};
client.createRoot(document.getElementById("root")).render(
  /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(QueryClientProvider, { client: queryClient, children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "p-8 max-w-[1280px] mx-auto bg-white rounded-xl shadow mt-10", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-xl font-bold text-[#111827]", children: "Yorum Yönetimi Modülü" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-[#6B7280]", children: "Bağımsız Geliştirme Ortamı (Standalone)" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(AdminReviews, { session: mockSession })
  ] }) }) })
);
