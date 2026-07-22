import { importShared } from './__federation_fn_import-sHsD3zYY.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { f as fetchProductMedia } from './endpoints-Bp5h6O00.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useProductMedia = (productId) => {
  return useQuery({
    queryKey: ["product-media", productId],
    queryFn: () => fetchProductMedia(productId),
    enabled: Boolean(productId),
    staleTime: 6e4,
    retry: 1
  });
};

const PlaceholderBox = () => {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(
    "div",
    {
      className: "w-full aspect-square bg-[#F3F4F6] border border-[#E5E7EB] rounded-xl flex flex-col items-center justify-center gap-2",
      style: { aspectRatio: "1 / 1" },
      children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs(
          "svg",
          {
            width: "48",
            height: "48",
            viewBox: "0 0 24 24",
            stroke: "#9CA3AF",
            strokeWidth: "1.5",
            fill: "none",
            strokeLinecap: "round",
            strokeLinejoin: "round",
            children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("rect", { x: "3", y: "3", width: "18", height: "18", rx: "2", ry: "2" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("circle", { cx: "8.5", cy: "8.5", r: "1.5" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx("polyline", { points: "21 15 16 10 5 21" })
            ]
          }
        ),
        /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[13px] text-[#9CA3AF] text-center px-4", children: "Görsel yakında eklenecek" })
      ]
    }
  );
};

const SkeletonGallery = () => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "w-full", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
    "div",
    {
      className: "w-full aspect-square rounded-xl sb-shimmer",
      style: { aspectRatio: "1 / 1" }
    }
  ) });
};

const {useState: useState$1} = await importShared('react');
const MainImage = ({ asset }) => {
  const [hasError, setHasError] = useState$1(false);
  if (hasError) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx(PlaceholderBox, {});
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx(
    "div",
    {
      className: "w-full aspect-square rounded-xl overflow-hidden border border-[#E5E7EB] bg-[#F3F4F6]",
      style: { aspectRatio: "1 / 1" },
      children: /* @__PURE__ */ jsxRuntimeExports.jsx(
        "img",
        {
          src: asset.url,
          alt: "Product",
          className: "w-full h-full object-cover",
          onError: () => setHasError(true)
        }
      )
    }
  );
};

const ThumbnailList = ({ assets, activeId, onSelect }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex gap-3 overflow-x-auto pb-2 mt-4 custom-scrollbar", children: assets.map((asset) => {
    const isActive = asset.assetId === activeId;
    return /* @__PURE__ */ jsxRuntimeExports.jsx(
      "div",
      {
        onClick: () => onSelect(asset),
        className: `w-[72px] h-[72px] rounded-lg overflow-hidden shrink-0 cursor-pointer transition-colors border ${isActive ? "border-[2px] border-[#1D4ED8]" : "border border-[#E5E7EB] hover:border-[#9CA3AF]"}`,
        children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          "img",
          {
            src: asset.thumbUrl,
            alt: "Thumbnail",
            className: "w-full h-full object-cover",
            onError: (e) => {
              e.currentTarget.style.display = "none";
            }
          }
        )
      },
      asset.assetId
    );
  }) });
};

const {useState,useEffect} = await importShared('react');
const ProductGallery = ({ productId }) => {
  const { data: assets, isLoading, isError } = useProductMedia(productId);
  const [activeAsset, setActiveAsset] = useState(null);
  useEffect(() => {
    if (assets && assets.length > 0) {
      const primary = assets.find((a) => a.primary) || assets[0];
      setActiveAsset(primary);
    } else {
      setActiveAsset(null);
    }
  }, [assets]);
  if (isLoading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx(SkeletonGallery, {});
  }
  if (isError || !assets || assets.length === 0 || !activeAsset) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx(PlaceholderBox, {});
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "w-full", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(MainImage, { asset: activeAsset }),
    assets.length > 1 && /* @__PURE__ */ jsxRuntimeExports.jsx(
      ThumbnailList,
      {
        assets,
        activeId: activeAsset.assetId,
        onSelect: setActiveAsset
      }
    )
  ] });
};

export { ProductGallery as default };
