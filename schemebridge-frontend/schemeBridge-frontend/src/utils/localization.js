/**
 * Centralized safe localization resolver for SchemeBridge.
 *
 * Resolves localized strings from either plain string values, numbers,
 * or multilingual objects ({ english, tamil, translations, hindi, etc. }).
 *
 * Invariant: GUARANTEED to NEVER return a raw object as a React child.
 *
 * @param {any} value - String, localized object, number, or null/undefined
 * @param {string} [lang="en"] - Active language code (e.g. "en", "ta", "hi", "english", "tamil")
 * @param {string} [fallback=""] - Fallback string if no valid text can be resolved
 * @returns {string} - The resolved, human-readable localized string
 */
export function resolveLocalized(value, lang = "en", fallback = "") {
  // 1. Return safe empty/placeholder when value is null or undefined
  if (value === null || value === undefined) {
    return fallback;
  }

  // 2. Return the string directly if already a string
  if (typeof value === "string") {
    return value;
  }

  // Handle primitives (numbers, booleans)
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }

  // 3. If value is an object, resolve language
  if (typeof value === "object") {
    const normalizedLang = String(lang || "en").toLowerCase().trim();

    // Map of common language keys / aliases
    const langKeyMap = {
      en: ["english", "en"],
      ta: ["tamil", "ta"],
      hi: ["hindi", "hi"],
      english: ["english", "en"],
      tamil: ["tamil", "ta"],
      hindi: ["hindi", "hi"],
    };

    const targetKeys = langKeyMap[normalizedLang] || [normalizedLang];

    // Check direct properties on the object matching active language
    for (const k of targetKeys) {
      if (typeof value[k] === "string" && value[k].trim()) {
        return value[k];
      }
    }

    // Check nested translations dictionary if present
    if (value.translations && typeof value.translations === "object") {
      for (const k of targetKeys) {
        if (typeof value.translations[k] === "string" && value.translations[k].trim()) {
          return value.translations[k];
        }
      }
    }

    // 4. Fall back to English when active language is unavailable
    if (typeof value.english === "string" && value.english.trim()) {
      return value.english;
    }
    if (typeof value.en === "string" && value.en.trim()) {
      return value.en;
    }
    if (value.translations && typeof value.translations === "object") {
      if (typeof value.translations.english === "string" && value.translations.english.trim()) {
        return value.translations.english;
      }
      if (typeof value.translations.en === "string" && value.translations.en.trim()) {
        return value.translations.en;
      }
    }

    // 5. Fall back to another valid string in the object
    if (typeof value.tamil === "string" && value.tamil.trim()) {
      return value.tamil;
    }
    if (typeof value.hindi === "string" && value.hindi.trim()) {
      return value.hindi;
    }

    // Check if there are any non-empty string values at all in the object
    for (const key of Object.keys(value)) {
      if (key !== "translations" && typeof value[key] === "string" && value[key].trim()) {
        return value[key];
      }
    }

    // If translations object has string values
    if (value.translations && typeof value.translations === "object") {
      for (const key of Object.keys(value.translations)) {
        if (typeof value.translations[key] === "string" && value.translations[key].trim()) {
          return value.translations[key];
        }
      }
    }

    // Check common nested text keys (name, title, label, text)
    if (typeof value.name === "string" && value.name.trim()) return value.name;
    if (typeof value.title === "string" && value.title.trim()) return value.title;
    if (typeof value.label === "string" && value.label.trim()) return value.label;
    if (typeof value.text === "string" && value.text.trim()) return value.text;

    // 6. NEVER return the raw object to JSX
    return fallback;
  }

  return fallback;
}

export const getLocalizedText = resolveLocalized;
export default resolveLocalized;
