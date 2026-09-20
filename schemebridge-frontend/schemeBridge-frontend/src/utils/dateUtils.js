/**
 * @file dateUtils.js
 * @description Centralized date formatting, parsing, and strict calendar validation.
 * Ensures consistent handling of Indian date formats (DD/MM/YYYY) and ISO formats (YYYY-MM-DD).
 */

/**
 * Convert ISO date string (YYYY-MM-DD or ISO timestamp) to DD/MM/YYYY for UI display.
 * @param {string|Date} isoStr 
 * @returns {string} Formatted DD/MM/YYYY string or original trimmed string
 */
export function formatIsoToDisplay(isoStr) {
  if (!isoStr) return "";
  const s = String(isoStr).trim();
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
    const parts = s.substring(0, 10).split("-");
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) {
    return s;
  }
  return s;
}

/**
 * Strict calendar date validation.
 * Rejects malformed dates (e.g. 31/02/2020, 99/99/9999), future dates, and out-of-range years.
 * Returns ISO representation (YYYY-MM-DD), calculated age, and validation error if any.
 * 
 * @param {string} displayStr - Date in DD/MM/YYYY or YYYY-MM-DD format
 * @param {{ minAge?: number, maxAge?: number, requireAdult?: boolean }} options
 * @returns {{ iso: string|null, calculatedAge: number|null, error: string|null }}
 */
export function parseDisplayToIso(displayStr, options = {}) {
  if (!displayStr || !String(displayStr).trim()) {
    return { iso: null, calculatedAge: null, error: "Date of birth is required" };
  }
  const trimmed = String(displayStr).trim();

  let day, month, year;
  const dmyMatch = trimmed.match(/^(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})$/);
  const ymdMatch = trimmed.match(/^(\d{4})[/.-](\d{1,2})[/.-](\d{1,2})$/);

  if (dmyMatch) {
    day = parseInt(dmyMatch[1], 10);
    month = parseInt(dmyMatch[2], 10);
    year = parseInt(dmyMatch[3], 10);
  } else if (ymdMatch) {
    year = parseInt(ymdMatch[1], 10);
    month = parseInt(ymdMatch[2], 10);
    day = parseInt(ymdMatch[3], 10);
  } else {
    return { iso: null, calculatedAge: null, error: "Please enter a valid date in DD/MM/YYYY format" };
  }

  if (month < 1 || month > 12) {
    return { iso: null, calculatedAge: null, error: "Month must be between 01 and 12" };
  }

  // Days in month validation (including leap year)
  const isLeap = (year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0);
  const daysInMonth = [31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

  if (day < 1 || day > daysInMonth[month - 1]) {
    return {
      iso: null,
      calculatedAge: null,
      error: `Invalid date: Day ${day} does not exist in month ${month}`
    };
  }

  const currentYear = new Date().getFullYear();
  if (year < 1900) {
    return { iso: null, calculatedAge: null, error: "Year must be 1900 or later" };
  }
  if (year > currentYear) {
    return { iso: null, calculatedAge: null, error: "Date of birth cannot be in the future" };
  }

  const birthDate = new Date(year, month - 1, day);
  const today = new Date();
  if (birthDate > today) {
    return { iso: null, calculatedAge: null, error: "Date of birth cannot be in the future" };
  }

  let calculatedAge = today.getFullYear() - year;
  const monthDiff = today.getMonth() - (month - 1);
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < day)) {
    calculatedAge--;
  }

  const minAge = options.minAge ?? 0;
  const maxAge = options.maxAge ?? 120;

  if (calculatedAge < minAge) {
    return { iso: null, calculatedAge, error: `Minimum age required is ${minAge} years` };
  }
  if (calculatedAge > maxAge) {
    return { iso: null, calculatedAge, error: `Age must be less than ${maxAge} years` };
  }

  const pad = (n) => String(n).padStart(2, "0");
  const iso = `${year}-${pad(month)}-${pad(day)}`;

  return { iso, calculatedAge, error: null };
}

/**
 * Check if a given string represents a valid calendar date.
 */
export function isValidCalendarDate(dateStr) {
  const res = parseDisplayToIso(dateStr);
  return !res.error && res.iso !== null;
}
