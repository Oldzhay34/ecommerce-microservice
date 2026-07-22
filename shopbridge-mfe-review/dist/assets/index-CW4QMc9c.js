import { importShared } from './__federation_fn_import-Ps2UjJ-J.js';
import { j as jsxRuntimeExports } from './jsx-runtime-CyoIsdjr.js';
import { r as reactDomExports } from './index-D9Af7wOI.js';
import ReviewsWidget from './__federation_expose_Widget-CZm9jla4.js';

var client = {};

var m = reactDomExports;
{
  client.createRoot = m.createRoot;
  client.hydrateRoot = m.hydrateRoot;
}

const {QueryClient,QueryClientProvider} = await importShared('@tanstack/react-query');
const queryClient = new QueryClient();
function App() {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(QueryClientProvider, { client: queryClient, children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "max-w-md mx-auto mt-10 p-4 border-2 border-dashed border-gray-300 rounded-lg", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-xl font-bold mb-4 text-gray-800 text-center", children: "İzole Çalışma Modu (Reviews)" }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(ReviewsWidget, {})
  ] }) });
}

const React = await importShared('react');
client.createRoot(document.getElementById("root")).render(
  /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(App, {}) })
);
