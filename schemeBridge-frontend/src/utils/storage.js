/**
 * Debounced localStorage utilities
 * Prevents excessive localStorage writes by batching updates
 */

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
    // Store the value to be written
    this.pendingWrites.set(key, value);

    // Clear existing timer for this key
    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key));
    }

    // Set new timer
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
      try {
        localStorage.setItem(key, JSON.stringify(value));
      } catch (error) {
        console.error(`Failed to write to localStorage for key "${key}":`, error);
      }
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
    try {
      const item = localStorage.getItem(key);
      if (item === null) {
return defaultValue;
}
      return JSON.parse(item);
    } catch (error) {
      console.error(`Failed to read from localStorage for key "${key}":`, error);
      return defaultValue;
    }
  }

  /**
   * Remove item from localStorage (immediate)
   * @param {string} key - Storage key
   */
  removeItem(key) {
    // Cancel any pending write for this key
    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key));
      this.timers.delete(key);
    }
    this.pendingWrites.delete(key);

    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error(`Failed to remove from localStorage for key "${key}":`, error);
    }
  }

  /**
   * Clear all items (immediate)
   */
  clear() {
    // Cancel all pending writes
    for (const timer of this.timers.values()) {
      clearTimeout(timer);
    }
    this.timers.clear();
    this.pendingWrites.clear();

    try {
      localStorage.clear();
    } catch (error) {
      console.error("Failed to clear localStorage:", error);
    }
  }

  /**
   * Get all keys matching a prefix
   * @param {string} prefix - Key prefix to filter
   * @returns {string[]} Array of matching keys
   */
  getKeysByPrefix(prefix) {
    const keys = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith(prefix)) {
        keys.push(key);
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

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      const value = localStorage.getItem(key);
      if (key && value) {
        totalSize += (key.length + value.length) * 2; // UTF-16 uses 2 bytes per character
        itemCount++;
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

// Flush all pending writes before page unload
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

export default debouncedStorage;
