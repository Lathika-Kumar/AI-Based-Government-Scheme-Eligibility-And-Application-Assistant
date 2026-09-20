import { useEffect } from "react";

export function usePageMeta(title, description) {
  useEffect(() => {
    document.title = title ? `${title} — SchemeBridge` : "SchemeBridge";
    const meta = document.querySelector('meta[name="description"]');
    if (meta && description) {
      meta.setAttribute("content", description);
    }
  }, [title, description]);
}
