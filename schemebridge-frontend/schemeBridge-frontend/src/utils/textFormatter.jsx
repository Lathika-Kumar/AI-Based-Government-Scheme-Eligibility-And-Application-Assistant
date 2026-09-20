import React from "react";
import { resolveLocalized } from "./localization";

export { resolveLocalized };

/**
 * Safely renders plain text that contains literal <br>, <br/>, <br />, \r\n, or \n as React DOM elements.
 * 
 * SECURITY:
 * Fully immune to XSS because string tokens are passed as standard React child text nodes,
 * never evaluated via dangerouslySetInnerHTML. Line breaks are rendered as <br /> JSX elements.
 *
 * @param {string|object|null|undefined} text - The raw input text or localization object
 * @param {string} [lang="en"] - Language code if text is a localization object
 * @returns {React.ReactNode}
 */
export function renderFormattedText(text, lang = "en") {
  if (text === "") return "";
  if (text === null || text === undefined) return null;
  
  // Resolve localized text if an object was passed
  const resolved = typeof text === "object" ? resolveLocalized(text, lang, "") : (typeof text !== "string" ? String(text) : text);
  if (!resolved) return "";

  // Split on <br>, <br/>, <br />, \r\n, \n (case-insensitive)
  const parts = resolved.split(/(?:<br\s*\/?>|\r?\n)/gi);
  if (parts.length === 1) {
    return parts[0];
  }

  return parts.map((part, idx) => (
    <React.Fragment key={idx}>
      {idx > 0 && <br />}
      {part}
    </React.Fragment>
  ));
}

export default renderFormattedText;
