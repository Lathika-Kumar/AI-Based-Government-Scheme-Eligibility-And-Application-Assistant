/**
 * @file storage.js
 * @description Safe and Debounced localStorage utilities with QuotaExceededError protection
 * and legacy scheme cache migration.
 *
 * CRITICAL ARCHITECTURAL RULE:
 * Large datasets (such as the 4,734 schemes catalog) must NEVER be persisted to browser storage.
 * Only small user-scoped state, preferences, and identifiers are permitted.
 */

/**
 * Get available localStorage instance across browser and test environments.
 */
function getStorage() {
  try {
    if (typeof globalThis !== "undefined" && globalThis.localStorage) {
      return globalThis.localStorage;
    }
    if (typeof window !== "undefined" && window.localStorage) {
      return window.localStorage;
    }
  } catch {
    // Storage might be restricted / disabled by browser policy
  }
  return null;
}

/**
 * Check if a thrown error is a browser QuotaExceededError across browsers and environments.
 */
export function isQuotaExceededError(err) {
  if (!err) return false;
  return (
    err.name === "QuotaExceededError" ||
    err.name === "NS_ERROR_DOM_QUOTA_REACHED" ||
    err.code === 22 ||
    err.code === 1014 ||
    (typeof err.message === "string" && err.message.toLowerCase().includes("quota"))
  );
}

/**
 * Idempotent migration to detect and remove legacy large scheme catalog cache
 * from localStorage without affecting user auth, profile, or session state.
 */
export function cleanupLegacySchemeCache() {
  const storage = getStorage();
  if (!storage) return;

  try {
    if (storage.getItem("schemebridge_schemes") !== null) {
      storage.removeItem("schemebridge_schemes");
      if (typeof process === "undefined" || process.env?.NODE_ENV !== "test") {
        console.info("[StorageMigration] Safely removed legacy 'schemebridge_schemes' full catalog cache from localStorage.");
      }
    }
  } catch (err) {
    console.warn("[StorageMigration] Unable to access localStorage during legacy key cleanup:", err);
  }
}

/**
 * Defensive localStorage write. Catches QuotaExceededError and other storage failures
 * without crashing React execution or throwing unhandled exceptions.
 *
 * @param {string} key - Storage key
 * @param {any} value - Value to serialize (or raw string if already stringified)
 * @returns {boolean} true if write succeeded, false otherwise
 */
export function safeSetItem(key, value) {
  const storage = getStorage();
  if (!storage) return false;

  try {
    const serialized = typeof value === "string" ? value : JSON.stringify(value);
    storage.setItem(key, serialized);
    return true;
  } catch (error) {
    if (isQuotaExceededError(error)) {
      console.warn(`[SafeStorage] Storage quota exceeded while writing key "${key}". Write skipped safely.`, error);
    } else {
      console.warn(`[SafeStorage] Failed to write key "${key}" to localStorage:`, error);
    }
    return false;
  }
}

/**
 * Defensive localStorage read with JSON parse fallback.
 *
 * @param {string} key - Storage key
 * @param {any} defaultValue - Value returned if key is missing or parsing fails
 * @returns {any} Parsed value or defaultValue
 */
export function safeGetItem(key, defaultValue = null) {
  const storage = getStorage();
  if (!storage) return defaultValue;

  try {
    const item = storage.getItem(key);
    if (item === null) {
      return defaultValue;
    }
    try {
      return JSON.parse(item);
    } catch {
      return item;
    }
  } catch (error) {
    console.warn(`[SafeStorage] Failed to read key "${key}" from localStorage:`, error);
    return defaultValue;
  }
}

/**
 * Defensive localStorage removal.
 *
 * @param {string} key - Storage key
 * @returns {boolean} true if removal succeeded
 */
export function safeRemoveItem(key) {
  const storage = getStorage();
  if (!storage) return false;

  try {
    storage.removeItem(key);
    return true;
  } catch (error) {
    console.warn(`[SafeStorage] Failed to remove key "${key}" from localStorage:`, error);
    return false;
  }
}

