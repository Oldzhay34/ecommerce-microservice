import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { importShared } from './__federation_fn_import-sHsD3zYY.js';
import { M as MEDIA_LIMITS } from './endpoints-Bp5h6O00.js';

const {createContext} = await importShared('react');

const AuthTokenContext = createContext(null);
function AuthTokenProvider({
  token,
  children
}) {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenContext.Provider, { value: token, children });
}

function validateImageFile(file) {
  const accepted = MEDIA_LIMITS.acceptedMimeTypes;
  if (!accepted.includes(file.type)) {
    return "Yalnızca PNG, JPEG ve WebP formatları kabul edilir.";
  }
  if (file.size > MEDIA_LIMITS.maxFileSizeBytes) {
    return "Dosya boyutu 5MB sınırını aşıyor.";
  }
  return null;
}
function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const {useRef,useState} = await importShared('react');
function ImageDropzone({
  onFilesSelected,
  disabled,
  remainingSlots
}) {
  const inputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const isFull = remainingSlots <= 0;
  const blocked = disabled || isFull;
  const handleFiles = (fileList) => {
    if (!fileList || blocked) return;
    onFilesSelected(Array.from(fileList));
    if (inputRef.current) inputRef.current.value = "";
  };
  const onDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    handleFiles(e.dataTransfer.files);
  };
  const onDragOver = (e) => {
    e.preventDefault();
    if (!blocked) setIsDragging(true);
  };
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "div",
      {
        onDrop,
        onDragOver,
        onDragLeave: () => setIsDragging(false),
        onClick: () => !blocked && inputRef.current?.click(),
        role: "button",
        tabIndex: blocked ? -1 : 0,
        "aria-disabled": blocked,
        onKeyDown: (e) => {
          if ((e.key === "Enter" || e.key === " ") && !blocked) {
            e.preventDefault();
            inputRef.current?.click();
          }
        },
        className: [
          "w-full rounded-sb border border-dashed px-4 py-8 text-center",
          "transition-colors",
          blocked ? "border-border bg-canvas cursor-not-allowed opacity-60" : "cursor-pointer hover:border-brand",
          isDragging ? "border-brand bg-brand/10" : "border-border-strong bg-surface"
        ].join(" "),
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink text-sm font-semibold", children: isFull ? `En fazla ${MEDIA_LIMITS.maxImagesOnCreate} görsel ekleyebilirsiniz.` : "Görselleri sürükleyin veya seçmek için tıklayın" }),
          !isFull && /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-ink-muted text-xs mt-1.5", children: [
            "PNG, JPEG veya WebP · tek dosya en fazla 5MB · ",
            remainingSlots,
            " görsel daha eklenebilir"
          ] })
        ]
      }
    ),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      "input",
      {
        ref: inputRef,
        type: "file",
        multiple: true,
        accept: MEDIA_LIMITS.acceptedMimeTypes.join(","),
        disabled: blocked,
        onChange: (e) => handleFiles(e.target.files),
        className: "hidden"
      }
    )
  ] });
}

function UploadPreviewList({
  images,
  results,
  onRemove,
  onMoveUp,
  disabled
}) {
  if (images.length === 0) return null;
  const resultOf = (id) => results.find((r) => r.id === id);
  return /* @__PURE__ */ jsxRuntimeExports.jsx("ul", { className: "mt-4 flex flex-col gap-2", children: images.map((img, index) => {
    const result = resultOf(img.id);
    const isCover = index === 0;
    return /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "li",
      {
        className: "flex items-center gap-3 border border-border rounded-sb px-3 py-2 bg-surface",
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            "img",
            {
              src: img.previewUrl,
              alt: "",
              className: "w-12 h-12 object-cover rounded shrink-0"
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "min-w-0 flex-1", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-ink text-sm truncate", children: img.file.name }),
              isCover && /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[10px] uppercase tracking-wide bg-brand text-white px-1.5 py-0.5 rounded shrink-0", children: "Kapak" })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(StatusLine, { result, sizeBytes: img.file.size })
          ] }),
          !disabled && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-1 shrink-0", children: [
            index > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                type: "button",
                onClick: () => onMoveUp(img.id),
                "aria-label": "Yukarı taşı",
                className: "text-ink-muted hover:text-ink text-sm px-2 py-1",
                children: "↑"
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                type: "button",
                onClick: () => onRemove(img.id),
                "aria-label": "Kaldır",
                className: "text-ink-muted hover:text-danger text-sm px-2 py-1",
                children: "✕"
              }
            )
          ] })
        ]
      },
      img.id
    );
  }) });
}
function StatusLine({
  result,
  sizeBytes
}) {
  if (!result || result.status === "pending") {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink-faint text-xs mt-0.5", children: formatBytes(sizeBytes) });
  }
  if (result.status === "uploading") {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink-muted text-xs mt-0.5", children: "Yükleniyor…" });
  }
  if (result.status === "success") {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-xs mt-0.5 text-success", children: "Yüklendi" });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-danger text-xs mt-0.5", children: result.message });
}

const {useCallback} = await importShared('react');
function ProductImageUploader({
  images,
  onChange,
  results,
  disabled,
  maxImages = MEDIA_LIMITS.maxImagesOnCreate
}) {
  const remainingSlots = maxImages - images.length;
  const handleFilesSelected = useCallback(
    (files) => {
      const accepted = [];
      for (const file of files) {
        if (accepted.length >= remainingSlots) break;
        if (validateImageFile(file) !== null) continue;
        accepted.push({
          id: `${file.name}-${file.size}-${file.lastModified}-${Math.random().toString(36).slice(2, 8)}`,
          file,
          previewUrl: URL.createObjectURL(file)
        });
      }
      if (accepted.length > 0) onChange([...images, ...accepted]);
    },
    [images, onChange, remainingSlots]
  );
  const handleRemove = useCallback(
    (id) => {
      const target = images.find((i) => i.id === id);
      if (target) URL.revokeObjectURL(target.previewUrl);
      onChange(images.filter((i) => i.id !== id));
    },
    [images, onChange]
  );
  const handleMoveUp = useCallback(
    (id) => {
      const index = images.findIndex((i) => i.id === id);
      if (index <= 0) return;
      const next = [...images];
      [next[index - 1], next[index]] = [next[index], next[index - 1]];
      onChange(next);
    },
    [images, onChange]
  );
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      ImageDropzone,
      {
        onFilesSelected: handleFilesSelected,
        disabled,
        remainingSlots
      }
    ),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      UploadPreviewList,
      {
        images,
        results,
        onRemove: handleRemove,
        onMoveUp: handleMoveUp,
        disabled
      }
    ),
    images.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-ink-muted text-xs mt-3", children: "İlk sıradaki görsel ürünün kapak görseli olur. Sıralamayı ↑ ile değiştirebilirsiniz." })
  ] });
}

function ProductImageUpload({
  authToken,
  images,
  onChange,
  results,
  disabled
}) {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(
    ProductImageUploader,
    {
      images,
      onChange,
      results,
      disabled
    }
  ) });
}

export { ProductImageUpload as default };
