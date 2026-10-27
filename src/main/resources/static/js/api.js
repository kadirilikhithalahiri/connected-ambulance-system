/**
 * Connected Ambulance System - API & CSRF Client
 * Handles authenticated session requests, CSRF token attachment, and HTTP error normalization.
 */
(function(window) {
    'use strict';

    let cachedCsrfToken = null;

    /**
     * Reads a cookie by name from document.cookie.
     * @param {string} name
     * @returns {string|null}
     */
    function getCookie(name) {
        if (!document.cookie) return null;
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.startsWith(name + '=')) {
                return decodeURIComponent(cookie.substring(name.length + 1));
            }
        }
        return null;
    }

    /**
     * Fetches or retrieves the current CSRF token from cookie or /api/auth/csrf.
     * @returns {Promise<string|null>}
     */
    async function getCsrfToken() {
        // First priority: read the XSRF-TOKEN cookie set by CookieCsrfTokenRepository
        const cookieToken = getCookie('XSRF-TOKEN');
        if (cookieToken) {
            cachedCsrfToken = cookieToken;
            return cookieToken;
        }

        if (cachedCsrfToken) {
            return cachedCsrfToken;
        }

        // Fallback: fetch from /api/auth/csrf endpoint
        try {
            const res = await fetch('/api/auth/csrf', {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            });
            if (res.ok) {
                const json = await res.json();
                if (json && json.data && json.data.token) {
                    cachedCsrfToken = json.data.token;
                    return cachedCsrfToken;
                }
            }
        } catch (e) {
            console.warn('Unable to retrieve CSRF token from endpoint:', e);
        }

        return null;
    }

    /**
     * Clears the in-memory cached CSRF token.
     * Invoked during logout or session invalidation to avoid stale token reuse.
     */
    function clearCsrf() {
        cachedCsrfToken = null;
    }

    /**
     * Main fetch wrapper for making API calls.
     * @param {string} url Relative URL path (e.g. /api/auth/login)
     * @param {Object} options Fetch request options
     * @returns {Promise<Object>} JSON response payload
     */
    async function apiFetch(url, options = {}) {
        const fetchOptions = {
            credentials: 'same-origin',
            ...options,
            headers: {
                'Accept': 'application/json',
                ...(options.headers || {})
            }
        };

        const method = (fetchOptions.method || 'GET').toUpperCase();

        // Mutating methods require JSON content type (unless FormData) and CSRF header
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            if (fetchOptions.body && typeof fetchOptions.body === 'string' && !fetchOptions.headers['Content-Type']) {
                fetchOptions.headers['Content-Type'] = 'application/json';
            }

            // Exclude login and csrf endpoint from CSRF header requirement if needed, but attach if available
            const csrfToken = await getCsrfToken();
            if (csrfToken) {
                fetchOptions.headers['X-XSRF-TOKEN'] = csrfToken;
            }
        }

        let response;
        try {
            response = await fetch(url, fetchOptions);
        } catch (networkError) {
            throw {
                status: 0,
                message: 'Network connection failed. Please check your connectivity and server availability.',
                data: null
            };
        }

        let responseData = null;
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            try {
                responseData = await response.json();
            } catch (jsonErr) {
                responseData = null;
            }
        }

        if (!response.ok) {
            const errorObj = {
                status: response.status,
                message: (responseData && responseData.message) ? responseData.message : `Request failed with status ${response.status}`,
                data: (responseData && responseData.data) ? responseData.data : null
            };

            // Global handling for session expiration (401 Unauthorized during navigation)
            if (response.status === 401 && !url.includes('/api/auth/login') && !url.includes('/api/auth/me')) {
                clearCsrf();
                if (window.EmergencyApp && typeof window.EmergencyApp.handleSessionExpired === 'function') {
                    window.EmergencyApp.handleSessionExpired();
                }
            }

            throw errorObj;
        }

        return responseData;
    }

    window.EmergencyApi = {
        apiFetch,
        getCsrfToken,
        clearCsrf,
        getCookie
    };

})(window);
