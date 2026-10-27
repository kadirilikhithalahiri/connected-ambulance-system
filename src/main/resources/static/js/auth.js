/**
 * Connected Ambulance System - Authentication Module
 * Manages authenticated user state strictly in memory; derives credentials and sessions from Spring Security cookies.
 */
(function(window) {
    'use strict';

    let currentUser = null;

    /**
     * Checks if there is an active Spring Security session by calling /api/auth/me.
     * @returns {Promise<Object|null>} User object if authenticated, null otherwise
     */
    async function checkSession() {
        try {
            const res = await window.EmergencyApi.apiFetch('/api/auth/me', { method: 'GET' });
            if (res && res.data) {
                currentUser = res.data;
                return currentUser;
            }
        } catch (err) {
            currentUser = null;
        }
        return null;
    }

    /**
     * Authenticates with username and password against /api/auth/login.
     * @param {string} username
     * @param {string} password
     * @returns {Promise<Object>} Authenticated user profile
     */
    async function login(username, password) {
        if (!username || !username.trim() || !password) {
            throw {
                status: 400,
                message: 'Username and password are required.',
                data: null
            };
        }

        const payload = {
            username: username.trim(),
            password: password
        };

        const res = await window.EmergencyApi.apiFetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res && res.data) {
            currentUser = res.data;
            return currentUser;
        }

        throw {
            status: 500,
            message: 'Malformed response received from login endpoint.',
            data: null
        };
    }

    /**
     * Logs out the user via /api/auth/logout and clears in-memory state.
     * @returns {Promise<boolean>} True if server-side logout succeeded, false if API notification failed
     */
    async function logout() {
        let serverSuccess = false;
        try {
            await window.EmergencyApi.apiFetch('/api/auth/logout', { method: 'POST' });
            serverSuccess = true;
        } catch (err) {
            console.warn('Logout API notification error:', err);
            serverSuccess = false;
        } finally {
            currentUser = null;
            if (window.EmergencyApi && typeof window.EmergencyApi.clearCsrf === 'function') {
                window.EmergencyApi.clearCsrf();
            }
        }
        return serverSuccess;
    }

    /**
     * Returns the currently authenticated user in memory.
     * @returns {Object|null}
     */
    function getCurrentUser() {
        return currentUser;
    }

    window.EmergencyAuth = {
        checkSession,
        login,
        logout,
        getCurrentUser
    };

})(window);
