import { importShared } from './__federation_fn_import-sHsD3zYY.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { r as reactDomExports } from './__federation_shared_react-dom-DFP2NIW6.js';
import ProductGallery from './__federation_expose_ProductGallery-C09M_oPr.js';

var client = {};

var m = reactDomExports;
{
  client.createRoot = m.createRoot;
  client.hydrateRoot = m.hydrateRoot;
}

const React = await importShared('react');
const {QueryClient,QueryClientProvider} = await importShared('@tanstack/react-query');
const queryClient = new QueryClient();
client.createRoot(document.getElementById("root")).render(
  /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(QueryClientProvider, { client: queryClient, children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-[420px] mx-auto mt-10 p-4 border rounded-sb-lg border-dashed border-border", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("h2", { className: "text-sm text-ink-muted mb-4 font-mono", children: "Dev Test Container" }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(ProductGallery, { productId: "dev-test-123" })
  ] }) }) })
);
