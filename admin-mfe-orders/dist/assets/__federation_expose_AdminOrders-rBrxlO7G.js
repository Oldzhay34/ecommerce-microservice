import { importShared } from './__federation_fn_import-BO9tszH7.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makeOrderApi, A as AuthTokenProvider } from './orderApi-CS_nH2Q2.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useAdminOrders = (page) => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const orderApi = makeOrderApi(client);
  return useQuery({
    queryKey: ["admin-orders", page],
    queryFn: () => orderApi.getOrders(page),
    placeholderData: (prev) => prev
  });
};

const {useMutation,useQueryClient} = await importShared('@tanstack/react-query');
const useUpdateOrderStatus = () => {
  const token = useAuthToken();
  const queryClient = useQueryClient();
  const client = createApiClient(() => token);
  const orderApi = makeOrderApi(client);
  return useMutation({
    mutationFn: ({ id, status }) => orderApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
      queryClient.invalidateQueries({ queryKey: ["admin-order-count"] });
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

const OrderStatusBadge = ({ status }) => {
  let classes = "bg-gray-100 text-[#4B5563]";
  let label = status;
  switch (status) {
    case "PENDING":
      classes = "bg-[#FEF3C7] text-[#92400E]";
      label = "Beklemede";
      break;
    case "APPROVED":
      classes = "bg-[#DBEAFE] text-[#1E40AF]";
      label = "Onaylandı";
      break;
    case "SHIPPED":
      classes = "bg-[#D1FAE5] text-[#065F46]";
      label = "Kargolandı";
      break;
    case "CANCELLED":
      classes = "bg-[#FEE2E2] text-[#991B1B]";
      label = "İptal Edildi";
      break;
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: `inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`, children: label });
};

const PaginationBar = ({
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  isFirst,
  isLast
}) => {
  if (totalPages === 0) return null;
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "w-full border-t border-[#E5E7EB] pt-4 flex items-center justify-between", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-xs text-[#6B7280]", children: [
      "Toplam ",
      totalElements,
      " sipariş · Sayfa ",
      currentPage + 1,
      " / ",
      totalPages
    ] }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center space-x-2", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        "button",
        {
          onClick: () => onPageChange(currentPage - 1),
          disabled: isFirst,
          className: "h-8 px-3 rounded bg-white border border-[#D1D5DB] text-xs font-semibold text-[#111827] hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors",
          children: "← Önceki"
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        "button",
        {
          onClick: () => onPageChange(currentPage + 1),
          disabled: isLast,
          className: "h-8 px-3 rounded bg-white border border-[#D1D5DB] text-xs font-semibold text-[#111827] hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors",
          children: "Sonraki →"
        }
      )
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

const formatTRY = (amount) => {
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY"
  }).format(amount);
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
const InnerAdminOrders = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading, error, isError } = useAdminOrders(page);
  const { mutateAsync: updateStatus, isPending: isMutating } = useUpdateOrderStatus();
  const [toastMessage, setToastMessage] = useState(null);
  const [dialogConfig, setDialogConfig] = useState({ isOpen: false, orderId: "", action: "SHIPPED" });
  const handleOpenDialog = (orderId, action) => {
    setDialogConfig({ isOpen: true, orderId, action });
  };
  const handleCloseDialog = () => {
    setDialogConfig({ isOpen: false, orderId: "", action: "SHIPPED" });
  };
  const handleConfirmAction = async () => {
    try {
      await updateStatus({ id: dialogConfig.orderId, status: dialogConfig.action });
      setToastMessage(
        dialogConfig.action === "SHIPPED" ? "Sipariş kargolandı." : "Sipariş iptal edildi."
      );
    } catch (err) {
      alert(err.message || "İşlem gerçekleştirilemedi.");
    } finally {
      handleCloseDialog();
    }
  };
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx(TableSkeleton, {});
  if (isError && error) return /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: error.message });
  const orders = data?.content || [];
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full border-collapse", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#E5E7EB]", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Sipariş No" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Müşteri" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Tarih" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Ürün" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Tutar" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Durum" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4", children: "İşlemler" })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { children: orders.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsx("tr", { children: /* @__PURE__ */ jsxRuntimeExports.jsx("td", { colSpan: 7, className: "p-8 text-center text-sm text-[#6B7280]", children: "Henüz sipariş yok." }) }) : orders.map((order) => {
        const canShip = order.status === "PENDING" || order.status === "APPROVED";
        const canCancel = order.status !== "CANCELLED";
        return /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: order.id, children: shortId(order.id) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: order.userId, children: shortId(order.userId) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm text-[#111827]", children: formatDate(order.createdAt) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("td", { className: "p-3 py-4 text-sm text-[#111827]", children: [
            order.items.length,
            " kalem"
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-semibold text-[#111827]", children: formatTRY(order.totalAmount) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(OrderStatusBadge, { status: order.status }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("td", { className: "p-3 py-4 text-sm text-right space-x-2 whitespace-nowrap", children: [
            canShip && /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                onClick: () => handleOpenDialog(order.id, "SHIPPED"),
                className: "h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors",
                children: "Kargola"
              }
            ),
            canCancel && /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                onClick: () => handleOpenDialog(order.id, "CANCELLED"),
                className: "h-8 px-3 rounded bg-[#B91C1C] hover:bg-[#991B1B] text-xs font-semibold text-white transition-colors",
                children: "İptal Et"
              }
            ),
            !canShip && !canCancel && /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#9CA3AF]", children: "—" })
          ] })
        ] }, order.id);
      }) })
    ] }) }),
    data && /* @__PURE__ */ jsxRuntimeExports.jsx(
      PaginationBar,
      {
        currentPage: page,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
        onPageChange: setPage,
        isFirst: data.first,
        isLast: data.last
      }
    ),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      ConfirmDialog,
      {
        isOpen: dialogConfig.isOpen,
        title: dialogConfig.action === "SHIPPED" ? "Sipariş kargolansın mı?" : "Sipariş iptal edilsin mi?",
        description: dialogConfig.action === "SHIPPED" ? `${shortId(dialogConfig.orderId)} numaralı sipariş "Kargolandı" olarak işaretlenecek.` : `${shortId(dialogConfig.orderId)} numaralı sipariş iptal edilecek. Bu işlem geri alınamaz.`,
        onConfirm: handleConfirmAction,
        onCancel: handleCloseDialog,
        confirmLabel: dialogConfig.action === "SHIPPED" ? "Kargola" : "İptal Et",
        isDanger: dialogConfig.action === "CANCELLED",
        isLoading: isMutating
      }
    ),
    toastMessage && /* @__PURE__ */ jsxRuntimeExports.jsx(Toast, { message: toastMessage, onClose: () => setToastMessage(null) })
  ] });
};
const AdminOrders = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminOrders, {}) });
};

export { AdminOrders as default };
