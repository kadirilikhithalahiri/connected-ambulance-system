/**
 * Connected Ambulance System - Main Application Controller
 * Coordinates UI state transitions, responsive navigation, module placeholders, and error display.
 */
(function(window) {
    'use strict';

    // Module definitions for the dashboard and placeholder views
    const MODULES = {
        'dashboard': {
            title: 'Dashboard Overview',
            icon: '📊',
            desc: 'System operational status and quick navigation.',
            status: 'Operational'
        },
        'patients': {
            title: 'Patient Registry',
            icon: '👤',
            desc: 'Secure patient profiles, medical histories, allergies, and emergency contacts.',
            apiEndpoint: '/api/patients',
            status: 'Operational'
        },
        'cases': {
            title: 'Emergency Cases',
            icon: '🚨',
            desc: 'Atomic emergency triage registration, symptoms tracking, and status lifecycle management.',
            apiEndpoint: '/api/cases',
            status: 'Operational'
        },
        'ambulances': {
            title: 'Ambulance Tracking',
            icon: '🚑',
            desc: 'Real-time vehicle availability, equipment inventory, and GPS location tracking.',
            apiEndpoint: '/api/ambulances',
            status: 'Operational'
        },
        'dispatches': {
            title: 'Dispatch Management',
            icon: '📡',
            desc: 'Emergency dispatch coordination, pre-departure cancellation, and driver missions.',
            apiEndpoint: '/api/dispatches',
            status: 'Operational'
        },
        'reports': {
            title: 'Reports & Analytics',
            icon: '📈',
            desc: 'Utilization reports, response time statistics, and operational readiness metrics.',
            apiEndpoint: '/api/cases, /api/ambulances, /api/dispatches',
            status: 'Operational'
        }
    };

    // DOM Elements Cache
    let elements = {};

    function cacheElements() {
        elements = {
            loadingOverlay: document.getElementById('loadingOverlay'),
            loginView: document.getElementById('loginView'),
            appShell: document.getElementById('appShell'),
            loginForm: document.getElementById('loginForm'),
            usernameInput: document.getElementById('username'),
            passwordInput: document.getElementById('password'),
            loginBtn: document.getElementById('loginBtn'),
            loginAlert: document.getElementById('loginAlert'),
            globalAlert: document.getElementById('globalAlert'),
            
            // Header / User display
            headerTitle: document.getElementById('headerTitle'),
            userNameDisplay: document.getElementById('userNameDisplay'),
            userRoleDisplay: document.getElementById('userRoleDisplay'),
            userAvatarDisplay: document.getElementById('userAvatarDisplay'),
            logoutBtn: document.getElementById('logoutBtn'),
            menuToggleBtn: document.getElementById('menuToggleBtn'),
            sidebarOverlay: document.getElementById('sidebarOverlay'),
            appSidebar: document.getElementById('appSidebar'),

            // Views
            viewDashboard: document.getElementById('viewDashboard'),
            viewPatients: document.getElementById('viewPatients'),
            viewCases: document.getElementById('viewCases'),
            viewAmbulances: document.getElementById('viewAmbulances'),
            viewDispatches: document.getElementById('viewDispatches'),
            viewReports: document.getElementById('viewReports'),
            viewPlaceholder: document.getElementById('viewPlaceholder'),
            placeholderTitle: document.getElementById('placeholderTitle'),
            placeholderIcon: document.getElementById('placeholderIcon'),
            placeholderDesc: document.getElementById('placeholderDesc'),
            placeholderStatusBadge: document.getElementById('placeholderStatusBadge'),
            placeholderNote: document.getElementById('placeholderNote'),
            placeholderBackBtn: document.getElementById('placeholderBackBtn'),

            // Dashboard User Welcome
            welcomeTitle: document.getElementById('welcomeTitle'),
            welcomeRole: document.getElementById('welcomeRole'),
            systemRoleVal: document.getElementById('systemRoleVal'),
            systemUserIdVal: document.getElementById('systemUserIdVal')
        };
    }

    /**
     * Shows or hides the full-screen loading spinner.
     */
    function showLoading(show) {
        if (elements.loadingOverlay) {
            if (show) {
                elements.loadingOverlay.classList.remove('hidden');
            } else {
                elements.loadingOverlay.classList.add('hidden');
            }
        }
    }

    /**
     * Displays a dismissible alert box.
     */
    function showAlert(container, message, type = 'error') {
        if (!container) return;
        container.className = `alert alert-${type}`;
        container.innerHTML = `<span>${escapeHtml(message)}</span>`;
        container.classList.remove('hidden');
    }

    function hideAlert(container) {
        if (!container) return;
        container.classList.add('hidden');
        container.innerHTML = '';
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Renders user details into the header and welcome cards.
     */
    function renderUserDetails(user) {
        if (!user) return;
        const name = user.fullName || user.username || 'Personnel';
        const role = user.role || 'USER';

        if (elements.userNameDisplay) elements.userNameDisplay.textContent = name;
        if (elements.userRoleDisplay) elements.userRoleDisplay.textContent = role;
        if (elements.userAvatarDisplay) {
            elements.userAvatarDisplay.textContent = name.substring(0, 2).toUpperCase();
        }

        if (elements.welcomeTitle) {
            elements.welcomeTitle.textContent = `Welcome, ${name}`;
        }
        if (elements.welcomeRole) {
            elements.welcomeRole.textContent = `Authorized Role: ${role}`;
        }
        if (elements.systemRoleVal) {
            elements.systemRoleVal.textContent = role;
        }
        if (elements.systemUserIdVal) {
            elements.systemUserIdVal.textContent = `User #${user.userId} (${user.username})`;
        }
    }

    /**
     * Switches between Login View and App Shell.
     */
    function setViewState(isAuthenticated) {
        hideAlert(elements.globalAlert);
        hideAlert(elements.loginAlert);

        if (isAuthenticated) {
            elements.loginView.classList.add('hidden');
            elements.appShell.classList.remove('hidden');
            switchModule('dashboard');
        } else {
            elements.appShell.classList.add('hidden');
            elements.loginView.classList.remove('hidden');
            if (elements.passwordInput) elements.passwordInput.value = '';
            if (elements.usernameInput) elements.usernameInput.focus();
        }
    }

    /**
     * Handles switching between Dashboard and Placeholder views.
     */
    function switchModule(moduleKey) {
        // Update active navigation item
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            if (item.getAttribute('data-module') === moduleKey) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Close mobile sidebar if open
        if (elements.appSidebar && elements.appSidebar.classList.contains('mobile-open')) {
            elements.appSidebar.classList.remove('mobile-open');
            elements.sidebarOverlay.classList.add('hidden');
        }

        const mod = MODULES[moduleKey] || MODULES['dashboard'];

        if (moduleKey === 'dashboard') {
            elements.headerTitle.textContent = 'Operational Dashboard';
            elements.viewDashboard.classList.remove('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.add('hidden');
        } else if (moduleKey === 'patients') {
            elements.headerTitle.textContent = 'Patient Registry';
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.remove('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.add('hidden');

            if (window.EmergencyPatients && typeof window.EmergencyPatients.initView === 'function') {
                window.EmergencyPatients.initView();
            }
        } else if (moduleKey === 'cases') {
            elements.headerTitle.textContent = 'Emergency Cases';
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.remove('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.add('hidden');

            if (window.EmergencyCases && typeof window.EmergencyCases.initView === 'function') {
                window.EmergencyCases.initView();
            }
        } else if (moduleKey === 'ambulances') {
            elements.headerTitle.textContent = 'Ambulance Fleet & Tracking';
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.remove('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.add('hidden');

            if (window.EmergencyAmbulances && typeof window.EmergencyAmbulances.initView === 'function') {
                window.EmergencyAmbulances.initView();
            }
        } else if (moduleKey === 'dispatches') {
            elements.headerTitle.textContent = 'Emergency Dispatches';
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.remove('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.add('hidden');

            if (window.EmergencyDispatches && typeof window.EmergencyDispatches.initView === 'function') {
                window.EmergencyDispatches.initView();
            }
        } else if (moduleKey === 'reports') {
            elements.headerTitle.textContent = 'Operational Reports & Analytics';
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.remove('hidden');
            elements.viewPlaceholder.classList.add('hidden');

            if (window.EmergencyReports && typeof window.EmergencyReports.initView === 'function') {
                window.EmergencyReports.initView();
            }
        } else {
            elements.headerTitle.textContent = mod.title;
            elements.viewDashboard.classList.add('hidden');
            if (elements.viewPatients) elements.viewPatients.classList.add('hidden');
            if (elements.viewCases) elements.viewCases.classList.add('hidden');
            if (elements.viewAmbulances) elements.viewAmbulances.classList.add('hidden');
            if (elements.viewDispatches) elements.viewDispatches.classList.add('hidden');
            if (elements.viewReports) elements.viewReports.classList.add('hidden');
            elements.viewPlaceholder.classList.remove('hidden');

            elements.placeholderIcon.textContent = mod.icon || '📁';
            elements.placeholderTitle.textContent = mod.title;
            elements.placeholderDesc.textContent = mod.desc;
            elements.placeholderStatusBadge.textContent = mod.status;
            elements.placeholderNote.textContent = mod.phaseNote || 'Module interface scheduled for Phase 5E.';
        }
    }

    /**
     * Session expired handler called by apiFetch when 401 is encountered.
     */
    function handleSessionExpired() {
        setViewState(false);
        showAlert(elements.loginAlert, 'Your session has expired or authentication is required. Please sign in.', 'error');
    }

    /**
     * Setup event listeners for forms, navigation, and mobile menu.
     */
    function setupEventListeners() {
        // Login form submission
        if (elements.loginForm) {
            elements.loginForm.addEventListener('submit', async function(e) {
                e.preventDefault();
                hideAlert(elements.loginAlert);

                const username = elements.usernameInput.value.trim();
                const password = elements.passwordInput.value;

                if (!username || !password) {
                    showAlert(elements.loginAlert, 'Please enter both username and password.', 'error');
                    return;
                }

                elements.loginBtn.disabled = true;
                elements.loginBtn.textContent = 'Verifying credentials...';

                try {
                    const user = await window.EmergencyAuth.login(username, password);
                    renderUserDetails(user);
                    setViewState(true);
                } catch (err) {
                    let errMsg = err.message || 'Login failed. Please check your credentials.';
                    if (err.status === 401) {
                        errMsg = 'Invalid username or password, or account inactive.';
                    } else if (err.status === 500) {
                        errMsg = 'A server error occurred during authentication. Please contact the administrator.';
                    }
                    showAlert(elements.loginAlert, errMsg, 'error');
                } finally {
                    elements.loginBtn.disabled = false;
                    elements.loginBtn.textContent = 'Sign In';
                }
            });
        }

        // Logout action
        if (elements.logoutBtn) {
            elements.logoutBtn.addEventListener('click', async function() {
                showLoading(true);
                let serverSuccess = false;
                try {
                    serverSuccess = await window.EmergencyAuth.logout();
                } finally {
                    showLoading(false);
                    setViewState(false);
                    if (serverSuccess) {
                        showAlert(elements.loginAlert, 'You have been signed out successfully.', 'success');
                    } else {
                        showAlert(elements.loginAlert, 'Signed out locally. Server session notification could not be verified.', 'info');
                    }
                }
            });
        }

        // Sidebar Navigation clicks
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', function(e) {
                e.preventDefault();
                const moduleKey = this.getAttribute('data-module');
                if (moduleKey) {
                    switchModule(moduleKey);
                }
            });
        });

        // Dashboard Module Card clicks
        document.querySelectorAll('.module-card').forEach(card => {
            card.addEventListener('click', function() {
                const moduleKey = this.getAttribute('data-module');
                if (moduleKey) {
                    switchModule(moduleKey);
                }
            });
        });

        // Placeholder Back to Dashboard button
        if (elements.placeholderBackBtn) {
            elements.placeholderBackBtn.addEventListener('click', function() {
                switchModule('dashboard');
            });
        }

        // Mobile menu toggle
        if (elements.menuToggleBtn) {
            elements.menuToggleBtn.addEventListener('click', function() {
                elements.appSidebar.classList.toggle('mobile-open');
                elements.sidebarOverlay.classList.toggle('hidden');
            });
        }

        if (elements.sidebarOverlay) {
            elements.sidebarOverlay.addEventListener('click', function() {
                elements.appSidebar.classList.remove('mobile-open');
                elements.sidebarOverlay.classList.add('hidden');
            });
        }
    }

    /**
     * Application Initialization
     */
    async function init() {
        cacheElements();
        setupEventListeners();

        showLoading(true);
        try {
            const user = await window.EmergencyAuth.checkSession();
            if (user) {
                renderUserDetails(user);
                setViewState(true);
            } else {
                setViewState(false);
            }
        } catch (err) {
            setViewState(false);
        } finally {
            showLoading(false);
        }
    }

    // Public API on window
    window.EmergencyApp = {
        init,
        switchModule,
        handleSessionExpired
    };

    // DOM Ready execution
    document.addEventListener('DOMContentLoaded', init);

})(window);
