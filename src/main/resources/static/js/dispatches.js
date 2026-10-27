/**
 * Connected Ambulance System - Dispatch Management Module
 * Coordinates emergency vehicle mobilizations, monitor response missions, and manage lifecycle status.
 * Integrates directly with Spring Boot REST endpoints:
 *   POST /api/dispatches (create dispatch)
 *   GET  /api/dispatches (all dispatches for dispatcher/admin)
 *   GET  /api/dispatches/my-active (driver active dispatch)
 *   GET  /api/dispatches/{id} (single dispatch details)
 *   PUT  /api/dispatches/{id}/status (status transitions)
 */
(function(window) {
    'use strict';

    // Module State
    let allDispatches = [];
    let currentRole = '';
    let currentUserId = -1;
    let isInitialized = false;
    let lastFocusedElement = null;
    let activeModal = null;
    let focusTrapHandler = null;
    let selectedDispatchForTransition = null;
    let selectedTargetStatus = null;

    // DOM Elements Cache
    let dom = {};

    function cacheDom() {
        dom = {
            view: document.getElementById('viewDispatches'),
            refreshBtn: document.getElementById('refreshDispatchesBtn'),
            openCreateBtn: document.getElementById('openCreateDispatchBtn'),
            emptyCreateBtn: document.getElementById('emptyCreateDispatchBtn'),
            roleNotice: document.getElementById('dispatchRoleNotice'),
            alertBanner: document.getElementById('dispatchAlert'),

            // Toolbar & Filters
            searchInput: document.getElementById('dispatchSearchInput'),
            clearSearchBtn: document.getElementById('clearDispatchSearchBtn'),
            statusFilter: document.getElementById('dispatchStatusFilter'),
            countBadge: document.getElementById('dispatchCountBadge'),

            // State containers
            loadingState: document.getElementById('dispatchLoadingState'),
            errorState: document.getElementById('dispatchErrorState'),
            errorMessage: document.getElementById('dispatchErrorMessage'),
            retryBtn: document.getElementById('retryDispatchesBtn'),
            restrictedState: document.getElementById('dispatchRestrictedState'),
            emptyState: document.getElementById('dispatchEmptyState'),
            emptyDesc: document.getElementById('dispatchEmptyDesc'),

            // Table
            tableWrapper: document.getElementById('dispatchTableWrapper'),
            tableBody: document.getElementById('dispatchTableBody'),

            // Create Modal
            createModal: document.getElementById('dispatchCreateModal'),
            createHeaderCloseBtn: document.getElementById('dispatchCreateHeaderCloseBtn'),
            createCancelBtn: document.getElementById('dispatchCreateCancelBtn'),
            createSubmitBtn: document.getElementById('dispatchCreateSubmitBtn'),
            createForm: document.getElementById('dispatchCreateForm'),
            createAlert: document.getElementById('dispatchCreateAlert'),
            inputCaseSelect: document.getElementById('dispatchFormCaseSelect'),
            inputCaseManualId: document.getElementById('dispatchFormCaseManualId'),
            inputAmbulanceSelect: document.getElementById('dispatchFormAmbulanceSelect'),
            inputAmbulanceManualId: document.getElementById('dispatchFormAmbulanceManualId'),
            inputEta: document.getElementById('dispatchFormEta'),

            // Details Modal
            detailsModal: document.getElementById('dispatchDetailsModal'),
            detailsHeaderCloseBtn: document.getElementById('detailDispatchHeaderCloseBtn'),
            detailsCloseBtn: document.getElementById('detailDispatchCloseBtn'),
            detailsUpdateStatusBtn: document.getElementById('detailDispatchUpdateStatusBtn'),
            detailId: document.getElementById('detailDispatchId'),
            detailCaseId: document.getElementById('detailDispatchCaseId'),
            detailAmbulance: document.getElementById('detailDispatchAmbulance'),
            detailStatus: document.getElementById('detailDispatchStatus'),
            detailEta: document.getElementById('detailDispatchEta'),
            detailTime: document.getElementById('detailDispatchTime'),
            detailArrivalTime: document.getElementById('detailDispatchArrivalTime'),

            // Status Modal
            statusModal: document.getElementById('dispatchStatusModal'),
            statusHeaderCloseBtn: document.getElementById('dispatchStatusHeaderCloseBtn'),
            statusCancelBtn: document.getElementById('dispatchStatusCancelBtn'),
            statusSubmitBtn: document.getElementById('dispatchStatusSubmitBtn'),
            statusAlert: document.getElementById('dispatchStatusAlert'),
            statusModalId: document.getElementById('dispatchStatusModalId'),
            statusModalCurrent: document.getElementById('dispatchStatusModalCurrent'),
            statusOptionsContainer: document.getElementById('dispatchStatusOptionsContainer'),
            statusTerminalNotice: document.getElementById('dispatchStatusTerminalNotice')
        };
    }

    /**
     * Traps keyboard focus within an active modal dialog.
     */
    function trapFocus(modalElement) {
        if (!modalElement) return;
        releaseFocusTrap();

        activeModal = modalElement;
        const focusableSelectors = 'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

        focusTrapHandler = function(e) {
            if (e.key !== 'Tab') return;
            const focusables = Array.from(modalElement.querySelectorAll(focusableSelectors))
                .filter(el => el.offsetParent !== null);
            if (focusables.length === 0) {
                e.preventDefault();
                return;
            }
            const firstElement = focusables[0];
            const lastElement = focusables[focusables.length - 1];

            if (e.shiftKey) {
                if (document.activeElement === firstElement) {
                    e.preventDefault();
                    lastElement.focus();
                }
            } else {
                if (document.activeElement === lastElement) {
                    e.preventDefault();
                    firstElement.focus();
                }
            }
        };

        modalElement.addEventListener('keydown', focusTrapHandler);
        const focusables = Array.from(modalElement.querySelectorAll(focusableSelectors))
            .filter(el => el.offsetParent !== null);
        if (focusables.length > 0) {
            focusables[0].focus();
        }
    }

    /**
     * Releases active focus trap and restores focus to previous control.
     */
    function releaseFocusTrap() {
        if (activeModal && focusTrapHandler) {
            activeModal.removeEventListener('keydown', focusTrapHandler);
        }
        focusTrapHandler = null;
        activeModal = null;

        if (lastFocusedElement) {
            try {
                if (typeof lastFocusedElement.focus === 'function') {
                    if (document.body.contains(lastFocusedElement)) {
                        lastFocusedElement.focus();
                    }
                }
            } catch (_) {}
        }
        lastFocusedElement = null;
    }

    /**
     * Evaluates caller authorization and role capabilities.
     */
    function getRolePermissions() {
        const user = window.EmergencyAuth ? window.EmergencyAuth.getCurrentUser() : null;
        currentRole = user ? user.role : '';
        currentUserId = user ? user.userId : -1;

        const isAdminOrDispatcher = ['ADMIN', 'DISPATCHER'].includes(currentRole);
        const isDriver = currentRole === 'AMBULANCE_DRIVER';

        return {
            isAdminOrDispatcher,
            isDriver,
            canView: isAdminOrDispatcher || isDriver,
            canCreate: isAdminOrDispatcher,
            canUpdateStatus: isAdminOrDispatcher || isDriver
        };
    }

    /**
     * Validates dispatch payload according to backend constraints.
     */
    function validateDispatchPayload(payload) {
        if (!payload) return 'Dispatch payload cannot be empty.';

        if (!Number.isInteger(payload.caseId) || payload.caseId <= 0) {
            return 'A valid Emergency Case ID is required (must be a positive integer).';
        }

        if (!Number.isInteger(payload.ambulanceId) || payload.ambulanceId <= 0) {
            return 'A valid Ambulance ID is required (must be a positive integer).';
        }

        if (payload.estimatedArrivalMinutes === undefined || payload.estimatedArrivalMinutes === null) {
            return 'Estimated arrival time (ETA) is required.';
        }

        if (!Number.isInteger(payload.estimatedArrivalMinutes) ||
            payload.estimatedArrivalMinutes < 0 ||
            payload.estimatedArrivalMinutes > 300) {
            return 'Estimated arrival time must be between 0 and 300 minutes.';
        }

        return null;
    }

    /**
     * Helper to render semantic status badges.
     */
    function renderStatusBadge(status) {
        const badge = document.createElement('span');
        const s = (status || 'UNKNOWN').toUpperCase();
        badge.className = 'badge';

        switch (s) {
            case 'DISPATCHED':
                badge.classList.add('badge-dispatch-dispatched');
                badge.textContent = 'DISPATCHED';
                break;
            case 'EN_ROUTE':
                badge.classList.add('badge-dispatch-enroute');
                badge.textContent = 'EN ROUTE';
                break;
            case 'ARRIVED':
                badge.classList.add('badge-dispatch-arrived');
                badge.textContent = 'ARRIVED';
                break;
            case 'COMPLETED':
                badge.classList.add('badge-dispatch-completed');
                badge.textContent = 'COMPLETED';
                break;
            case 'CANCELLED':
                badge.classList.add('badge-dispatch-cancelled');
                badge.textContent = 'CANCELLED';
                break;
            default:
                badge.classList.add('badge-pending');
                badge.textContent = s;
                break;
        }

        return badge;
    }

    /**
     * Helper to display module-level banner messages.
     */
    function showModuleAlert(message, type = 'info') {
        if (!dom.alertBanner) return;
        dom.alertBanner.className = `alert alert-${type}`;
        dom.alertBanner.innerHTML = `<span>${escapeHtml(message)}</span>`;
        dom.alertBanner.classList.remove('hidden');
    }

    function hideModuleAlert() {
        if (!dom.alertBanner) return;
        dom.alertBanner.classList.add('hidden');
        dom.alertBanner.innerHTML = '';
    }

    /**
     * Retrieves valid target transitions from current dispatch status based on DispatchService rules.
     */
    function getAllowedTransitions(currentStatus) {
        if (!currentStatus) return [];
        const curr = currentStatus.toUpperCase();

        switch (curr) {
            case 'DISPATCHED':
                return [
                    {
                        status: 'EN_ROUTE',
                        label: 'Mobilize En Route (EN_ROUTE)',
                        desc: 'Ambulance is moving towards the scene. Case updates to IN_TRANSIT.'
                    },
                    {
                        status: 'CANCELLED',
                        label: 'Pre-Departure Cancellation (CANCELLED)',
                        desc: 'Cancel dispatch before vehicle departs. Case resets to REGISTERED for redispatch, ambulance returns to AVAILABLE.'
                    }
                ];
            case 'EN_ROUTE':
                return [
                    {
                        status: 'ARRIVED',
                        label: 'Mark Arrived at Scene (ARRIVED)',
                        desc: 'Ambulance arrived at incident location. Case updates to ARRIVED, records arrival timestamp.'
                    },
                    {
                        status: 'CANCELLED',
                        label: 'In-Transit Cancellation (CANCELLED)',
                        desc: 'Emergency mission aborted while in transit. Case is marked CANCELLED, ambulance returns to AVAILABLE.'
                    }
                ];
            case 'ARRIVED':
                return [
                    {
                        status: 'COMPLETED',
                        label: 'Complete Mission (COMPLETED)',
                        desc: 'Patient care or transport concluded. Case marked COMPLETED, ambulance returns to AVAILABLE.'
                    },
                    {
                        status: 'CANCELLED',
                        label: 'At-Scene Cancellation (CANCELLED)',
                        desc: 'Mission aborted after arrival. Case marked CANCELLED, ambulance returns to AVAILABLE.'
                    }
                ];
            case 'COMPLETED':
            case 'CANCELLED':
            default:
                return []; // Terminal states
        }
    }

    /**
     * Loads dispatches from backend using role-appropriate endpoints.
     */
    async function loadDispatches() {
        hideModuleAlert();
        const perms = getRolePermissions();

        if (!perms.canView) {
            dom.loadingState.classList.add('hidden');
            dom.errorState.classList.add('hidden');
            dom.emptyState.classList.add('hidden');
            dom.tableWrapper.classList.add('hidden');
            dom.restrictedState.classList.remove('hidden');
            if (dom.openCreateBtn) dom.openCreateBtn.classList.add('hidden');
            return;
        }

        dom.restrictedState.classList.add('hidden');
        dom.errorState.classList.add('hidden');
        dom.emptyState.classList.add('hidden');
        dom.tableWrapper.classList.add('hidden');
        dom.loadingState.classList.remove('hidden');

        // Configure role notifications and buttons
        if (perms.isDriver) {
            if (dom.roleNotice) {
                dom.roleNotice.innerHTML = '<strong>Driver Mission View:</strong> Displaying the active dispatch assigned to your vehicle.';
                dom.roleNotice.classList.remove('hidden');
            }
            if (dom.openCreateBtn) dom.openCreateBtn.classList.add('hidden');
            if (dom.emptyCreateBtn) dom.emptyCreateBtn.classList.add('hidden');
        } else {
            if (dom.roleNotice) dom.roleNotice.classList.add('hidden');
            if (dom.openCreateBtn) dom.openCreateBtn.classList.remove('hidden');
        }

        try {
            if (perms.isDriver) {
                // Drivers fetch from /api/dispatches/my-active
                try {
                    const res = await window.EmergencyApi.apiFetch('/api/dispatches/my-active', { method: 'GET' });
                    if (res && res.data) {
                        allDispatches = [res.data];
                    } else {
                        allDispatches = [];
                    }
                } catch (driverErr) {
                    // 404 indicates no active dispatch assigned, which is normal for drivers off mission
                    if (driverErr && driverErr.status === 404) {
                        allDispatches = [];
                    } else if (driverErr && driverErr.status === 403) {
                        dom.loadingState.classList.add('hidden');
                        dom.restrictedState.classList.remove('hidden');
                        return;
                    } else {
                        throw driverErr;
                    }
                }
            } else {
                // Admin and Dispatchers fetch all dispatches from /api/dispatches
                const res = await window.EmergencyApi.apiFetch('/api/dispatches', { method: 'GET' });
                allDispatches = (res && Array.isArray(res.data)) ? res.data : [];
            }

            dom.loadingState.classList.add('hidden');
            applyFilters();

        } catch (err) {
            dom.loadingState.classList.add('hidden');
            if (err && err.status === 403) {
                dom.restrictedState.classList.remove('hidden');
            } else {
                dom.errorState.classList.remove('hidden');
                if (dom.errorMessage) {
                    dom.errorMessage.textContent = err.message || 'Unable to connect to /api/dispatches';
                }
            }
        }
    }

    /**
     * Filters dispatches locally by search text and status.
     */
    function applyFilters() {
        const query = (dom.searchInput ? dom.searchInput.value : '').trim().toLowerCase();
        const selectedStatus = dom.statusFilter ? dom.statusFilter.value : 'ALL';

        if (dom.clearSearchBtn) {
            if (query.length > 0) {
                dom.clearSearchBtn.classList.remove('hidden');
            } else {
                dom.clearSearchBtn.classList.add('hidden');
            }
        }

        const filtered = allDispatches.filter(dispatch => {
            // Status filter
            if (selectedStatus !== 'ALL') {
                const s = (dispatch.dispatchStatus || '').toUpperCase();
                if (s !== selectedStatus) return false;
            }

            // Search query filter
            if (query.length > 0) {
                const idMatch = String(dispatch.dispatchId || '').toLowerCase().includes(query);
                const caseMatch = String(dispatch.caseId || '').toLowerCase().includes(query);
                const ambMatch = String(dispatch.ambulanceId || '').toLowerCase().includes(query);
                const vehMatch = String(dispatch.vehicleNumber || '').toLowerCase().includes(query);
                const statusMatch = String(dispatch.dispatchStatus || '').toLowerCase().includes(query);
                return idMatch || caseMatch || ambMatch || vehMatch || statusMatch;
            }

            return true;
        });

        // Update count badge
        if (dom.countBadge) {
            const count = filtered.length;
            dom.countBadge.textContent = count === 1 ? '1 dispatch' : `${count} dispatches`;
        }

        renderTable(filtered);
    }

    /**
     * Renders filtered dispatch records into the table view.
     */
    function renderTable(dispatches) {
        if (!dom.tableBody) return;
        dom.tableBody.innerHTML = '';
        const perms = getRolePermissions();

        if (dispatches.length === 0) {
            dom.tableWrapper.classList.add('hidden');
            dom.emptyState.classList.remove('hidden');

            if (dom.emptyDesc) {
                const query = (dom.searchInput ? dom.searchInput.value : '').trim();
                const selectedStatus = dom.statusFilter ? dom.statusFilter.value : 'ALL';
                if (query.length > 0 || selectedStatus !== 'ALL') {
                    dom.emptyDesc.textContent = 'No dispatch records match the selected filter criteria.';
                } else if (perms.isDriver) {
                    dom.emptyDesc.textContent = 'No active dispatch mission is currently assigned to your vehicle.';
                } else {
                    dom.emptyDesc.textContent = 'No dispatch records are registered in the system.';
                }
            }

            if (dom.emptyCreateBtn) {
                if (perms.canCreate) {
                    dom.emptyCreateBtn.classList.remove('hidden');
                } else {
                    dom.emptyCreateBtn.classList.add('hidden');
                }
            }
            return;
        }

        dom.emptyState.classList.add('hidden');
        dom.tableWrapper.classList.remove('hidden');

        dispatches.forEach(dispatch => {
            const tr = document.createElement('tr');

            // 1. Dispatch ID
            const tdId = document.createElement('td');
            tdId.style.fontWeight = '700';
            tdId.textContent = `#${dispatch.dispatchId}`;
            tr.appendChild(tdId);

            // 2. Case ID
            const tdCase = document.createElement('td');
            const caseBadge = document.createElement('span');
            caseBadge.className = 'badge badge-role';
            caseBadge.textContent = `Case #${dispatch.caseId}`;
            tdCase.appendChild(caseBadge);
            tr.appendChild(tdCase);

            // 3. Assigned Ambulance
            const tdAmb = document.createElement('td');
            const ambName = document.createElement('span');
            ambName.style.fontWeight = '600';
            ambName.textContent = dispatch.vehicleNumber ? `${dispatch.vehicleNumber} ` : `Ambulance #${dispatch.ambulanceId} `;
            tdAmb.appendChild(ambName);

            const ambIdBadge = document.createElement('span');
            ambIdBadge.className = 'badge';
            ambIdBadge.style.fontSize = '11px';
            ambIdBadge.style.backgroundColor = 'var(--bg-surface-secondary)';
            ambIdBadge.textContent = `ID #${dispatch.ambulanceId}`;
            tdAmb.appendChild(ambIdBadge);
            tr.appendChild(tdAmb);

            // 4. Status
            const tdStatus = document.createElement('td');
            tdStatus.appendChild(renderStatusBadge(dispatch.dispatchStatus));
            tr.appendChild(tdStatus);

            // 5. ETA
            const tdEta = document.createElement('td');
            tdEta.textContent = `${dispatch.estimatedArrivalMinutes} min`;
            tr.appendChild(tdEta);

            // 6. Timestamps
            const tdTime = document.createElement('td');
            tdTime.style.fontSize = '12px';
            tdTime.style.color = 'var(--text-muted)';
            if (dispatch.actualArrivalTime) {
                tdTime.textContent = `Arrived: ${dispatch.actualArrivalTime}`;
            } else if (dispatch.dispatchTime) {
                tdTime.textContent = `Dispatched: ${dispatch.dispatchTime}`;
            } else {
                tdTime.textContent = 'Mission logged';
            }
            tr.appendChild(tdTime);

            // 7. Actions
            const tdActions = document.createElement('td');
            tdActions.style.textAlign = 'right';
            tdActions.className = 'table-actions';

            // Details Button
            const detailsBtn = document.createElement('button');
            detailsBtn.type = 'button';
            detailsBtn.className = 'btn btn-secondary btn-xs';
            detailsBtn.textContent = 'Details';
            detailsBtn.title = 'View comprehensive dispatch details';
            detailsBtn.addEventListener('click', (e) => openDetailsModal(dispatch, e.currentTarget));
            tdActions.appendChild(detailsBtn);

            // Update Status Button (available if permitted and not in terminal state)
            const isTerminal = ['COMPLETED', 'CANCELLED'].includes((dispatch.dispatchStatus || '').toUpperCase());
            if (perms.canUpdateStatus && !isTerminal) {
                const statusBtn = document.createElement('button');
                statusBtn.type = 'button';
                statusBtn.className = 'btn btn-primary btn-xs';
                statusBtn.style.marginLeft = '6px';
                statusBtn.textContent = 'Update Status';
                statusBtn.title = 'Advance mission lifecycle';
                statusBtn.addEventListener('click', (e) => openStatusModal(dispatch, e.currentTarget));
                tdActions.appendChild(statusBtn);
            }

            tr.appendChild(tdActions);
            dom.tableBody.appendChild(tr);
        });
    }

    /**
     * Opens the Details Modal for a dispatch.
     */
    function openDetailsModal(dispatch, triggerElement) {
        lastFocusedElement = triggerElement;
        const perms = getRolePermissions();

        if (dom.detailId) dom.detailId.textContent = `#${dispatch.dispatchId}`;
        if (dom.detailCaseId) dom.detailCaseId.textContent = `Emergency Case #${dispatch.caseId}`;
        if (dom.detailAmbulance) {
            dom.detailAmbulance.textContent = dispatch.vehicleNumber
                ? `${dispatch.vehicleNumber} (Ambulance ID #${dispatch.ambulanceId})`
                : `Ambulance ID #${dispatch.ambulanceId}`;
        }
        if (dom.detailStatus) {
            dom.detailStatus.innerHTML = '';
            dom.detailStatus.appendChild(renderStatusBadge(dispatch.dispatchStatus));
        }
        if (dom.detailEta) dom.detailEta.textContent = `${dispatch.estimatedArrivalMinutes} minutes`;
        if (dom.detailTime) dom.detailTime.textContent = dispatch.dispatchTime || 'Recorded upon creation';
        if (dom.detailArrivalTime) dom.detailArrivalTime.textContent = dispatch.actualArrivalTime || 'Pending arrival';

        // Update Status button visibility in details modal
        const isTerminal = ['COMPLETED', 'CANCELLED'].includes((dispatch.dispatchStatus || '').toUpperCase());
        if (dom.detailsUpdateStatusBtn) {
            if (perms.canUpdateStatus && !isTerminal) {
                dom.detailsUpdateStatusBtn.classList.remove('hidden');
                dom.detailsUpdateStatusBtn.onclick = () => {
                    closeDetailsModal();
                    openStatusModal(dispatch, triggerElement);
                };
            } else {
                dom.detailsUpdateStatusBtn.classList.add('hidden');
                dom.detailsUpdateStatusBtn.onclick = null;
            }
        }

        if (dom.detailsModal) {
            dom.detailsModal.classList.remove('hidden');
            trapFocus(dom.detailsModal);
        }
    }

    function closeDetailsModal() {
        if (dom.detailsModal) dom.detailsModal.classList.add('hidden');
        releaseFocusTrap();
    }

    /**
     * Loads registered emergency cases and available ambulances for dispatch creation.
     */
    async function loadEligibleCasesAndAmbulances() {
        if (!dom.inputCaseSelect || !dom.inputAmbulanceSelect) return;

        dom.inputCaseSelect.innerHTML = '<option value="">-- Loading Registered Cases... --</option>';
        dom.inputAmbulanceSelect.innerHTML = '<option value="">-- Loading Available Ambulances... --</option>';

        try {
            // 1. Fetch Cases
            const casesRes = await window.EmergencyApi.apiFetch('/api/cases', { method: 'GET' });
            const cases = (casesRes && Array.isArray(casesRes.data)) ? casesRes.data : [];
            const registeredCases = cases.filter(c => (c.caseStatus || '').toUpperCase() === 'REGISTERED');

            dom.inputCaseSelect.innerHTML = '<option value="">-- Select Registered Emergency Case --</option>';
            if (registeredCases.length === 0) {
                const opt = document.createElement('option');
                opt.value = '';
                opt.textContent = '-- No cases in REGISTERED status found --';
                opt.disabled = true;
                dom.inputCaseSelect.appendChild(opt);
            } else {
                registeredCases.forEach(c => {
                    const opt = document.createElement('option');
                    opt.value = c.caseId;
                    opt.textContent = `Case #${c.caseId} - ${c.emergencyType} (${c.incidentLocation})`;
                    dom.inputCaseSelect.appendChild(opt);
                });
            }

            // 2. Fetch Ambulances
            const ambRes = await window.EmergencyApi.apiFetch('/api/ambulances', { method: 'GET' });
            const ambulances = (ambRes && Array.isArray(ambRes.data)) ? ambRes.data : [];
            const availableAmbulances = ambulances.filter(a => (a.availability || '').toUpperCase() === 'AVAILABLE');

            dom.inputAmbulanceSelect.innerHTML = '<option value="">-- Select Available Ambulance --</option>';
            if (availableAmbulances.length === 0) {
                const opt = document.createElement('option');
                opt.value = '';
                opt.textContent = '-- No ambulances in AVAILABLE status found --';
                opt.disabled = true;
                dom.inputAmbulanceSelect.appendChild(opt);
            } else {
                availableAmbulances.forEach(a => {
                    const opt = document.createElement('option');
                    opt.value = a.ambulanceId;
                    const veh = a.vehicleNumber || `Unit #${a.ambulanceId}`;
                    const eq = a.equipment ? ` - ${a.equipment}` : '';
                    opt.textContent = `${veh} (ID #${a.ambulanceId})${eq}`;
                    dom.inputAmbulanceSelect.appendChild(opt);
                });
            }

        } catch (err) {
            showCreateAlert(err.message || 'Failed to load eligible cases and available ambulances.', 'error');
        }
    }

    /**
     * Opens the Create Dispatch modal.
     */
    function openCreateModal(triggerElement) {
        const perms = getRolePermissions();
        if (!perms.canCreate) return;

        lastFocusedElement = triggerElement;
        hideCreateAlert();

        if (dom.inputCaseManualId) dom.inputCaseManualId.value = '';
        if (dom.inputAmbulanceManualId) dom.inputAmbulanceManualId.value = '';
        if (dom.inputEta) dom.inputEta.value = '15';

        if (dom.createModal) {
            dom.createModal.classList.remove('hidden');
            trapFocus(dom.createModal);
        }

        loadEligibleCasesAndAmbulances();
    }

    function closeCreateModal() {
        if (dom.createModal) dom.createModal.classList.add('hidden');
        hideCreateAlert();
        releaseFocusTrap();
    }

    function showCreateAlert(message, type = 'error') {
        if (!dom.createAlert) return;
        dom.createAlert.className = `alert alert-${type}`;
        dom.createAlert.innerHTML = `<span>${escapeHtml(message)}</span>`;
        dom.createAlert.classList.remove('hidden');
    }

    function hideCreateAlert() {
        if (!dom.createAlert) return;
        dom.createAlert.classList.add('hidden');
        dom.createAlert.innerHTML = '';
    }

    /**
     * Handles dispatch creation submission.
     */
    async function handleCreateSubmit(e) {
        e.preventDefault();
        hideCreateAlert();

        // Resolve Case ID
        let caseId = null;
        if (dom.inputCaseManualId && dom.inputCaseManualId.value.trim() !== '') {
            caseId = parseInt(dom.inputCaseManualId.value.trim(), 10);
        } else if (dom.inputCaseSelect && dom.inputCaseSelect.value) {
            caseId = parseInt(dom.inputCaseSelect.value, 10);
        }

        // Resolve Ambulance ID
        let ambulanceId = null;
        if (dom.inputAmbulanceManualId && dom.inputAmbulanceManualId.value.trim() !== '') {
            ambulanceId = parseInt(dom.inputAmbulanceManualId.value.trim(), 10);
        } else if (dom.inputAmbulanceSelect && dom.inputAmbulanceSelect.value) {
            ambulanceId = parseInt(dom.inputAmbulanceSelect.value, 10);
        }

        // Resolve ETA
        const eta = dom.inputEta ? parseInt(dom.inputEta.value.trim(), 10) : NaN;

        const payload = {
            caseId: caseId,
            ambulanceId: ambulanceId,
            estimatedArrivalMinutes: eta
        };

        const validationErr = validateDispatchPayload(payload);
        if (validationErr) {
            showCreateAlert(validationErr, 'error');
            return;
        }

        dom.createSubmitBtn.disabled = true;
        const origText = dom.createSubmitBtn.textContent;
        dom.createSubmitBtn.textContent = 'Mobilizing ambulance...';

        try {
            const res = await window.EmergencyApi.apiFetch('/api/dispatches', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            const newId = (res && res.data && res.data.dispatchId) ? res.data.dispatchId : '';
            closeCreateModal();
            showModuleAlert(`Ambulance dispatched successfully! Active dispatch record #${newId} created.`, 'success');
            await loadDispatches();

        } catch (err) {
            if (err.status === 409) {
                // Concurrency or availability conflict
                showCreateAlert(`Conflict: ${err.message || 'Selected case or ambulance is no longer available.'} Refreshing options...`, 'warning');
                await loadEligibleCasesAndAmbulances();
            } else {
                showCreateAlert(err.message || 'Failed to dispatch ambulance.', 'error');
            }
        } finally {
            dom.createSubmitBtn.disabled = false;
            dom.createSubmitBtn.textContent = origText;
        }
    }

    /**
     * Opens Status Transition Modal for a dispatch.
     */
    function openStatusModal(dispatch, triggerElement) {
        lastFocusedElement = triggerElement;
        selectedDispatchForTransition = dispatch;
        selectedTargetStatus = null;
        hideStatusAlert();

        if (dom.statusModalId) dom.statusModalId.textContent = `#${dispatch.dispatchId}`;
        if (dom.statusModalCurrent) {
            dom.statusModalCurrent.innerHTML = '';
            dom.statusModalCurrent.appendChild(renderStatusBadge(dispatch.dispatchStatus));
        }

        const allowed = getAllowedTransitions(dispatch.dispatchStatus);
        renderTransitionOptions(allowed);

        if (dom.statusModal) {
            dom.statusModal.classList.remove('hidden');
            trapFocus(dom.statusModal);
        }
    }

    function closeStatusModal() {
        if (dom.statusModal) dom.statusModal.classList.add('hidden');
        hideStatusAlert();
        selectedDispatchForTransition = null;
        selectedTargetStatus = null;
        releaseFocusTrap();
    }

    function showStatusAlert(message, type = 'error') {
        if (!dom.statusAlert) return;
        dom.statusAlert.className = `alert alert-${type}`;
        dom.statusAlert.innerHTML = `<span>${escapeHtml(message)}</span>`;
        dom.statusAlert.classList.remove('hidden');
    }

    function hideStatusAlert() {
        if (!dom.statusAlert) return;
        dom.statusAlert.classList.add('hidden');
        dom.statusAlert.innerHTML = '';
    }

    /**
     * Renders valid transitions as selectable option cards.
     */
    function renderTransitionOptions(options) {
        if (!dom.statusOptionsContainer) return;
        dom.statusOptionsContainer.innerHTML = '';

        if (options.length === 0) {
            if (dom.statusTerminalNotice) dom.statusTerminalNotice.classList.remove('hidden');
            if (dom.statusSubmitBtn) dom.statusSubmitBtn.disabled = true;
            return;
        }

        if (dom.statusTerminalNotice) dom.statusTerminalNotice.classList.add('hidden');
        if (dom.statusSubmitBtn) dom.statusSubmitBtn.disabled = true;

        options.forEach(opt => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'transition-option-btn';
            btn.setAttribute('data-target-status', opt.status);

            const contentDiv = document.createElement('div');
            const titleDiv = document.createElement('div');
            titleDiv.className = 'transition-btn-title';
            titleDiv.textContent = opt.label;

            const descDiv = document.createElement('div');
            descDiv.className = 'transition-btn-desc';
            descDiv.textContent = opt.desc;

            contentDiv.appendChild(titleDiv);
            contentDiv.appendChild(descDiv);
            btn.appendChild(contentDiv);

            const badgeSpan = renderStatusBadge(opt.status);
            btn.appendChild(badgeSpan);

            btn.addEventListener('click', () => {
                const allOptionBtns = dom.statusOptionsContainer.querySelectorAll('.transition-option-btn');
                allOptionBtns.forEach(b => b.classList.remove('selected'));
                btn.classList.add('selected');
                selectedTargetStatus = opt.status;
                if (dom.statusSubmitBtn) dom.statusSubmitBtn.disabled = false;
            });

            dom.statusOptionsContainer.appendChild(btn);
        });
    }

    /**
     * Handles dispatch status transition submission.
     */
    async function handleStatusSubmit() {
        if (!selectedDispatchForTransition || !selectedTargetStatus) {
            showStatusAlert('Please select a target status transition.', 'warning');
            return;
        }

        const dispatchId = selectedDispatchForTransition.dispatchId;
        hideStatusAlert();

        dom.statusSubmitBtn.disabled = true;
        const origText = dom.statusSubmitBtn.textContent;
        dom.statusSubmitBtn.textContent = 'Updating status...';

        try {
            const res = await window.EmergencyApi.apiFetch(`/api/dispatches/${dispatchId}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status: selectedTargetStatus })
            });

            closeStatusModal();
            showModuleAlert(res.message || `Dispatch #${dispatchId} updated to ${selectedTargetStatus} successfully.`, 'success');
            await loadDispatches();

        } catch (err) {
            if (err.status === 409) {
                showStatusAlert(`Conflict: ${err.message || 'Status was modified by another operator.'} Reloading dispatches...`, 'warning');
                await loadDispatches();
            } else {
                showStatusAlert(err.message || 'Failed to update dispatch status.', 'error');
            }
        } finally {
            dom.statusSubmitBtn.disabled = false;
            dom.statusSubmitBtn.textContent = origText;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Attaches DOM event listeners once.
     */
    function setupEvents() {
        if (isInitialized) return;
        isInitialized = true;

        if (dom.refreshBtn) dom.refreshBtn.addEventListener('click', loadDispatches);
        if (dom.retryBtn) dom.retryBtn.addEventListener('click', loadDispatches);

        if (dom.openCreateBtn) dom.openCreateBtn.addEventListener('click', (e) => openCreateModal(e.currentTarget));
        if (dom.emptyCreateBtn) dom.emptyCreateBtn.addEventListener('click', (e) => openCreateModal(e.currentTarget));

        // Filters
        if (dom.searchInput) dom.searchInput.addEventListener('input', applyFilters);
        if (dom.statusFilter) dom.statusFilter.addEventListener('change', applyFilters);

        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.addEventListener('click', () => {
                dom.searchInput.value = '';
                applyFilters();
                dom.searchInput.focus();
            });
        }

        // Create Modal Events
        if (dom.createHeaderCloseBtn) dom.createHeaderCloseBtn.addEventListener('click', closeCreateModal);
        if (dom.createCancelBtn) dom.createCancelBtn.addEventListener('click', closeCreateModal);
        if (dom.createModal) {
            dom.createModal.addEventListener('click', (e) => {
                if (e.target === dom.createModal) closeCreateModal();
            });
        }
        if (dom.createForm) dom.createForm.addEventListener('submit', handleCreateSubmit);

        // When select inputs change, clear manual input
        if (dom.inputCaseSelect) {
            dom.inputCaseSelect.addEventListener('change', () => {
                if (dom.inputCaseSelect.value && dom.inputCaseManualId) {
                    dom.inputCaseManualId.value = '';
                }
            });
        }
        if (dom.inputCaseManualId) {
            dom.inputCaseManualId.addEventListener('input', () => {
                if (dom.inputCaseManualId.value && dom.inputCaseSelect) {
                    dom.inputCaseSelect.value = '';
                }
            });
        }

        if (dom.inputAmbulanceSelect) {
            dom.inputAmbulanceSelect.addEventListener('change', () => {
                if (dom.inputAmbulanceSelect.value && dom.inputAmbulanceManualId) {
                    dom.inputAmbulanceManualId.value = '';
                }
            });
        }
        if (dom.inputAmbulanceManualId) {
            dom.inputAmbulanceManualId.addEventListener('input', () => {
                if (dom.inputAmbulanceManualId.value && dom.inputAmbulanceSelect) {
                    dom.inputAmbulanceSelect.value = '';
                }
            });
        }

        // Details Modal Events
        if (dom.detailsHeaderCloseBtn) dom.detailsHeaderCloseBtn.addEventListener('click', closeDetailsModal);
        if (dom.detailsCloseBtn) dom.detailsCloseBtn.addEventListener('click', closeDetailsModal);
        if (dom.detailsModal) {
            dom.detailsModal.addEventListener('click', (e) => {
                if (e.target === dom.detailsModal) closeDetailsModal();
            });
        }

        // Status Modal Events
        if (dom.statusHeaderCloseBtn) dom.statusHeaderCloseBtn.addEventListener('click', closeStatusModal);
        if (dom.statusCancelBtn) dom.statusCancelBtn.addEventListener('click', closeStatusModal);
        if (dom.statusModal) {
            dom.statusModal.addEventListener('click', (e) => {
                if (e.target === dom.statusModal) closeStatusModal();
            });
        }
        if (dom.statusSubmitBtn) dom.statusSubmitBtn.addEventListener('click', handleStatusSubmit);

        // Global Escape Key Listener
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (dom.statusModal && !dom.statusModal.classList.contains('hidden')) {
                    closeStatusModal();
                } else if (dom.createModal && !dom.createModal.classList.contains('hidden')) {
                    closeCreateModal();
                } else if (dom.detailsModal && !dom.detailsModal.classList.contains('hidden')) {
                    closeDetailsModal();
                }
            }
        });
    }

    /**
     * Initializes the view when navigating to Dispatches module.
     */
    function initView() {
        cacheDom();
        setupEvents();
        loadDispatches();
    }

    // Public API
    window.EmergencyDispatches = {
        initView,
        loadDispatches,
        applyFilters,
        validateDispatchPayload,
        openCreateModal,
        openDetailsModal,
        openStatusModal
    };

})(window);
