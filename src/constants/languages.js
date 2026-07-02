/**
 * @file languages.js
 * @description Supported application languages.
 * Backend integration: fetch from /api/config/languages to enable dynamic language management.
 */

/** Supported languages with code, English label, and native label */
export const LANGUAGES = Object.freeze([
  { code: "en", label: "English", nativeLabel: "English" },
  { code: "hi", label: "Hindi", nativeLabel: "हिंदी" },
  { code: "ta", label: "Tamil", nativeLabel: "தமிழ்" },
  { code: "te", label: "Telugu", nativeLabel: "తెలుగు" },
  { code: "kn", label: "Kannada", nativeLabel: "ಕನ್ನಡ" },
  { code: "mr", label: "Marathi", nativeLabel: "मराठी" },
  { code: "bn", label: "Bengali", nativeLabel: "বাংলা" },
  { code: "gu", label: "Gujarati", nativeLabel: "ગુજરાતી" },
]);

/** Default language code */
export const DEFAULT_LANGUAGE = "en";

/** Map of language code → language object for O(1) lookup */
export const LANGUAGE_MAP = Object.freeze(
  Object.fromEntries(LANGUAGES.map((lang) => [lang.code, lang]))
);

/**
 * Returns the native label for a given language code.
 * Falls back to code if not found.
 * @param {string} code
 * @returns {string}
 */
export function getLanguageLabel(code) {
  return LANGUAGE_MAP[code]?.nativeLabel ?? code;
}
