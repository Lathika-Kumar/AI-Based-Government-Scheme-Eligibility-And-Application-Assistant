/**
 * API Client with interceptors and error handling
 * Provides a centralized HTTP client for all API requests
 */

class ApiClient {
  constructor(baseURL = "/api/v1", defaultHeaders = {}) {
    this.baseURL = baseURL;
    this.defaultHeaders = {
      "Content-Type": "application/json",
      ...defaultHeaders,
    };
    this.requestInterceptors = [];
    this.responseInterceptors = [];
    this.errorHandlers = [];
  }

  /**
   * Add a request interceptor
   * @param {Function} interceptor - Function that receives config and returns modified config
   */
  addRequestInterceptor(interceptor) {
    this.requestInterceptors.push(interceptor);
  }

  /**
   * Add a response interceptor
   * @param {Function} interceptor - Function that receives response and returns modified response
   */
  addResponseInterceptor(interceptor) {
    this.responseInterceptors.push(interceptor);
  }

  /**
   * Add an error handler
   * @param {Function} handler - Function that receives error and returns modified error or throws
   */
  addErrorHandler(handler) {
    this.errorHandlers.push(handler);
  }

  /**
   * Build full URL from endpoint
   * @param {string} endpoint - API endpoint
   * @returns {string} Full URL
   */
  buildUrl(endpoint) {
    return `${this.baseURL}${endpoint}`;
  }

  /**
   * Apply request interceptors
   * @param {Object} config - Request config
   * @returns {Object} Modified config
   */
  async applyRequestInterceptors(config) {
    let modifiedConfig = { ...config };
    for (const interceptor of this.requestInterceptors) {
      modifiedConfig = await interceptor(modifiedConfig);
    }
    return modifiedConfig;
  }

  /**
   * Apply response interceptors
   * @param {Object} response - Response object
   * @returns {Object} Modified response
   */
  async applyResponseInterceptors(response) {
    let modifiedResponse = { ...response };
    for (const interceptor of this.responseInterceptors) {
      modifiedResponse = await interceptor(modifiedResponse);
    }
    return modifiedResponse;
  }

  /**
   * Apply error handlers
   * @param {Error} error - Error object
   * @returns {Error} Modified error or re-thrown
   */
  async applyErrorHandlers(error) {
    let modifiedError = error;
    for (const handler of this.errorHandlers) {
      try {
        modifiedError = await handler(modifiedError);
      } catch (e) {
        // If handler throws, continue to next handler
        modifiedError = e;
      }
    }
    throw modifiedError;
  }

  /**
   * Make HTTP request
   * @param {string} method - HTTP method
   * @param {string} endpoint - API endpoint
   * @param {Object} options - Request options
   * @returns {Promise<Object>} Response data
   */
  async request(method, endpoint, options = {}) {
    const {
      headers = {},
      body,
      params,
      timeout = 10000,
      retries = 3,
      retryDelay = 1000,
    } = options;

    let config = {
      method,
      url: this.buildUrl(endpoint),
      headers: { ...this.defaultHeaders, ...headers },
      body: body ? JSON.stringify(body) : undefined,
      params,
      timeout,
    };

    // Apply request interceptors
    config = await this.applyRequestInterceptors(config);

    // Add query parameters to URL
    if (params) {
      const queryString = new URLSearchParams(params).toString();
      config.url = `${config.url}${queryString ? `?${queryString}` : ""}`;
    }

    let attempt = 0;
    let lastError;

    while (attempt < retries) {
      attempt++;
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeout);

        const response = await fetch(config.url, {
          method: config.method,
          headers: config.headers,
          body: config.body,
          signal: controller.signal,
        });

        clearTimeout(timeoutId);

        // Handle non-OK responses
        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}));
          const error = new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
          error.status = response.status;
          error.data = errorData;
          throw error;
        }

        const data = await response.json();

        // Apply response interceptors
        const modifiedResponse = await this.applyResponseInterceptors({
          data,
          status: response.status,
          headers: response.headers,
        });

        return modifiedResponse.data;

      } catch (error) {
        lastError = error;

        // Don't retry on abort or 4xx errors (except 429)
        if (error.name === "AbortError" || (error.status >= 400 && error.status < 500 && error.status !== 429)) {
          break;
        }

        // Wait before retrying
        if (attempt < retries) {
          await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
        }
      }
    }

    // Apply error handlers
    return this.applyErrorHandlers(lastError);
  }

  /**
   * GET request
   */
  async get(endpoint, options = {}) {
    return this.request("GET", endpoint, options);
  }

  /**
   * POST request
   */
  async post(endpoint, body, options = {}) {
    return this.request("POST", endpoint, { ...options, body });
  }

  /**
   * PUT request
   */
  async put(endpoint, body, options = {}) {
    return this.request("PUT", endpoint, { ...options, body });
  }

  /**
   * PATCH request
   */
  async patch(endpoint, body, options = {}) {
    return this.request("PATCH", endpoint, { ...options, body });
  }

  /**
   * DELETE request
   */
  async delete(endpoint, options = {}) {
    return this.request("DELETE", endpoint, options);
  }
}

// Create default API client instance
const apiClient = new ApiClient();

// Add authentication interceptor
apiClient.addRequestInterceptor(async (config) => {
  const token = localStorage.getItem("schemebridge_token");
  if (token) {
    config.headers = {
      ...config.headers,
      Authorization: `Bearer ${token}`,
    };
  }
  return config;
});

// Add request ID interceptor for tracking
apiClient.addRequestInterceptor(async (config) => {
  config.headers = {
    ...config.headers,
    "X-Request-ID": crypto.randomUUID(),
  };
  return config;
});

// Add response logging interceptor (development only)
if (import.meta.env.DEV) {
  apiClient.addResponseInterceptor(async (response) => {
    console.log(`API Response [${response.status}]:`, response.data);
    return response;
  });
}

// Add error handler
apiClient.addErrorHandler(async (error) => {
  console.error("API Error:", error);

  // Handle specific error codes
  if (error.status === 401) {
    // Unauthorized - clear token and redirect to login
    localStorage.removeItem("schemebridge_token");
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  }

  if (error.status === 429) {
    // Rate limited - extract retry-after header
    const retryAfter = error.data?.retryAfter || 60;
    error.message = `Too many requests. Please try again in ${retryAfter} seconds.`;
  }

  if (error.status === 500) {
    error.message = "Server error. Please try again later.";
  }

  if (error.name === "AbortError") {
    error.message = "Request timeout. Please check your connection.";
  }

  return error;
});

export default apiClient;