class DebouncedStorage {
  constructor(defaultDelay = 500) {
    this.defaultDelay = defaultDelay;
    this.timers = new Map();
    this.pendingWrites = new Map();
  }

  /**
   * Debounced write to localStorage
   * @param {string} key - Storage key
   * @param {any} value - Value to store
   * @param {number} delay - Delay in milliseconds
   */
  setItem(key, value, delay = this.defaultDelay) {
    this.pendingWrites.set(key, value);

    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key));
    }

    const timer = setTimeout(() => {
      this.flushItem(key);
    }, delay);

    this.timers.set(key, timer);
  }

  /**
   * Immediately write a pending item to localStorage
   * @param {string} key - Storage key
   */
  flushItem(key) {
    if (this.pendingWrites.has(key)) {
      const value = this.pendingWrites.get(key);
      safeSetItem(key, value);
      this.pendingWrites.delete(key);
      this.timers.delete(key);
    }
  }

  /**
   * Flush all pending writes immediately
   */
  flushAll() {
    for (const key of this.pendingWrites.keys()) {
      this.flushItem(key);
    }
  }

  /**
   * Get item from localStorage
   * @param {string} key - Storage key
   * @param {any} defaultValue - Default value if key doesn't exist
   * @returns {any} Parsed value or default
   */
  getItem(key, defaultValue = null) {
    return safeGetItem(key, defaultValue);
  }

  /**
   * Remove item from localStorage (immediate)
   * @param {string} key - Storage key
   */
  removeItem(key) {
    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key));
      this.timers.delete(key);
    }
    this.pendingWrites.delete(key);
    safeRemoveItem(key);
  }

  /**
   * Clear all items (immediate)
   */
  clear() {
    for (const timer of this.timers.values()) {
      clearTimeout(timer);
    }
    this.timers.clear();
    this.pendingWrites.clear();

    const storage = getStorage();
    if (storage) {
      try {
        storage.clear();
      } catch (error) {
        console.error("[SafeStorage] Failed to clear localStorage:", error);
      }
    }
  }

  /**
   * Get all keys matching a prefix
   * @param {string} prefix - Key prefix to filter
   * @returns {string[]} Array of matching keys
   */
  getKeysByPrefix(prefix) {
    const keys = [];
    const storage = getStorage();
    if (storage) {
      try {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          if (key && key.startsWith(prefix)) {
            keys.push(key);
          }
        }
      } catch (err) {
        console.warn("[SafeStorage] Error reading keys by prefix:", err);
      }
    }
    return keys;
  }

  /**
   * Remove all keys matching a prefix
   * @param {string} prefix - Key prefix to filter
   */
  removeByPrefix(prefix) {
    const keys = this.getKeysByPrefix(prefix);
    keys.forEach(key => this.removeItem(key));
  }

  /**
   * Get storage usage statistics
   * @returns {Object} Storage stats
   */
  getStats() {
    let totalSize = 0;
    let itemCount = 0;
    const storage = getStorage();

    if (storage) {
      try {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          const value = storage.getItem(key);
          if (key && value) {
            totalSize += (key.length + value.length) * 2; // UTF-16 uses 2 bytes per character
            itemCount++;
          }
        }
      } catch (err) {
        console.warn("[SafeStorage] Error reading storage stats:", err);
      }
    }

    return {
      itemCount,
      totalSize,
      totalSizeKB: (totalSize / 1024).toFixed(2),
      totalSizeMB: (totalSize / (1024 * 1024)).toFixed(2),
      pendingWrites: this.pendingWrites.size,
    };
  }
}

// Create default instance
const debouncedStorage = new DebouncedStorage(500);

// Flush all pending writes before page unload in browser environment
if (typeof window !== "undefined") {
  window.addEventListener("beforeunload", () => {
    debouncedStorage.flushAll();
  });

  // Also flush on page visibility change (user switching tabs)
  window.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") {
      debouncedStorage.flushAll();
    }
  });
}

// Run legacy cleanup once on module evaluation
cleanupLegacySchemeCache();

export default debouncedStorage;
