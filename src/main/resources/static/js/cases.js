/**
 * Connected Ambulance System - Emergency Cases Module
 * Handles case triage list, search, details inspection, registration, and atomic status transitions.
 */
(function(window) {
    'use strict';

    let allCases = [];
    let currentFiltered = [];
    let currentRole = null;
    let selectedCaseForStatus = null;
    let selectedTargetStatus = null;
    let isInitialized = false;

    // Accessibility focus management
    let modalFocusCleanup = null;
    let lastFocusedElement = null;

    // DOM cache
    let dom = {};

    function cacheDom() {
        dom = {
            view: document.getElementById('viewCases'),
            roleNotice: document.getElementById('caseRoleNotice'),
            caseAlert: document.getElementById('caseAlert'),
            searchInput: document.getElementById('caseSearchInput'),
            clearSearchBtn: document.getElementById('clearCaseSearchBtn'),
            statusFilter: document.getElementById('caseStatusFilter'),
            priorityFilter: document.getElementById('casePriorityFilter'),
            countBadge: document.getElementById('caseCountBadge'),
            refreshBtn: document.getElementById('refreshCasesBtn'),
            openAddBtn: document.getElementById('openAddCaseBtn'),
            emptyAddBtn: document.getElementById('emptyAddCaseBtn'),
            retryBtn: document.getElementById('retryCasesBtn'),

            // State containers
            loadingState: document.getElementById('caseLoadingState'),
            errorState: document.getElementById('caseErrorState'),
            errorMessage: document.getElementById('caseErrorMessage'),
            restrictedState: document.getElementById('caseRestrictedState'),
            emptyState: document.getElementById('caseEmptyState'),
            emptyDesc: document.getElementById('caseEmptyDesc'),
            tableWrapper: document.getElementById('caseTableWrapper'),
            tableBody: document.getElementById('caseTableBody'),

            // Details Modal
            detailsModal: document.getElementById('caseDetailsModal'),
            detailsTitle: document.getElementById('caseDetailsTitle'),
            detailsId: document.getElementById('detailCaseId'),
            detailsPatientId: document.getElementById('detailCasePatientId'),
            detailsLocation: document.getElementById('detailCaseLocation'),
            detailsType: document.getElementById('detailCaseType'),
            detailsPriority: document.getElementById('detailCasePriority'),
            detailsStatus: document.getElementById('detailCaseStatus'),
            detailsSymptoms: document.getElementById('detailCaseSymptoms'),
            detailsUpdateStatusBtn: document.getElementById('detailUpdateStatusBtn'),
            detailsCloseBtn: document.getElementById('detailCaseCloseBtn'),
            detailsHeaderCloseBtn: document.getElementById('detailCaseHeaderCloseBtn'),

            // Register Modal
            registerModal: document.getElementById('caseRegisterModal'),
            registerForm: document.getElementById('caseRegisterForm'),
            registerAlert: document.getElementById('caseRegisterAlert'),
            inputPatientSelect: document.getElementById('caseFormPatientSelect'),
            inputPatientManualId: document.getElementById('caseFormPatientManualId'),
            inputLocation: document.getElementById('caseFormLocation'),
            inputType: document.getElementById('caseFormType'),
            inputSymptoms: document.getElementById('caseFormSymptoms'),
            registerSubmitBtn: document.getElementById('caseRegisterSubmitBtn'),
            registerCancelBtn: document.getElementById('caseRegisterCancelBtn'),
            registerHeaderCloseBtn: document.getElementById('caseRegisterHeaderCloseBtn'),

            // Status Transition Modal
            statusModal: document.getElementById('caseStatusModal'),
            statusModalCaseId: document.getElementById('statusModalCaseId'),
            statusModalCurrentStatus: document.getElementById('statusModalCurrentStatus'),
            statusModalAlert: document.getElementById('statusModalAlert'),
            statusModalOptionsContainer: document.getElementById('statusModalOptionsContainer'),
            statusModalDispatchNotice: document.getElementById('statusModalDispatchNotice'),
            statusModalGoDispatchesBtn: document.getElementById('statusModalGoDispatchesBtn'),
            statusModalTerminalNotice: document.getElementById('statusModalTerminalNotice'),
            statusModalSubmitBtn: document.getElementById('caseStatusSubmitBtn'),
            statusModalCancelBtn: document.getElementById('caseStatusCancelBtn'),
            statusModalHeaderCloseBtn: document.getElementById('caseStatusHeaderCloseBtn')
        };
    }

    /**
     * Traps Tab / Shift+Tab keyboard focus inside the active modal dialog.
     */
    function trapFocus(modalEl, initialFocusEl) {
        if (modalFocusCleanup) {
            modalFocusCleanup();
            modalFocusCleanup = null;
        }

        const focusableSelectors = [
            'a[href]',
            'button:not([disabled])',
            'input:not([disabled])',
            'select:not([disabled])',
            'textarea:not([disabled])',
            '[tabindex]:not([tabindex="-1"])'
        ].join(',');

        function getFocusableElements() {
            return Array.from(modalEl.querySelectorAll(focusableSelectors))
                .filter(el => el.offsetParent !== null && window.getComputedStyle(el).visibility !== 'hidden');
        }

        function handleKeyDown(e) {
            if (e.key !== 'Tab') return;

            const focusables = getFocusableElements();
            if (focusables.length === 0) {
                e.preventDefault();
                return;
            }

            const first = focusables[0];
            const last = focusables[focusables.length - 1];

            if (e.shiftKey) {
                if (document.activeElement === first || !modalEl.contains(document.activeElement)) {
                    e.preventDefault();
                    last.focus();
                }
            } else {
                if (document.activeElement === last || !modalEl.contains(document.activeElement)) {
                    e.preventDefault();
                    first.focus();
                }
            }
        }

        modalEl.addEventListener('keydown', handleKeyDown);

        requestAnimationFrame(() => {
            if (initialFocusEl && typeof initialFocusEl.focus === 'function' && !initialFocusEl.disabled) {
                initialFocusEl.focus();
            } else {
                const focusables = getFocusableElements();
                if (focusables.length > 0) {
                    focusables[0].focus();
                }
            }
        });

        modalFocusCleanup = () => {
            modalEl.removeEventListener('keydown', handleKeyDown);
        };
    }

    /**
     * Releases focus trap and restores focus to the triggering element.
     */
    function releaseFocusTrap() {
        if (modalFocusCleanup) {
            modalFocusCleanup();
            modalFocusCleanup = null;
        }
        if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
            try {
                if (document.contains(lastFocusedElement)) {
                    lastFocusedElement.focus();
                } else if (dom.view && document.contains(dom.view)) {
                    if (dom.refreshBtn && !dom.refreshBtn.classList.contains('hidden')) {
                        dom.refreshBtn.focus();
                    }
                }
            } catch (_) {}
        }
        lastFocusedElement = null;
    }

    /**
     * Determines user role capabilities.
     */
    function getRolePermissions() {
        const user = window.EmergencyAuth ? window.EmergencyAuth.getCurrentUser() : null;
        currentRole = user ? user.role : '';

        return {
            canView: ['ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR', 'AMBULANCE_DRIVER'].includes(currentRole),
            canCreate: ['ADMIN', 'DISPATCHER', 'PARAMEDIC'].includes(currentRole),
            canUpdateStatus: ['ADMIN', 'DISPATCHER', 'PARAMEDIC'].includes(currentRole),
            isDoctor: currentRole === 'EMERGENCY_DOCTOR',
            isDriver: currentRole === 'AMBULANCE_DRIVER',
            isHospitalAdmin: currentRole === 'HOSPITAL_ADMIN'
        };
    }

    /**
     * Returns allowed target transitions for a given case status in the UI.
     * Prevents case-dispatch deadlock: vehicle mobilization belongs strictly to the
     * Dispatches module (POST /api/dispatches). The manual Cases status dialog permits
     * cancellation before dispatch, and incident milestone updates after dispatch.
     */
    function getAllowedTransitions(currentStatus) {
        if (!currentStatus) return [];
        const curr = currentStatus.toUpperCase();

        switch (curr) {
            case 'REGISTERED':
                // REGISTERED -> DISPATCHED is suppressed to require proper vehicle dispatch
                // via POST /api/dispatches. Pre-dispatch cancellation is allowed.
                return [
                    { status: 'CANCELLED', label: 'Cancel Incident (CANCELLED)', desc: 'Call resolved or false alarm before vehicle dispatch' }
                ];
            case 'DISPATCHED':
                return [
                    { status: 'IN_TRANSIT', label: 'Mark In Transit (IN_TRANSIT)', desc: 'Update status: ambulance is en route to scene' },
                    { status: 'CANCELLED', label: 'Cancel Incident (CANCELLED)', desc: 'Incident cancelled while vehicle is en route' }
                ];
            case 'IN_TRANSIT':
                return [
                    { status: 'ARRIVED', label: 'Mark Arrived (ARRIVED)', desc: 'Update status: emergency crew arrived at scene' },
                    { status: 'CANCELLED', label: 'Cancel Incident (CANCELLED)', desc: 'Incident cancelled or aborted per dispatch instruction' }
                ];
            case 'ARRIVED':
                return [
                    { status: 'COMPLETED', label: 'Complete Case (COMPLETED)', desc: 'Patient handover completed at hospital facility' },
                    { status: 'CANCELLED', label: 'Cancel Incident (CANCELLED)', desc: 'Patient refused treatment or incident terminated' }
                ];
            case 'COMPLETED':
            case 'CANCELLED':
            default:
                return []; // Terminal states
        }
    }

    /**
     * Helper to render status badges with semantic colors.
     */
    function renderStatusBadge(status) {
        const badge = document.createElement('span');
        const s = (status || 'UNKNOWN').toUpperCase();
        badge.className = 'badge';

        switch (s) {
            case 'REGISTERED':
                badge.classList.add('badge-status-registered');
                break;
            case 'DISPATCHED':
                badge.classList.add('badge-status-dispatched');
                break;
            case 'IN_TRANSIT':
                badge.classList.add('badge-status-intransit');
                break;
            case 'ARRIVED':
                badge.classList.add('badge-status-arrived');
                break;
            case 'COMPLETED':
                badge.classList.add('badge-status-completed');
                break;
            case 'CANCELLED':
                badge.classList.add('badge-status-cancelled');
                break;
            default:
                badge.classList.add('badge-role');
        }

        badge.textContent = s;
        return badge;
    }

    /**
     * Helper to render priority badges with severity colors.
     */
    function renderPriorityBadge(priority) {
        const badge = document.createElement('span');
        const p = (priority || 'LOW').toUpperCase();
        badge.className = 'badge';

        switch (p) {
            case 'CRITICAL':
                badge.classList.add('badge-priority-critical');
                break;
            case 'HIGH':
                badge.classList.add('badge-priority-high');
                break;
            case 'MODERATE':
                badge.classList.add('badge-priority-moderate');
                break;
            case 'LOW':
            default:
                badge.classList.add('badge-priority-low');
        }

        badge.textContent = p;
        return badge;
    }

    function showModuleAlert(message, type = 'error') {
        if (!dom.caseAlert) return;
        dom.caseAlert.className = `alert alert-${type}`;
        dom.caseAlert.textContent = message;
        dom.caseAlert.classList.remove('hidden');
    }

    function hideModuleAlert() {
        if (!dom.caseAlert) return;
        dom.caseAlert.classList.add('hidden');
        dom.caseAlert.textContent = '';
    }

    function showRegisterAlert(message, type = 'error') {
        if (!dom.registerAlert) return;
        dom.registerAlert.className = `alert alert-${type}`;
        dom.registerAlert.textContent = message;
        dom.registerAlert.classList.remove('hidden');
    }

    function hideRegisterAlert() {
        if (!dom.registerAlert) return;
        dom.registerAlert.classList.add('hidden');
        dom.registerAlert.textContent = '';
    }

    function showStatusModalAlert(message, type = 'error') {
        if (!dom.statusModalAlert) return;
        dom.statusModalAlert.className = `alert alert-${type}`;
        dom.statusModalAlert.textContent = message;
        dom.statusModalAlert.classList.remove('hidden');
    }

    function hideStatusModalAlert() {
        if (!dom.statusModalAlert) return;
        dom.statusModalAlert.classList.add('hidden');
        dom.statusModalAlert.textContent = '';
    }

    function setViewState(state) {
        if (dom.loadingState) dom.loadingState.classList.toggle('hidden', state !== 'loading');
        if (dom.errorState) dom.errorState.classList.toggle('hidden', state !== 'error');
        if (dom.restrictedState) dom.restrictedState.classList.toggle('hidden', state !== 'restricted');
        if (dom.emptyState) dom.emptyState.classList.toggle('hidden', state !== 'empty');
        if (dom.tableWrapper) dom.tableWrapper.classList.toggle('hidden', state !== 'table');
    }

    /**
     * Loads cases from /api/cases.
     */
    async function loadCases() {
        const perms = getRolePermissions();
        hideModuleAlert();

        if (perms.isHospitalAdmin) {
            setViewState('restricted');
            return;
        }

        if (!perms.canView) {
            setViewState('restricted');
            return;
        }

        // Apply role notices
        if (dom.roleNotice) {
            if (perms.isDoctor) {
                dom.roleNotice.textContent = 'Notice: Emergency Doctor view active. Incident viewing is enabled; case registration and manual lifecycle status transitions are managed by dispatch/paramedic operations.';
                dom.roleNotice.classList.remove('hidden');
            } else if (perms.isDriver) {
                dom.roleNotice.textContent = 'Notice: Ambulance Driver view active. Displaying cases assigned to your vehicle missions. Manual status transitions are restricted.';
                dom.roleNotice.classList.remove('hidden');
            } else {
                dom.roleNotice.classList.add('hidden');
            }
        }

        // Manage Add button visibility
        if (dom.openAddBtn) {
            dom.openAddBtn.classList.toggle('hidden', !perms.canCreate);
        }
        if (dom.emptyAddBtn) {
            dom.emptyAddBtn.classList.toggle('hidden', !perms.canCreate);
        }

        setViewState('loading');

        try {
            const res = await window.EmergencyApi.apiFetch('/api/cases', { method: 'GET' });
            allCases = (res && Array.isArray(res.data)) ? res.data : [];
            applyFilters();
        } catch (err) {
            setViewState('error');
            if (dom.errorMessage) {
                dom.errorMessage.textContent = err.message || 'Failed to load emergency cases from server.';
            }
        }
    }

    /**
     * Filters cases by search text, status, and priority.
     */
    function applyFilters() {
        const query = dom.searchInput ? dom.searchInput.value.trim().toLowerCase() : '';
        const statusVal = dom.statusFilter ? dom.statusFilter.value : 'ALL';
        const priorityVal = dom.priorityFilter ? dom.priorityFilter.value : 'ALL';

        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.classList.toggle('hidden', !query);
        }

        currentFiltered = allCases.filter(c => {
            // Text search (case ID, location, type, symptoms)
            if (query) {
                const idMatch = c.caseId && (c.caseId.toString() === query || c.caseId.toString().startsWith(query));
                const locMatch = c.incidentLocation && c.incidentLocation.toLowerCase().includes(query);
                const typeMatch = c.emergencyType && c.emergencyType.toLowerCase().includes(query);
                const sympMatch = c.symptoms && c.symptoms.toLowerCase().includes(query);
                if (!idMatch && !locMatch && !typeMatch && !sympMatch) return false;
            }

            // Status filter
            if (statusVal !== 'ALL') {
                if (!c.caseStatus || c.caseStatus.toUpperCase() !== statusVal) return false;
            }

            // Priority filter
            if (priorityVal !== 'ALL') {
                if (!c.priorityLevel || c.priorityLevel.toUpperCase() !== priorityVal) return false;
            }

            return true;
        });

        renderTable(currentFiltered);
    }

    /**
     * Renders cases into the table body.
     */
    function renderTable(cases) {
        const perms = getRolePermissions();

        if (dom.countBadge) {
            const total = allCases.length;
            const shown = cases.length;
            dom.countBadge.textContent = (shown === total) ? `${total} cases` : `${shown} of ${total} cases`;
        }

        if (cases.length === 0) {
            setViewState('empty');
            if (dom.emptyDesc) {
                const hasFilters = (dom.searchInput && dom.searchInput.value.trim()) ||
                                   (dom.statusFilter && dom.statusFilter.value !== 'ALL') ||
                                   (dom.priorityFilter && dom.priorityFilter.value !== 'ALL');
                dom.emptyDesc.textContent = hasFilters
                    ? 'No emergency cases matched your search/filter criteria.'
                    : 'No emergency cases are currently registered in the system.';
            }
            return;
        }

        setViewState('table');
        if (!dom.tableBody) return;

        dom.tableBody.innerHTML = '';

        cases.forEach(c => {
            const tr = document.createElement('tr');

            // Case ID
            const tdId = document.createElement('td');
            tdId.style.fontWeight = '700';
            tdId.textContent = `#${c.caseId}`;
            tr.appendChild(tdId);

            // Patient Ref
            const tdPatient = document.createElement('td');
            tdPatient.textContent = `Patient #${c.patientId}`;
            tr.appendChild(tdPatient);

            // Type & Priority
            const tdType = document.createElement('td');
            const typeContainer = document.createElement('div');
            typeContainer.style.display = 'flex';
            typeContainer.style.flexDirection = 'column';
            typeContainer.style.gap = '4px';
            const typeText = document.createElement('span');
            typeText.style.fontWeight = '600';
            typeText.textContent = c.emergencyType || 'EMERGENCY';
            typeContainer.appendChild(typeText);
            typeContainer.appendChild(renderPriorityBadge(c.priorityLevel));
            tdType.appendChild(typeContainer);
            tr.appendChild(tdType);

            // Location
            const tdLoc = document.createElement('td');
            tdLoc.textContent = c.incidentLocation || 'Not specified';
            tr.appendChild(tdLoc);

            // Status
            const tdStatus = document.createElement('td');
            tdStatus.appendChild(renderStatusBadge(c.caseStatus));
            tr.appendChild(tdStatus);

            // Actions
            const tdActions = document.createElement('td');
            tdActions.style.textAlign = 'right';
            const actionContainer = document.createElement('div');
            actionContainer.className = 'table-actions';

            const viewBtn = document.createElement('button');
            viewBtn.type = 'button';
            viewBtn.className = 'btn btn-secondary btn-xs';
            viewBtn.textContent = 'View';
            viewBtn.addEventListener('click', (e) => openDetailsModal(c, e.currentTarget));
            actionContainer.appendChild(viewBtn);

            const transitions = getAllowedTransitions(c.caseStatus);
            if (perms.canUpdateStatus && transitions.length > 0) {
                const statusBtn = document.createElement('button');
                statusBtn.type = 'button';
                statusBtn.className = 'btn btn-primary btn-xs';
                statusBtn.textContent = 'Status';
                statusBtn.title = 'Advance or cancel case status';
                statusBtn.addEventListener('click', (e) => openStatusModal(c, e.currentTarget));
                actionContainer.appendChild(statusBtn);
            }

            tdActions.appendChild(actionContainer);
            tr.appendChild(tdActions);

            dom.tableBody.appendChild(tr);
        });
    }

    /**
     * Opens case details modal.
     */
    function openDetailsModal(emergencyCase, triggerEl) {
        if (!emergencyCase || !dom.detailsModal) return;
        lastFocusedElement = triggerEl || document.activeElement;
        const perms = getRolePermissions();

        if (dom.detailsTitle) dom.detailsTitle.textContent = `Emergency Case Details — #${emergencyCase.caseId}`;
        if (dom.detailsId) dom.detailsId.textContent = `#${emergencyCase.caseId}`;
        if (dom.detailsPatientId) dom.detailsPatientId.textContent = `Patient #${emergencyCase.patientId}`;
        if (dom.detailsLocation) dom.detailsLocation.textContent = emergencyCase.incidentLocation || 'Not specified';
        if (dom.detailsType) dom.detailsType.textContent = emergencyCase.emergencyType || 'Not specified';

        if (dom.detailsPriority) {
            dom.detailsPriority.innerHTML = '';
            dom.detailsPriority.appendChild(renderPriorityBadge(emergencyCase.priorityLevel));
        }

        if (dom.detailsStatus) {
            dom.detailsStatus.innerHTML = '';
            dom.detailsStatus.appendChild(renderStatusBadge(emergencyCase.caseStatus));
        }

        if (dom.detailsSymptoms) {
            dom.detailsSymptoms.textContent = emergencyCase.symptoms || 'None recorded';
        }

        const transitions = getAllowedTransitions(emergencyCase.caseStatus);
        if (dom.detailsUpdateStatusBtn) {
            if (perms.canUpdateStatus && transitions.length > 0) {
                dom.detailsUpdateStatusBtn.classList.remove('hidden');
                dom.detailsUpdateStatusBtn.onclick = () => {
                    const originalTrigger = lastFocusedElement;
                    closeDetailsModal();
                    openStatusModal(emergencyCase, originalTrigger);
                };
            } else {
                dom.detailsUpdateStatusBtn.classList.add('hidden');
            }
        }

        dom.detailsModal.classList.remove('hidden');
        trapFocus(dom.detailsModal, dom.detailsCloseBtn || dom.detailsHeaderCloseBtn);
    }

    function closeDetailsModal() {
        if (dom.detailsModal) {
            dom.detailsModal.classList.add('hidden');
        }
        releaseFocusTrap();
    }

    /**
     * Opens Status Transition Modal for a specific case.
     */
    function openStatusModal(emergencyCase, triggerEl) {
        if (!emergencyCase || !dom.statusModal) return;
        lastFocusedElement = triggerEl || document.activeElement;
        selectedCaseForStatus = emergencyCase;
        selectedTargetStatus = null;
        hideStatusModalAlert();

        if (dom.statusModalCaseId) dom.statusModalCaseId.textContent = `#${emergencyCase.caseId}`;
        if (dom.statusModalCurrentStatus) {
            dom.statusModalCurrentStatus.innerHTML = '';
            dom.statusModalCurrentStatus.appendChild(renderStatusBadge(emergencyCase.caseStatus));
        }

        const isRegistered = (emergencyCase.caseStatus || '').toUpperCase() === 'REGISTERED';
        if (dom.statusModalDispatchNotice) {
            dom.statusModalDispatchNotice.classList.toggle('hidden', !isRegistered);
        }

        const transitions = getAllowedTransitions(emergencyCase.caseStatus);

        if (dom.statusModalOptionsContainer) {
            dom.statusModalOptionsContainer.innerHTML = '';

            if (transitions.length === 0) {
                if (dom.statusModalTerminalNotice) dom.statusModalTerminalNotice.classList.remove('hidden');
                if (dom.statusModalSubmitBtn) dom.statusModalSubmitBtn.disabled = true;
            } else {
                if (dom.statusModalTerminalNotice) dom.statusModalTerminalNotice.classList.add('hidden');
                if (dom.statusModalSubmitBtn) {
                    dom.statusModalSubmitBtn.disabled = true; // Enabled once an option is picked
                    dom.statusModalSubmitBtn.textContent = 'Apply Transition';
                }

                transitions.forEach((trans, index) => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'transition-option-btn';
                    btn.innerHTML = `
                        <div>
                            <div class="transition-btn-title">${escapeHtml(trans.label)}</div>
                            <div class="transition-btn-desc">${escapeHtml(trans.desc)}</div>
                        </div>
                        <span class="badge ${trans.status === 'CANCELLED' ? 'badge-priority-critical' : 'badge-status-dispatched'}">${trans.status}</span>
                    `;

                    btn.addEventListener('click', () => {
                        // Deselect other buttons
                        dom.statusModalOptionsContainer.querySelectorAll('.transition-option-btn').forEach(b => b.classList.remove('selected'));
                        btn.classList.add('selected');
                        selectedTargetStatus = trans.status;
                        if (dom.statusModalSubmitBtn) {
                            dom.statusModalSubmitBtn.disabled = false;
                            dom.statusModalSubmitBtn.textContent = `Apply Transition (${trans.status})`;
                        }
                    });

                    dom.statusModalOptionsContainer.appendChild(btn);
                });
            }
        }

        dom.statusModal.classList.remove('hidden');
        const firstOption = dom.statusModalOptionsContainer.querySelector('button');
        trapFocus(dom.statusModal, firstOption || dom.statusModalCancelBtn);
    }

    function closeStatusModal() {
        if (dom.statusModal) {
            dom.statusModal.classList.add('hidden');
        }
        selectedCaseForStatus = null;
        selectedTargetStatus = null;
        releaseFocusTrap();
    }

    /**
     * Submits status transition to PUT /api/cases/{id}/status.
     */
    async function handleStatusSubmit() {
        if (!selectedCaseForStatus || !selectedTargetStatus) {
            showStatusModalAlert('Please select a valid target status transition.', 'error');
            return;
        }

        const caseId = selectedCaseForStatus.caseId;
        const target = selectedTargetStatus;

        dom.statusModalSubmitBtn.disabled = true;
        const origText = dom.statusModalSubmitBtn.textContent;
        dom.statusModalSubmitBtn.textContent = 'Updating status...';

        try {
            await window.EmergencyApi.apiFetch(`/api/cases/${caseId}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status: target })
            });

            closeStatusModal();
            showModuleAlert(`Emergency case #${caseId} status updated to ${target} successfully.`, 'success');
            await loadCases();
        } catch (err) {
            if (err.status === 409) {
                // Conflict: case was modified concurrently on server
                showStatusModalAlert('Status Conflict (409): The case was modified on the server by another request. Reloading current data...', 'error');
                const trigger = lastFocusedElement;
                // Refresh cases in background so state is synchronized
                await loadCases();
                const refreshed = allCases.find(c => c.caseId === caseId);
                if (refreshed) {
                    setTimeout(() => openStatusModal(refreshed, trigger), 1200);
                } else {
                    setTimeout(closeStatusModal, 1500);
                }
            } else {
                showStatusModalAlert(err.message || 'Failed to update case status.', 'error');
                dom.statusModalSubmitBtn.disabled = false;
                dom.statusModalSubmitBtn.textContent = origText;
            }
        }
    }

    /**
     * Loads patients from /api/patients to populate dropdown for case registration.
     */
    async function populatePatientOptions() {
        if (!dom.inputPatientSelect) return;
        dom.inputPatientSelect.innerHTML = '<option value="">-- Select Existing Patient --</option>';

        const perms = getRolePermissions();
        // Drivers and hospital admins cannot fetch patients
        if (!['ADMIN', 'DISPATCHER', 'PARAMEDIC'].includes(currentRole)) {
            return;
        }

        try {
            const res = await window.EmergencyApi.apiFetch('/api/patients', { method: 'GET' });
            if (res && Array.isArray(res.data) && res.data.length > 0) {
                res.data.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p.patientId;
                    opt.textContent = `Patient #${p.patientId} — ${p.patientName} (${p.age} yrs, ${p.gender})`;
                    dom.inputPatientSelect.appendChild(opt);
                });
            }
        } catch (e) {
            // Non-fatal: user can still input manual ID
        }
    }

    /**
     * Opens Add Case registration modal.
     */
    function openRegisterModal(triggerEl) {
        if (!triggerEl && document.activeElement) {
            triggerEl = document.activeElement;
        }
        lastFocusedElement = triggerEl || null;
        if (!dom.registerModal) return;
        hideRegisterAlert();
        if (dom.registerForm) dom.registerForm.reset();

        populatePatientOptions();

        dom.registerModal.classList.remove('hidden');
        trapFocus(dom.registerModal, dom.inputLocation);
    }

    function closeRegisterModal() {
        if (dom.registerModal) {
            dom.registerModal.classList.add('hidden');
        }
        releaseFocusTrap();
    }

    /**
     * Client-side validation for EmergencyCaseRequest.
     */
    function validateCasePayload(payload) {
        if (!payload.patientId || isNaN(payload.patientId) || payload.patientId <= 0) {
            return 'A valid Patient ID (greater than 0) is required.';
        }
        if (!payload.incidentLocation || !payload.incidentLocation.trim()) {
            return 'Incident location cannot be empty.';
        }
        if (!payload.emergencyType || !payload.emergencyType.trim()) {
            return 'Emergency type cannot be empty.';
        }
        if (!payload.symptoms || !payload.symptoms.trim()) {
            return 'Symptoms cannot be empty.';
        }
        return null;
    }

    /**
     * Submits new emergency case to POST /api/cases.
     */
    async function handleRegisterSubmit(e) {
        e.preventDefault();
        hideRegisterAlert();

        // Determine patientId from select dropdown or manual input
        let patientId = null;
        if (dom.inputPatientSelect && dom.inputPatientSelect.value) {
            patientId = parseInt(dom.inputPatientSelect.value, 10);
        } else if (dom.inputPatientManualId && dom.inputPatientManualId.value) {
            patientId = parseInt(dom.inputPatientManualId.value.trim(), 10);
        }

        const payload = {
            patientId: patientId,
            incidentLocation: dom.inputLocation ? dom.inputLocation.value.trim() : '',
            emergencyType: dom.inputType ? dom.inputType.value.trim() : '',
            symptoms: dom.inputSymptoms ? dom.inputSymptoms.value.trim() : ''
        };

        const validationErr = validateCasePayload(payload);
        if (validationErr) {
            showRegisterAlert(validationErr, 'error');
            return;
        }

        dom.registerSubmitBtn.disabled = true;
        const origText = dom.registerSubmitBtn.textContent;
        dom.registerSubmitBtn.textContent = 'Registering case...';

        try {
            const res = await window.EmergencyApi.apiFetch('/api/cases', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            const createdId = (res && res.data && res.data.caseId) ? res.data.caseId : '';
            closeRegisterModal();
            showModuleAlert(`Emergency case #${createdId} registered successfully with status REGISTERED.`, 'success');
            await loadCases();
        } catch (err) {
            showRegisterAlert(err.message || 'Failed to register emergency case.', 'error');
        } finally {
            dom.registerSubmitBtn.disabled = false;
            dom.registerSubmitBtn.textContent = origText;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Sets up event listeners once.
     */
    function setupEvents() {
        if (isInitialized) return;
        isInitialized = true;

        if (dom.refreshBtn) dom.refreshBtn.addEventListener('click', loadCases);
        if (dom.retryBtn) dom.retryBtn.addEventListener('click', loadCases);
        if (dom.openAddBtn) dom.openAddBtn.addEventListener('click', (e) => openRegisterModal(e.currentTarget));
        if (dom.emptyAddBtn) dom.emptyAddBtn.addEventListener('click', (e) => openRegisterModal(e.currentTarget));

        // Filters
        if (dom.searchInput) dom.searchInput.addEventListener('input', applyFilters);
        if (dom.statusFilter) dom.statusFilter.addEventListener('change', applyFilters);
        if (dom.priorityFilter) dom.priorityFilter.addEventListener('change', applyFilters);

        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.addEventListener('click', () => {
                dom.searchInput.value = '';
                applyFilters();
                dom.searchInput.focus();
            });
        }

        // Details Modal close events
        if (dom.detailsCloseBtn) dom.detailsCloseBtn.addEventListener('click', closeDetailsModal);
        if (dom.detailsHeaderCloseBtn) dom.detailsHeaderCloseBtn.addEventListener('click', closeDetailsModal);
        if (dom.detailsModal) {
            dom.detailsModal.addEventListener('click', (e) => {
                if (e.target === dom.detailsModal) closeDetailsModal();
            });
        }

        // Register Modal events
        if (dom.registerCancelBtn) dom.registerCancelBtn.addEventListener('click', closeRegisterModal);
        if (dom.registerHeaderCloseBtn) dom.registerHeaderCloseBtn.addEventListener('click', closeRegisterModal);
        if (dom.registerModal) {
            dom.registerModal.addEventListener('click', (e) => {
                if (e.target === dom.registerModal) closeRegisterModal();
            });
        }
        if (dom.registerForm) dom.registerForm.addEventListener('submit', handleRegisterSubmit);

        // When patient dropdown changes, clear manual input
        if (dom.inputPatientSelect) {
            dom.inputPatientSelect.addEventListener('change', () => {
                if (dom.inputPatientSelect.value && dom.inputPatientManualId) {
                    dom.inputPatientManualId.value = '';
                }
            });
        }
        if (dom.inputPatientManualId) {
            dom.inputPatientManualId.addEventListener('input', () => {
                if (dom.inputPatientManualId.value && dom.inputPatientSelect) {
                    dom.inputPatientSelect.value = '';
                }
            });
        }

        // Status Modal events
        if (dom.statusModalCancelBtn) dom.statusModalCancelBtn.addEventListener('click', closeStatusModal);
        if (dom.statusModalHeaderCloseBtn) dom.statusModalHeaderCloseBtn.addEventListener('click', closeStatusModal);
        if (dom.statusModal) {
            dom.statusModal.addEventListener('click', (e) => {
                if (e.target === dom.statusModal) closeStatusModal();
            });
        }
        if (dom.statusModalSubmitBtn) dom.statusModalSubmitBtn.addEventListener('click', handleStatusSubmit);

        // Status Modal Go to Dispatches navigation
        if (dom.statusModalGoDispatchesBtn) {
            dom.statusModalGoDispatchesBtn.addEventListener('click', () => {
                closeStatusModal();
                if (window.EmergencyApp && typeof window.EmergencyApp.switchModule === 'function') {
                    window.EmergencyApp.switchModule('dispatches');
                }
            });
        }

        // Keyboard escape
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (dom.statusModal && !dom.statusModal.classList.contains('hidden')) {
                    closeStatusModal();
                } else if (dom.registerModal && !dom.registerModal.classList.contains('hidden')) {
                    closeRegisterModal();
                } else if (dom.detailsModal && !dom.detailsModal.classList.contains('hidden')) {
                    closeDetailsModal();
                }
            }
        });
    }

    /**
     * Initializes the view when navigating to Cases module.
     */
    function initView() {
        cacheDom();
        setupEvents();
        loadCases();
    }

    window.EmergencyCases = {
        initView,
        loadCases,
        applyFilters,
        openDetailsModal,
        openStatusModal
    };

})(window);
