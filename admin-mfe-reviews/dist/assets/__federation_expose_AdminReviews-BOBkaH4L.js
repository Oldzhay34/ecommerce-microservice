import { importShared } from './__federation_fn_import-BO9tszH7.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makeReviewApi, A as AuthTokenProvider } from './reviewApi-DUzXSoC8.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useAdminReviews = () => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const reviewApi = makeReviewApi(client);
  return useQuery({
    queryKey: ["admin-reviews"],
    queryFn: () => reviewApi.getReviews()
  });
};

const {useMutation,useQueryClient} = await importShared('@tanstack/react-query');
const useModerateReview = () => {
  const token = useAuthToken();
  const queryClient = useQueryClient();
  const client = createApiClient(() => token);
  const reviewApi = makeReviewApi(client);
  return useMutation({
    mutationFn: ({ id, status }) => reviewApi.moderateReview(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-count"] });
    }
  });
};

const TableSkeleton = () => {
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-3", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-14 w-full sb-shimmer rounded-lg" }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-14 w-full sb-shimmer rounded-lg" }),
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-14 w-full sb-shimmer rounded-lg" })
  ] });
};

const ErrorBanner = ({ message }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-4 p-4 border border-[#FCA5A5] bg-[#FEF2F2] text-[#B91C1C] text-sm font-medium rounded-lg", children: message });
};

const ReviewStatusBadge = ({ status }) => {
  let classes = "bg-gray-100 text-[#4B5563]";
  let label = status;
  switch (status) {
    case "ACTIVE":
      classes = "bg-[#D1FAE5] text-[#065F46]";
      label = "Yayında";
      break;
    case "HIDDEN":
      classes = "bg-gray-100 text-[#4B5563]";
      label = "Gizli";
      break;
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: `inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`, children: label });
};

const RatingStars = ({ rating }) => {
  const fullStars = Math.max(0, Math.min(5, Math.floor(rating)));
  const emptyStars = Math.max(0, 5 - fullStars);
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-0.5", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center text-sm", children: [
      Array.from({ length: fullStars }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#F59E0B]", children: "★" }, `full-${i}`)),
      Array.from({ length: emptyStars }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#D1D5DB]", children: "☆" }, `empty-${i}`))
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-xs text-[#6B7280] font-semibold ml-1", children: [
      "(",
      rating,
      ")"
    ] })
  ] });
};

const ConfirmDialog = ({
  isOpen,
  title,
  description,
  onConfirm,
  onCancel,
  confirmLabel,
  isDanger = false,
  isLoading = false
}) => {
  if (!isOpen) return null;
  return /* @__PURE__ */ jsxRuntimeExports.jsx(
    "div",
    {
      onClick: onCancel,
      className: "fixed inset-0 z-50 bg-gray-900/45 flex items-center justify-center p-4",
      role: "dialog",
      "aria-modal": "true",
      children: /* @__PURE__ */ jsxRuntimeExports.jsxs(
        "div",
        {
          onClick: (e) => e.stopPropagation(),
          className: "w-full max-w-[420px] bg-white border border-[#E5E7EB] rounded-xl p-6 md:p-7 shadow-xl",
          children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "text-base font-semibold text-[#111827]", children: title }),
            /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-[#6B7280] mt-2 leading-relaxed", children: description }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-end space-x-3 mt-6", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "button",
                {
                  onClick: onCancel,
                  disabled: isLoading,
                  className: "h-10 px-4 rounded-lg bg-white border border-[#D1D5DB] text-[#111827] text-sm font-semibold hover:bg-gray-50 transition-colors disabled:opacity-50",
                  children: "Vazgeç"
                }
              ),
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "button",
                {
                  onClick: onConfirm,
                  disabled: isLoading,
                  className: `h-10 px-4 rounded-lg text-white text-sm font-semibold flex items-center justify-center space-x-2 transition-colors disabled:opacity-50 ${isDanger ? "bg-[#B91C1C] hover:bg-[#991B1B]" : "bg-[#1D4ED8] hover:bg-[#1E40AF]"}`,
                  children: /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: confirmLabel })
                }
              )
            ] })
          ]
        }
      )
    }
  );
};

const {useEffect} = await importShared('react');

