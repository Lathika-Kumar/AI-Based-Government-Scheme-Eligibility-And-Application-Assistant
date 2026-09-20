/**
 * @file ToastNotification.jsx
 * @description Lightweight, self-dismissing toast notification system.
 *
 * Usage:
 *   const { showToast } = useToast();
 *   showToast("success", "Saved!", "Your profile has been updated.");
 *
 * Mount <ToastContainer /> once near the app root (e.g., in AppProvider).
 */

import { useState, useCallback, useEffect, useRef, createContext, useContext } from "react";
import { CheckCircle, AlertCircle, AlertTriangle, Info, X } from "lucide-react";

// ── Toast Context ─────────────────────────────────────────────────────────────

const ToastContext = createContext(null);

// ── Helper to normalize polymorphic showToast calls ───────────────────────────

export function normalizeToastParams(arg1, arg2, arg3) {
  const validTypes = ["success", "error", "warning", "info"];

  const defaultTitle = (t) => {
    switch (t) {
      case "success": return "Success";
      case "error": return "Action Failed";
      case "warning": return "Notice";
      case "info":
      default: return "Information";
    }
  };

  // Case 1: showToast("success"|"error"|..., title, message)
  if (typeof arg1 === "string" && validTypes.includes(arg1.toLowerCase())) {
    const type = arg1.toLowerCase();
    if (arg3 !== undefined) {
      return { type, title: arg2 || defaultTitle(type), message: String(arg3 || "") };
    }
    // showToast("error", "Failed to load")
    return {
      type,
      title: defaultTitle(type),
      message: String(arg2 || ""),
    };
  }

  // Case 2: showToast(message, "error"|"success"|...) -> e.g. showToast("Saved!", "success")
  if (typeof arg2 === "string" && validTypes.includes(arg2.toLowerCase())) {
    const type = arg2.toLowerCase();
    return {
      type,
      title: defaultTitle(type),
      message: String(arg1 || ""),
    };
  }

  // Case 3: showToast("Title", "Success", "Message")
  if (typeof arg2 === "string" && validTypes.includes(arg2.trim().toLowerCase())) {
    const type = arg2.trim().toLowerCase();
    return {
      type,
      title: String(arg1 || defaultTitle(type)),
      message: String(arg3 || ""),
    };
  }

  return {
    type: "info",
    title: String(arg1 || "Notification"),
    message: String(arg2 || arg3 || ""),
  };
}

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    const fallback = (arg1, arg2, arg3) => {
      const { type, title, message } = normalizeToastParams(arg1, arg2, arg3);
      if (typeof console !== "undefined") {
        console.log(`[Toast ${type}] ${title}: ${message}`);
      }
    };
    fallback.showToast = fallback;
    return fallback;
  }
  const fn = (arg1, arg2, arg3) => ctx.showToast(arg1, arg2, arg3);
  fn.showToast = ctx.showToast;
  fn.dismissToast = ctx.dismissToast;
  return fn;
};


// ── Config ────────────────────────────────────────────────────────────────────

const TOAST_DURATION_MS = 4000;
const FADE_DURATION_MS  = 300;

const TYPE_CONFIG = {
  success: {
    Icon: CheckCircle,
    containerClass: "border-green-200 bg-white/90",
    iconClass: "text-green-500",
    titleClass: "text-green-800",
    msgClass: "text-green-600",
    bar: "bg-green-400",
  },
  error: {
    Icon: AlertCircle,
    containerClass: "border-red-200 bg-white/90",
    iconClass: "text-red-500",
    titleClass: "text-red-800",
    msgClass: "text-red-600",
    bar: "bg-red-400",
  },
  warning: {
    Icon: AlertTriangle,
    containerClass: "border-amber-200 bg-white/90",
    iconClass: "text-amber-500",
    titleClass: "text-amber-800",
    msgClass: "text-amber-600",
    bar: "bg-amber-400",
  },
  info: {
    Icon: Info,
    containerClass: "border-blue-200 bg-white/90",
    iconClass: "text-blue-500",
    titleClass: "text-blue-800",
    msgClass: "text-blue-600",
    bar: "bg-blue-400",
  },
};

// ── Individual Toast ──────────────────────────────────────────────────────────

function Toast({ id, type = "info", title, message, onDismiss }) {
  const cfg = TYPE_CONFIG[type] ?? TYPE_CONFIG.info;
  const { Icon } = cfg;

  const [visible, setVisible] = useState(false);
  const [progress, setProgress] = useState(100);
  const intervalRef = useRef(null);
  const startTimeRef = useRef(null);

  // Slide-in on mount
  useEffect(() => {
    const t = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(t);
  }, []);

  // Progress bar countdown + auto-dismiss
  useEffect(() => {
    startTimeRef.current = Date.now();
    intervalRef.current = setInterval(() => {
      const elapsed = Date.now() - startTimeRef.current;
      const remaining = Math.max(0, 100 - (elapsed / TOAST_DURATION_MS) * 100);
      setProgress(remaining);
      if (remaining === 0) {
        clearInterval(intervalRef.current);
        handleDismiss();
      }
    }, 50);

    return () => clearInterval(intervalRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDismiss = useCallback(() => {
    setVisible(false);
    setTimeout(() => onDismiss(id), FADE_DURATION_MS);
  }, [id, onDismiss]);

  return (
    <div
      role="alert"
      aria-live="assertive"
      style={{
        transition: `opacity ${FADE_DURATION_MS}ms ease, transform ${FADE_DURATION_MS}ms ease`,
        opacity: visible ? 1 : 0,
        transform: visible ? "translateX(0)" : "translateX(20px)",
      }}
      className={`relative w-80 rounded-xl border shadow-lg backdrop-blur px-4 py-3 flex items-start gap-3 overflow-hidden ${cfg.containerClass}`}
    >
      {/* Progress bar */}
      <div
        className={`absolute bottom-0 left-0 h-[3px] rounded-b-xl transition-all ${cfg.bar}`}
        style={{ width: `${progress}%`, transition: "width 50ms linear" }}
      />

      {/* Icon */}
      <div className="shrink-0 mt-0.5">
        <Icon className={`h-4.5 w-4.5 ${cfg.iconClass}`} />
      </div>

      {/* Text */}
      <div className="flex-1 min-w-0">
        <p className={`text-xs font-bold leading-tight ${cfg.titleClass}`}>{title}</p>
        {message && (
          <p className={`text-[11px] mt-0.5 leading-snug ${cfg.msgClass}`}>{message}</p>
        )}
      </div>

      {/* Dismiss button */}
      <button
        onClick={handleDismiss}
        className="shrink-0 text-gray-400 hover:text-gray-600 transition mt-0.5"
        aria-label="Dismiss notification"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

// ── Toast Container ───────────────────────────────────────────────────────────

export function ToastContainer({ toasts, onDismiss }) {
  if (!toasts.length) return null;

  return (
    <div
      aria-label="Notifications"
      className="fixed bottom-6 right-6 z-50 flex flex-col gap-2 items-end"
    >
      {toasts.map((toast) => (
        <Toast key={toast.id} {...toast} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

// ── Toast Provider ────────────────────────────────────────────────────────────

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((arg1, arg2, arg3) => {
    const { type, title, message } = normalizeToastParams(arg1, arg2, arg3);
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    setToasts((prev) => [...prev, { id, type, title, message }]);
  }, []);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  );
}

export default ToastProvider;
