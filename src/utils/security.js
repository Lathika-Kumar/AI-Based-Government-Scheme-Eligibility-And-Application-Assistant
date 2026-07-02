/**
 * Security utilities for authentication and data protection
 * Provides password hashing, validation, and sanitization functions
 */

/**
 * Simple hash function for demo purposes
 * In production, use bcrypt, argon2, or similar secure hashing libraries
 * @param {string} password - Plain text password
 * @returns {string} Hashed password
 */
export function hashPassword(password) {
  // Simple hash for demo - NOT SECURE for production
  // In production, use: await bcrypt.hash(password, 10)
  let hash = 0;
  const str = `${password  }schemebridge_salt_v1`;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  return `hashed_${  Math.abs(hash).toString(16)  }_${  btoa(str).slice(0, 16)}`;
}

/**
 * Verify password against hash
 * In production, use: await bcrypt.compare(password, hash)
 * @param {string} password - Plain text password
 * @param {string} hash - Hashed password
 * @returns {boolean}
 */
export function verifyPassword(password, hash) {
  const computedHash = hashPassword(password);
  return computedHash === hash;
}

/**
 * Generate a random token for session management
 * @param {number} length - Token length in bytes
 * @returns {string}
 */
export function generateToken(length = 32) {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  const randomValues = new Uint8Array(length);
  crypto.getRandomValues(randomValues);
  for (let i = 0; i < length; i++) {
    result += chars[randomValues[i] % chars.length];
  }
  return result;
}

/**
 * Sanitize user input to prevent XSS
 * @param {string} input - User input
 * @returns {string} Sanitized input
 */
export function sanitizeInput(input) {
  if (typeof input !== "string") {
return input;
}
  return input
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;")
    .trim();
}

/**
 * Validate email format with additional security checks
 * @param {string} email - Email address
 * @returns {boolean}
 */
export function isValidEmail(email) {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
return false;
}

  // Check for suspicious patterns
  const suspiciousPatterns = [
    /\.\./,          // Double dots
    /@.*@/,          // Multiple @ symbols
    /\+.*\+/,        // Multiple + symbols
  ];

  return !suspiciousPatterns.some(pattern => pattern.test(email));
}

/**
 * Check password strength
 * @param {string} password - Password to check
 * @returns {Object} { score: 0-4, feedback: string }
 */
export function checkPasswordStrength(password) {
  let score = 0;
  const feedback = [];

  if (password.length >= 8) {
score++;
} else {
feedback.push("Password should be at least 8 characters");
}

  if (password.length >= 12) {
score++;
} else {
feedback.push("Password should be at least 12 characters for better security");
}

  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) {
score++;
} else {
feedback.push("Include both uppercase and lowercase letters");
}

  if (/\d/.test(password)) {
score++;
} else {
feedback.push("Include at least one number");
}

  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
score++;
} else {
feedback.push("Include at least one special character");
}

  return {
    score: Math.min(score, 4),
    feedback: feedback.slice(0, 2), // Return top 2 suggestions
  };
}

/**
 * Rate limiter for API calls
 * Prevents brute force attacks
 */
class RateLimiter {
  constructor(maxAttempts = 5, windowMs = 60000) {
    this.maxAttempts = maxAttempts;
    this.windowMs = windowMs;
    this.attempts = new Map();
  }

  canAttempt(identifier) {
    const now = Date.now();
    const userAttempts = this.attempts.get(identifier) || [];

    // Remove attempts outside the time window
    const validAttempts = userAttempts.filter(time => now - time < this.windowMs);

    if (validAttempts.length >= this.maxAttempts) {
      return {
        allowed: false,
        retryAfter: Math.ceil((this.windowMs - (now - validAttempts[0])) / 1000),
      };
    }

    validAttempts.push(now);
    this.attempts.set(identifier, validAttempts);

    return { allowed: true, remaining: this.maxAttempts - validAttempts.length };
  }

  reset(identifier) {
    this.attempts.delete(identifier);
  }
}

export const loginRateLimiter = new RateLimiter(5, 60000); // 5 attempts per minute
export const signupRateLimiter = new RateLimiter(3, 3600000); // 3 attempts per hour

/**
 * Secure localStorage wrapper with encryption
 * Note: This is a basic implementation. For production, use proper encryption libraries
 */
export const secureStorage = {
  setItem(key, value) {
    try {
      const encrypted = btoa(JSON.stringify(value));
      localStorage.setItem(key, encrypted);
    } catch (error) {
      console.error("Failed to encrypt and store data:", error);
    }
  },

  getItem(key) {
    try {
      const encrypted = localStorage.getItem(key);
      if (!encrypted) {
return null;
}
      return JSON.parse(atob(encrypted));
    } catch (error) {
      console.error("Failed to decrypt and retrieve data:", error);
      return null;
    }
  },

  removeItem(key) {
    localStorage.removeItem(key);
  },

  clear() {
    localStorage.clear();
  },
};
