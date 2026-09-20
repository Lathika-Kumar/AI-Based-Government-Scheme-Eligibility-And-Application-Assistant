/**
 * @file nameComparison.js
 * @description Centralized name normalization and deterministic matching utility
 * for comparing document holder names against authenticated citizen profiles.
 */

/**
 * Standard Indian honorific prefixes to strip during name normalization.
 */
const HONORIFICS = /^(?:(?:selvi|thiru|tmt|shri|sri|smt|mr|mrs|ms|miss|dr|kumari|master)[.\s]+)+/gi;

/**
 * Normalizes a personal name:
 * - Trims whitespace
 * - Strips common honorifics (Selvi, Thiru, Shri, Smt, Dr, etc.)
 * - Converts to lowercase
 * - Replaces punctuation and special characters with spaces
 * - Collapses multiple spaces into a single space
 *
 * @param {string} name
 * @returns {string}
 */
export function normalizeName(name) {
  if (!name || typeof name !== "string") return "";

  // Trim and strip honorifics
  let clean = name.trim();
  clean = clean.replace(HONORIFICS, "");

  // Convert to lowercase, remove punctuation, collapse whitespace
  return clean
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Checks whether one token is an initial of another (e.g. "k" and "kumar" or "k" and "k").
 * @param {string} tok1
 * @param {string} tok2
 * @returns {boolean}
 */
function isInitialMatch(tok1, tok2) {
  if (!tok1 || !tok2) return false;
  if (tok1.length === 1 && tok2.startsWith(tok1)) return true;
  if (tok2.length === 1 && tok1.startsWith(tok2)) return true;
  return false;
}

/**
 * Levenshtein distance for fuzzy matching typos in long name tokens (length >= 5).
 */
function levenshtein(a, b) {
  const matrix = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));
  for (let i = 0; i <= a.length; i++) matrix[i][0] = i;
  for (let j = 0; j <= b.length; j++) matrix[0][j] = j;

  for (let i = 1; i <= a.length; i++) {
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      matrix[i][j] = Math.min(
        matrix[i - 1][j] + 1,
        matrix[i][j - 1] + 1,
        matrix[i - 1][j - 1] + cost
      );
    }
  }
  return matrix[a.length][b.length];
}

/**
 * Checks if two major tokens (length >= 5) are a fuzzy match.
 */
function isFuzzyMajorMatch(s1, s2) {
  if (!s1 || !s2) return false;
  if (s1 === s2) return true;
  if (s1[0] !== s2[0]) return false;
  if (s1.length < 5 || s2.length < 5) return false;

  const c1 = s1.replace(/[aeiouy]/g, "");
  const c2 = s2.replace(/[aeiouy]/g, "");
  if (c1 && c1 === c2) return true;

  return levenshtein(s1, s2) <= 1;
}

/**
 * Determines whether the document holder name is consistent with the applicant profile name.
 * Handles:
 * - Identical names (case-insensitive, whitespace-agnostic)
 * - Name with initials (e.g. "Lathika.K", "Lathika K", "K Lathika" vs "Lathika" or "Lathika Kumar")
 * - Reversed first/last name order (e.g. "Kumar Lathika" vs "Lathika Kumar")
 * - Honorific prefixes (e.g. "Selvi Lathika" vs "Lathika.K")
 * - Disqualifies contradictory full names (e.g. "Aditya Kumar" vs "Lathika.K")
 *
 * @param {string} applicantName
 * @param {string} docHolderName
 * @returns {boolean}
 */
export function isNameConsistent(applicantName, docHolderName) {
  if (!applicantName || !docHolderName) return false;

  const normApplicant = normalizeName(applicantName);
  const normDoc = normalizeName(docHolderName);

  if (!normApplicant || !normDoc) return false;
  if (normApplicant === normDoc) return true;

  const appTokens = normApplicant.split(" ").filter(Boolean);
  const docTokens = normDoc.split(" ").filter(Boolean);

  if (appTokens.length === 0 || docTokens.length === 0) return false;

  // Filter major tokens (length >= 3)
  const appMajor = appTokens.filter((t) => t.length >= 3);
  const docMajor = docTokens.filter((t) => t.length >= 3);

  // If both have major tokens, they must share at least one major token
  if (appMajor.length > 0 && docMajor.length > 0) {
    let hasMajorOverlap = false;
    for (const am of appMajor) {
      for (const dm of docMajor) {
        if (am === dm || isFuzzyMajorMatch(am, dm)) {
          hasMajorOverlap = true;
          break;
        }
      }
      if (hasMajorOverlap) break;
    }

    if (!hasMajorOverlap) {
      return false; // Contradictory names (e.g. "aditya" vs "lathika")
    }
  } else {
    // Single initial or short token check
    const appFirst = appTokens[0];
    const docFirst = docTokens[0];
    if (appFirst !== docFirst && !isInitialMatch(appFirst, docFirst)) {
      return false;
    }
  }

  // Token containment & initial verification
  let matched = 0;
  for (const aTok of appTokens) {
    for (const dTok of docTokens) {
      if (aTok === dTok || isInitialMatch(aTok, dTok) || isFuzzyMajorMatch(aTok, dTok)) {
        matched++;
        break;
      }
    }
  }

  const minTokens = Math.min(appTokens.length, docTokens.length);
  return matched >= minTokens;
}

/**
 * High-level comparator comparing an applicant profile against an extracted document holder name.
 *
 * @param {object|string} applicant - Citizen profile object or name string
 * @param {string} extractedHolderName - Extracted holder name from document
 * @returns {{
 *   isMatch: boolean,
 *   status: "MATCH" | "MISMATCH" | "UNCERTAIN",
 *   applicantName: string,
 *   holderName: string,
 *   reason: string | null
 * }}
 */
export function compareApplicantName(applicant, extractedHolderName) {
  const applicantName = (
    typeof applicant === "string"
      ? applicant
      : applicant?.displayName || applicant?.name || ""
  ).trim();

  const holderName = (extractedHolderName || "").trim();

  if (!holderName) {
    return {
      isMatch: false,
      status: "UNCERTAIN",
      applicantName,
      holderName: "",
      reason: "Document holder name could not be detected. Please upload a clearer document.",
    };
  }

  if (!applicantName) {
    return {
      isMatch: true, // Cannot compare if no profile name available
      status: "MATCH",
      applicantName: "",
      holderName,
      reason: null,
    };
  }

  const isMatch = isNameConsistent(applicantName, holderName);

  if (isMatch) {
    return {
      isMatch: true,
      status: "MATCH",
      applicantName,
      holderName,
      reason: null,
    };
  }

  return {
    isMatch: false,
    status: "MISMATCH",
    applicantName,
    holderName,
    reason: `Applicant Name Mismatch: Document holder "${holderName}" does not match logged-in applicant profile "${applicantName}".`,
  };
}