const Toast = ({ message, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 2500);
    return () => clearTimeout(timer);
  }, [onClose]);
  return /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "fixed bottom-6 right-6 z-50 bg-[#111827] text-white text-sm font-semibold px-4 py-3 rounded-lg shadow-lg flex items-center space-x-2", children: /* @__PURE__ */ jsxRuntimeExports.jsx("span", { children: message }) });
};

const formatDate = (isoString) => {
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(isoString));
};
const shortId = (id) => {
  if (!id) return "";
  return `${id.substring(0, 8)}...`;
};

const {useState} = await importShared('react');
const InnerAdminReviews = () => {
  const { data: reviews = [], isLoading, error, isError } = useAdminReviews();
  const { mutateAsync: moderateReview, isPending: isMutating } = useModerateReview();
  const [toastMessage, setToastMessage] = useState(null);
  const [dialogConfig, setDialogConfig] = useState({ isOpen: false, reviewId: "", action: "HIDDEN" });
  const handleOpenDialog = (reviewId, action) => {
    setDialogConfig({ isOpen: true, reviewId, action });
  };
  const handleCloseDialog = () => {
    setDialogConfig({ isOpen: false, reviewId: "", action: "HIDDEN" });
  };
  const handleConfirmAction = async () => {
    try {
      await moderateReview({ id: dialogConfig.reviewId, status: dialogConfig.action });
      setToastMessage(
        dialogConfig.action === "HIDDEN" ? "Yorum gizlendi." : "Yorum yayınlandı."
      );
    } catch (err) {
      alert(err.message || "Yorum moderasyonu başarısız.");
    } finally {
      handleCloseDialog();
    }
  };
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx(TableSkeleton, {});
  if (isError && error) return /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: error.message });
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full border-collapse", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#E5E7EB]", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Ürün" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Müşteri" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Puan" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Yorum" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Mağaza Yanıtı" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Tarih" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Durum" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4", children: "İşlemler" })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { children: reviews.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsx("tr", { children: /* @__PURE__ */ jsxRuntimeExports.jsx("td", { colSpan: 8, className: "p-8 text-center text-sm text-[#6B7280]", children: "Henüz yorum yok." }) }) : reviews.map((review) => {
        const isActive = review.status === "ACTIVE";
        return /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: review.productId, children: shortId(review.productId) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: review.customerId, children: shortId(review.customerId) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(RatingStars, { rating: review.rating }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm text-[#111827]", title: review.comment, children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "max-w-[320px] line-clamp-2 overflow-hidden text-ellipsis", children: review.comment ? review.comment : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#9CA3AF]", children: "—" }) }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm", children: review.storeReplyText ? /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-xs font-semibold text-[#065F46] bg-[#D1FAE5] px-2 py-0.5 rounded", children: "Yanıtlandı" }) : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#9CA3AF]", children: "—" }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm text-[#111827]", children: formatDate(review.createdAt) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(ReviewStatusBadge, { status: review.status }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm text-right whitespace-nowrap", children: isActive ? /* @__PURE__ */ jsxRuntimeExports.jsx(
            "button",
            {
              onClick: () => handleOpenDialog(review.id, "HIDDEN"),
              className: "h-8 px-3 rounded bg-[#B91C1C] hover:bg-[#991B1B] text-xs font-semibold text-white transition-colors",
              children: "Gizle"
            }
          ) : /* @__PURE__ */ jsxRuntimeExports.jsx(
            "button",
            {
              onClick: () => handleOpenDialog(review.id, "ACTIVE"),
              className: "h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors",
              children: "Yayınla"
            }
          ) })
        ] }, review.id);
      }) })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      ConfirmDialog,
      {
        isOpen: dialogConfig.isOpen,
        title: dialogConfig.action === "HIDDEN" ? "Yorum gizlensın mi?" : "Yorum yayınlansın mı?",
        description: dialogConfig.action === "HIDDEN" ? "Yorum müşteri listelerinde görünmeyecek." : "Yorum müşteri listelerinde tekrar görünecek.",
        onConfirm: handleConfirmAction,
        onCancel: handleCloseDialog,
        confirmLabel: dialogConfig.action === "HIDDEN" ? "Gizle" : "Yayınla",
        isDanger: dialogConfig.action === "HIDDEN",
        isLoading: isMutating
      }
    ),
    toastMessage && /* @__PURE__ */ jsxRuntimeExports.jsx(Toast, { message: toastMessage, onClose: () => setToastMessage(null) })
  ] });
};
const AdminReviews = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminReviews, {}) });
};

export { AdminReviews as default };
