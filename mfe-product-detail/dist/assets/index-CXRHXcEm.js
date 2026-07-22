import { importShared } from './__federation_fn_import-sHsD3zYY.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { r as reactDomExports } from './__federation_shared_react-dom-DFP2NIW6.js';
import ProductDetail from './__federation_expose_ProductDetail-BhaocoUU.js';

var client = {};

var m = reactDomExports;
{
  client.createRoot = m.createRoot;
  client.hydrateRoot = m.hydrateRoot;
}

const React = await importShared('react');
const {QueryClient,QueryClientProvider} = await importShared('@tanstack/react-query');
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false
    }
  }
});
const devSession = {
  authToken: "",
  userId: "00000000-0000-0000-0000-000000000000",
  role: "CUSTOMER"
};
const DEV_PRODUCT_ID = "eee829bb-0000-0000-0000-000000000000";
function DevApp() {
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { style: { minHeight: "100vh", backgroundColor: "#F4F5F7", padding: 24 }, children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { style: { maxWidth: 1120, margin: "0 auto" }, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { style: { fontSize: 20, fontWeight: 700, marginBottom: 20 }, children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("span", { style: { color: "#000000" }, children: "Shop" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("span", { style: { color: "#1D4ED8" }, children: "Bridge" })
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      ProductDetail,
      {
        session: devSession,
        productId: DEV_PRODUCT_ID,
        onNavigate: (path) => {
          console.log("[dev] onNavigate →", path);
        }
      }
    )
  ] }) });
}
client.createRoot(document.getElementById("root")).render(
  /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(QueryClientProvider, { client: queryClient, children: /* @__PURE__ */ jsxRuntimeExports.jsx(DevApp, {}) }) })
);
