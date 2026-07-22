import { importShared } from './__federation_fn_import-BO9tszH7.js';
import { j as jsxRuntimeExports } from './jsx-runtime-BGkVYGZI.js';
import { u as useAuthToken, c as createApiClient, m as makePaymentApi, A as AuthTokenProvider } from './paymentApi-DPmOhyia.js';

const {useQuery} = await importShared('@tanstack/react-query');
const useAdminPayments = () => {
  const token = useAuthToken();
  const client = createApiClient(() => token);
  const paymentApi = makePaymentApi(client);
  return useQuery({
    queryKey: ["admin-payments"],
    queryFn: () => paymentApi.getPayments()
  });
};

const {useMutation,useQueryClient} = await importShared('@tanstack/react-query');
const useApproveRefund = () => {
  const token = useAuthToken();
  const queryClient = useQueryClient();
  const client = createApiClient(() => token);
  const paymentApi = makePaymentApi(client);
  return useMutation({
    mutationFn: (id) => paymentApi.approveRefund(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-payments"] });
      queryClient.invalidateQueries({ queryKey: ["admin-refund-count"] });
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

const PaymentStatusBadge = ({ status }) => {
  if (!status) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase bg-gray-100 text-[#4B5563]", children: "--" });
  }
  let classes = "bg-gray-100 text-[#4B5563]";
  let label = status;
  switch (status) {
    case "PENDING":
      classes = "bg-[#FEF3C7] text-[#92400E]";
      label = "Beklemede";
      break;
    case "COMPLETED":
      classes = "bg-[#D1FAE5] text-[#065F46]";
      label = "Tamamlandı";
      break;
    case "FAILED":
      classes = "bg-[#FEE2E2] text-[#991B1B]";
      label = "Başarısız";
      break;
    case "REFUND_REQUESTED":
      classes = "bg-[#FFEDD5] text-[#9A3412]";
      label = "İade Talebi";
      break;
    case "REFUNDED":
      classes = "bg-[#E0E7FF] text-[#3730A3]";
      label = "İade Edildi";
      break;
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: `inline-block rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap ${classes}`, children: label });
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
const shortId = (id) => {
  if (!id) return "";
  return `${id.substring(0, 8)}...`;
};

const {useState} = await importShared('react');
const InnerAdminPayments = () => {
  const { data: payments = [], isLoading, error, isError } = useAdminPayments();
  const { mutateAsync: approveRefund, isPending: isMutating } = useApproveRefund();
  const [toastMessage, setToastMessage] = useState(null);
  const [dialogConfig, setDialogConfig] = useState({ isOpen: false, paymentId: "" });
  const handleOpenDialog = (paymentId) => {
    setDialogConfig({ isOpen: true, paymentId });
  };
  const handleCloseDialog = () => {
    setDialogConfig({ isOpen: false, paymentId: "" });
  };
  const handleConfirmAction = async () => {
    try {
      await approveRefund(dialogConfig.paymentId);
      setToastMessage("İade onaylandı.");
    } catch (err) {
      alert(err.message || "İade onayı başarısız.");
    } finally {
      handleCloseDialog();
    }
  };
  if (isLoading) return /* @__PURE__ */ jsxRuntimeExports.jsx(TableSkeleton, {});
  if (isError && error) return /* @__PURE__ */ jsxRuntimeExports.jsx(ErrorBanner, { message: error.message });
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "space-y-4", children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full border-collapse", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#E5E7EB]", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Ödeme No" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Sipariş No" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Müşteri" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Tutar" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4", children: "Durum" }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4", children: "İşlemler" })
      ] }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { children: payments.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsx("tr", { children: /* @__PURE__ */ jsxRuntimeExports.jsx("td", { colSpan: 6, className: "p-8 text-center text-sm text-[#6B7280]", children: "Henüz ödeme kaydı yok." }) }) : payments.map((payment) => {
        const isRefundable = payment.status === "REFUND_REQUESTED";
        return /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { className: "border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: payment.id, children: shortId(payment.id) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: payment.orderId, children: shortId(payment.orderId) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-mono text-[#6B7280]", title: payment.customerId, children: shortId(payment.customerId) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm font-semibold text-[#111827]", children: formatTRY(payment.amount) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm", children: /* @__PURE__ */ jsxRuntimeExports.jsx(PaymentStatusBadge, { status: payment.status }) }),
          /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "p-3 py-4 text-sm text-right", children: isRefundable ? /* @__PURE__ */ jsxRuntimeExports.jsx(
            "button",
            {
              onClick: () => handleOpenDialog(payment.id),
              className: "h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors",
              children: "İadeyi Onayla"
            }
          ) : /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-[#9CA3AF]", children: "—" }) })
        ] }, payment.id);
      }) })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      ConfirmDialog,
      {
        isOpen: dialogConfig.isOpen,
        title: "İade onaylansın mı?",
        description: `${shortId(dialogConfig.paymentId)} numaralı ödemenin iadesi onaylanacak. Bu işlem geri alınamaz.`,
        onConfirm: handleConfirmAction,
        onCancel: handleCloseDialog,
        confirmLabel: "Onayla",
        isLoading: isMutating
      }
    ),
    toastMessage && /* @__PURE__ */ jsxRuntimeExports.jsx(Toast, { message: toastMessage, onClose: () => setToastMessage(null) })
  ] });
};
const AdminPayments = ({ session }) => {
  return /* @__PURE__ */ jsxRuntimeExports.jsx(AuthTokenProvider, { token: session.authToken, children: /* @__PURE__ */ jsxRuntimeExports.jsx(InnerAdminPayments, {}) });
};

export { AdminPayments as default };
