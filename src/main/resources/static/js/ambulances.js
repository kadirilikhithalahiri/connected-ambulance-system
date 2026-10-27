/**
 * Connected Ambulance System - Ambulance Fleet & Tracking Module
 * Handles ambulance inventory list, availability filtering, details inspection,
 * and driver/dispatcher location updates with finite coordinate validation.
 */
(function(window) {
    'use strict';

    let allAmbulances = [];
    let currentFiltered = [];
    let currentRole = null;
    let selectedAmbulanceForLocation = null;
    let isInitialized = false;

    // Accessibility focus management
    let modalFocusCleanup = null;
    let lastFocusedElement = null;

    // DOM cache
    let dom = {};

    function cacheDom() {
        dom = {
            view: document.getElementById('viewAmbulances'),
            roleNotice: document.getElementById('ambulanceRoleNotice'),
            ambulanceAlert: document.getElementById('ambulanceAlert'),
            searchInput: document.getElementById('ambulanceSearchInput'),
            clearSearchBtn: document.getElementById('clearAmbulanceSearchBtn'),
            availabilityFilter: document.getElementById('ambulanceAvailabilityFilter'),
            countBadge: document.getElementById('ambulanceCountBadge'),
            refreshBtn: document.getElementById('refreshAmbulancesBtn'),
            retryBtn: document.getElementById('retryAmbulancesBtn'),

            // State containers
            loadingState: document.getElementById('ambulanceLoadingState'),
            errorState: document.getElementById('ambulanceErrorState'),
            errorMessage: document.getElementById('ambulanceErrorMessage'),
            restrictedState: document.getElementById('ambulanceRestrictedState'),
            emptyState: document.getElementById('ambulanceEmptyState'),
            emptyDesc: document.getElementById('ambulanceEmptyDesc'),
            tableWrapper: document.getElementById('ambulanceTableWrapper'),
            tableBody: document.getElementById('ambulanceTableBody'),

            // Details Modal
            detailsModal: document.getElementById('ambulanceDetailsModal'),
            detailsTitle: document.getElementById('ambulanceDetailsTitle'),
            detailsId: document.getElementById('detailAmbulanceId'),
            detailsVehicleNumber: document.getElementById('detailAmbulanceVehicleNumber'),
            detailsAvailability: document.getElementById('detailAmbulanceAvailability'),
            detailsDriver: document.getElementById('detailAmbulanceDriver'),
            detailsParamedic: document.getElementById('detailAmbulanceParamedic'),
            detailsLat: document.getElementById('detailAmbulanceLat'),
            detailsLng: document.getElementById('detailAmbulanceLng'),
            detailsEquipment: document.getElementById('detailAmbulanceEquipment'),
            detailsUpdateLocationBtn: document.getElementById('detailAmbulanceUpdateLocationBtn'),
            detailsCloseBtn: document.getElementById('detailAmbulanceCloseBtn'),
            detailsHeaderCloseBtn: document.getElementById('detailAmbulanceHeaderCloseBtn'),

            // Location Modal
            locationModal: document.getElementById('ambulanceLocationModal'),
            locationTitle: document.getElementById('ambulanceLocationTitle'),
            locationForm: document.getElementById('ambLocationForm'),
            locationAlert: document.getElementById('ambLocAlert'),
            locVehicleNumber: document.getElementById('ambLocVehicleNumber'),
            locAvailability: document.getElementById('ambLocAvailability'),
            inputLat: document.getElementById('ambLocInputLat'),
            inputLng: document.getElementById('ambLocInputLng'),
            inputSpeed: document.getElementById('ambLocInputSpeed'),
            locSubmitBtn: document.getElementById('ambLocSubmitBtn'),
            locCancelBtn: document.getElementById('ambLocCancelBtn'),
            locHeaderCloseBtn: document.getElementById('ambLocHeaderCloseBtn')
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
            canUpdateLocation: ['ADMIN', 'DISPATCHER', 'AMBULANCE_DRIVER'].includes(currentRole),
            isAdmin: currentRole === 'ADMIN',
            isDispatcher: currentRole === 'DISPATCHER',
            isDriver: currentRole === 'AMBULANCE_DRIVER',
            isDoctor: currentRole === 'EMERGENCY_DOCTOR',
            isParamedic: currentRole === 'PARAMEDIC',
            isHospitalAdmin: currentRole === 'HOSPITAL_ADMIN'
        };
    }

    /**
     * Helper to render availability badge.
     */
    function renderAvailabilityBadge(status) {
        const badge = document.createElement('span');
        const s = (status || 'UNKNOWN').toUpperCase();
        badge.className = 'badge';

        switch (s) {
            case 'AVAILABLE':
                badge.classList.add('badge-availability-available');
                break;
            case 'DISPATCHED':
                badge.classList.add('badge-availability-dispatched');
                break;
            case 'MAINTENANCE':
                badge.classList.add('badge-availability-maintenance');
                break;
            default:
                badge.classList.add('badge-role');
        }

        badge.textContent = s;
        return badge;
    }

    function showModuleAlert(message, type = 'error') {
        if (!dom.ambulanceAlert) return;
        dom.ambulanceAlert.className = `alert alert-${type}`;
        dom.ambulanceAlert.textContent = message;
        dom.ambulanceAlert.classList.remove('hidden');
    }

    function hideModuleAlert() {
        if (!dom.ambulanceAlert) return;
        dom.ambulanceAlert.classList.add('hidden');
        dom.ambulanceAlert.textContent = '';
    }

    function showLocationAlert(message, type = 'error') {
        if (!dom.locationAlert) return;
        dom.locationAlert.className = `alert alert-${type}`;
        dom.locationAlert.textContent = message;
        dom.locationAlert.classList.remove('hidden');
    }

    function hideLocationAlert() {
        if (!dom.locationAlert) return;
        dom.locationAlert.classList.add('hidden');
        dom.locationAlert.textContent = '';
    }

    function setViewState(state) {
        if (dom.loadingState) dom.loadingState.classList.toggle('hidden', state !== 'loading');
        if (dom.errorState) dom.errorState.classList.toggle('hidden', state !== 'error');
        if (dom.restrictedState) dom.restrictedState.classList.toggle('hidden', state !== 'restricted');
        if (dom.emptyState) dom.emptyState.classList.toggle('hidden', state !== 'empty');
        if (dom.tableWrapper) dom.tableWrapper.classList.toggle('hidden', state !== 'table');
    }

    /**
     * Loads ambulances from /api/ambulances.
     */
    async function loadAmbulances() {
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
            if (perms.isDriver) {
                dom.roleNotice.textContent = 'Notice: Ambulance Driver view active. Displaying your assigned emergency vehicle. Location telemetry updates are authorized.';
                dom.roleNotice.classList.remove('hidden');
            } else if (perms.isDoctor) {
                dom.roleNotice.textContent = 'Notice: Emergency Doctor view active. Vehicle availability and equipment inventory inspection enabled.';
                dom.roleNotice.classList.remove('hidden');
            } else if (perms.isParamedic) {
                dom.roleNotice.textContent = 'Notice: Paramedic view active. Fleet inventory viewing enabled. Location updates are restricted to vehicle drivers and dispatch operations.';
                dom.roleNotice.classList.remove('hidden');
            } else {
                dom.roleNotice.classList.add('hidden');
            }
        }

        setViewState('loading');

        try {
            const res = await window.EmergencyApi.apiFetch('/api/ambulances', { method: 'GET' });
            allAmbulances = (res && Array.isArray(res.data)) ? res.data : [];
            applyFilters();
        } catch (err) {
            setViewState('error');
            if (dom.errorMessage) {
                dom.errorMessage.textContent = err.message || 'Failed to load ambulance fleet from server.';
            }
        }
    }

    /**
     * Filters ambulances by search query and availability.
     */
    function applyFilters() {
        const query = dom.searchInput ? dom.searchInput.value.trim().toLowerCase() : '';
        const availVal = dom.availabilityFilter ? dom.availabilityFilter.value : 'ALL';

        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.classList.toggle('hidden', !query);
        }

        currentFiltered = allAmbulances.filter(a => {
            // Text search (vehicleNumber, ambulanceId, equipment, driverId, paramedicId)
            if (query) {
                const idMatch = a.ambulanceId && (a.ambulanceId.toString() === query || a.ambulanceId.toString().startsWith(query));
                const vehicleMatch = a.vehicleNumber && a.vehicleNumber.toLowerCase().includes(query);
                const equipMatch = a.equipment && a.equipment.toLowerCase().includes(query);
                const driverMatch = a.driverId && (a.driverId.toString() === query || `driver #${a.driverId}`.toLowerCase().includes(query));
                const paramedicMatch = a.paramedicId && (a.paramedicId.toString() === query || `paramedic #${a.paramedicId}`.toLowerCase().includes(query));
                if (!idMatch && !vehicleMatch && !equipMatch && !driverMatch && !paramedicMatch) return false;
            }

            // Availability filter
            if (availVal !== 'ALL') {
                if (!a.availability || a.availability.toUpperCase() !== availVal) return false;
            }

            return true;
        });

        renderTable(currentFiltered);
    }

    /**
     * Renders ambulances into table body.
     */
    function renderTable(ambulances) {
        const perms = getRolePermissions();

        if (dom.countBadge) {
            const total = allAmbulances.length;
            const shown = ambulances.length;
            dom.countBadge.textContent = (shown === total) ? `${total} vehicles` : `${shown} of ${total} vehicles`;
        }

        if (ambulances.length === 0) {
            setViewState('empty');
            if (dom.emptyDesc) {
                const hasFilters = (dom.searchInput && dom.searchInput.value.trim()) ||
                                   (dom.availabilityFilter && dom.availabilityFilter.value !== 'ALL');
                dom.emptyDesc.textContent = hasFilters
                    ? 'No ambulances matched your search/filter criteria.'
                    : (perms.isDriver ? 'No ambulance is currently assigned to your driver profile.' : 'No ambulance records are currently registered in the system.');
            }
            return;
        }

        setViewState('table');
        if (!dom.tableBody) return;

        dom.tableBody.innerHTML = '';

        ambulances.forEach(a => {
            const tr = document.createElement('tr');

            // ID
            const tdId = document.createElement('td');
            tdId.style.fontWeight = '700';
            tdId.textContent = `#${a.ambulanceId}`;
            tr.appendChild(tdId);

            // Vehicle Number
            const tdNum = document.createElement('td');
            tdNum.style.fontWeight = '600';
            tdNum.textContent = a.vehicleNumber || 'Unregistered';
            tr.appendChild(tdNum);

            // Availability
            const tdAvail = document.createElement('td');
            tdAvail.appendChild(renderAvailabilityBadge(a.availability));
            tr.appendChild(tdAvail);

            // Assigned Crew
            const tdCrew = document.createElement('td');
            const crewContainer = document.createElement('div');
            crewContainer.style.display = 'flex';
            crewContainer.style.flexDirection = 'column';
            crewContainer.style.gap = '2px';
            crewContainer.style.fontSize = '12px';

            const driverSpan = document.createElement('span');
            driverSpan.textContent = a.driverId > 0 ? `Driver: #${a.driverId}` : 'Driver: Unassigned';
            driverSpan.style.color = a.driverId > 0 ? 'var(--navy-900)' : 'var(--text-muted)';
            if (a.driverId > 0) driverSpan.style.fontWeight = '600';
            crewContainer.appendChild(driverSpan);

            const medicSpan = document.createElement('span');
            medicSpan.textContent = a.paramedicId > 0 ? `Paramedic: #${a.paramedicId}` : 'Paramedic: Unassigned';
            medicSpan.style.color = a.paramedicId > 0 ? 'var(--navy-900)' : 'var(--text-muted)';
            crewContainer.appendChild(medicSpan);

            tdCrew.appendChild(crewContainer);
            tr.appendChild(tdCrew);

            // GPS Coordinates
            const tdCoords = document.createElement('td');
            const coordsText = (a.gpsLatitude !== undefined && a.gpsLongitude !== undefined)
                ? `${Number(a.gpsLatitude).toFixed(4)}, ${Number(a.gpsLongitude).toFixed(4)}`
                : 'Not available';
            tdCoords.textContent = coordsText;
            tdCoords.style.fontSize = '12px';
            tdCoords.style.fontFamily = 'monospace';
            tr.appendChild(tdCoords);

            // Equipment
            const tdEquip = document.createElement('td');
            tdEquip.textContent = a.equipment || 'Standard BLS';
            tdEquip.style.maxWidth = '220px';
            tdEquip.style.overflow = 'hidden';
            tdEquip.style.textOverflow = 'ellipsis';
            tdEquip.style.whiteSpace = 'nowrap';
            tdEquip.title = a.equipment || 'Standard BLS';
            tr.appendChild(tdEquip);

            // Actions
            const tdActions = document.createElement('td');
            tdActions.style.textAlign = 'right';
            const actionContainer = document.createElement('div');
            actionContainer.className = 'table-actions';

            const viewBtn = document.createElement('button');
            viewBtn.type = 'button';
            viewBtn.className = 'btn btn-secondary btn-xs';
            viewBtn.textContent = 'View';
            viewBtn.addEventListener('click', (e) => openDetailsModal(a, e.currentTarget));
            actionContainer.appendChild(viewBtn);

            if (perms.canUpdateLocation) {
                const locBtn = document.createElement('button');
                locBtn.type = 'button';
                locBtn.className = 'btn btn-primary btn-xs';
                locBtn.textContent = 'Location';
                locBtn.title = 'Update vehicle GPS coordinates';
                locBtn.addEventListener('click', (e) => openLocationModal(a, e.currentTarget));
                actionContainer.appendChild(locBtn);
            }

            tdActions.appendChild(actionContainer);
            tr.appendChild(tdActions);

            dom.tableBody.appendChild(tr);
        });
    }

    /**
     * Opens ambulance details modal.
     */
    function openDetailsModal(ambulance, triggerEl) {
        if (!ambulance || !dom.detailsModal) return;
        lastFocusedElement = triggerEl || document.activeElement;
        const perms = getRolePermissions();

        if (dom.detailsTitle) dom.detailsTitle.textContent = `Ambulance Details — ${ambulance.vehicleNumber || '#' + ambulance.ambulanceId}`;
        if (dom.detailsId) dom.detailsId.textContent = `#${ambulance.ambulanceId}`;
        if (dom.detailsVehicleNumber) dom.detailsVehicleNumber.textContent = ambulance.vehicleNumber || 'Unregistered';

        if (dom.detailsAvailability) {
            dom.detailsAvailability.innerHTML = '';
            dom.detailsAvailability.appendChild(renderAvailabilityBadge(ambulance.availability));
        }

        if (dom.detailsDriver) {
            dom.detailsDriver.textContent = ambulance.driverId > 0 ? `Driver #${ambulance.driverId}` : 'Unassigned';
        }

        if (dom.detailsParamedic) {
            dom.detailsParamedic.textContent = ambulance.paramedicId > 0 ? `Paramedic #${ambulance.paramedicId}` : 'Unassigned';
        }

        if (dom.detailsLat) {
            dom.detailsLat.textContent = ambulance.gpsLatitude !== undefined ? Number(ambulance.gpsLatitude).toFixed(6) : '--';
        }

        if (dom.detailsLng) {
            dom.detailsLng.textContent = ambulance.gpsLongitude !== undefined ? Number(ambulance.gpsLongitude).toFixed(6) : '--';
        }

        if (dom.detailsEquipment) {
            dom.detailsEquipment.textContent = ambulance.equipment || 'None listed';
        }

        if (dom.detailsUpdateLocationBtn) {
            if (perms.canUpdateLocation) {
                dom.detailsUpdateLocationBtn.classList.remove('hidden');
                dom.detailsUpdateLocationBtn.onclick = () => {
                    const originalTrigger = lastFocusedElement;
                    closeDetailsModal();
                    openLocationModal(ambulance, originalTrigger);
                };
            } else {
                dom.detailsUpdateLocationBtn.classList.add('hidden');
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
     * Opens Update Location modal.
     */
    function openLocationModal(ambulance, triggerEl) {
        if (!ambulance || !dom.locationModal) return;
        lastFocusedElement = triggerEl || document.activeElement;
        selectedAmbulanceForLocation = ambulance;
        hideLocationAlert();

        if (dom.locVehicleNumber) dom.locVehicleNumber.textContent = `${ambulance.vehicleNumber} (#${ambulance.ambulanceId})`;
        if (dom.locAvailability) {
            dom.locAvailability.innerHTML = '';
            dom.locAvailability.appendChild(renderAvailabilityBadge(ambulance.availability));
        }

        if (dom.inputLat) dom.inputLat.value = (ambulance.gpsLatitude !== undefined && ambulance.gpsLatitude !== null) ? ambulance.gpsLatitude : '';
        if (dom.inputLng) dom.inputLng.value = (ambulance.gpsLongitude !== undefined && ambulance.gpsLongitude !== null) ? ambulance.gpsLongitude : '';
        if (dom.inputSpeed) dom.inputSpeed.value = '';

        dom.locationModal.classList.remove('hidden');
        trapFocus(dom.locationModal, dom.inputLat);
    }

    function closeLocationModal() {
        if (dom.locationModal) {
            dom.locationModal.classList.add('hidden');
        }
        selectedAmbulanceForLocation = null;
        releaseFocusTrap();
    }

    /**
     * Client validation for AmbulanceLocationUpdateRequest.
     */
    function validateLocationPayload(payload) {
        if (payload.latitude === null || isNaN(payload.latitude) || !isFinite(payload.latitude) || payload.latitude < -90.0 || payload.latitude > 90.0) {
            return 'Latitude must be a valid finite number between -90.0 and +90.0 degrees.';
        }
        if (payload.longitude === null || isNaN(payload.longitude) || !isFinite(payload.longitude) || payload.longitude < -180.0 || payload.longitude > 180.0) {
            return 'Longitude must be a valid finite number between -180.0 and +180.0 degrees.';
        }
        if (payload.speed !== null && (isNaN(payload.speed) || !isFinite(payload.speed) || payload.speed < 0.0 || payload.speed > 250.0)) {
            return 'Speed must be a valid finite number between 0.0 and 250.0 km/h.';
        }
        return null;
    }

    /**
     * Submits location update to PUT /api/ambulances/{id}/location.
     */
    async function handleLocationSubmit(e) {
        e.preventDefault();
        if (!selectedAmbulanceForLocation) {
            showLocationAlert('No ambulance selected for location update.', 'error');
            return;
        }

        const ambId = selectedAmbulanceForLocation.ambulanceId;
        const latVal = dom.inputLat ? parseFloat(dom.inputLat.value.trim()) : NaN;
        const lngVal = dom.inputLng ? parseFloat(dom.inputLng.value.trim()) : NaN;

        let speedVal = null;
        if (dom.inputSpeed && dom.inputSpeed.value.trim() !== '') {
            speedVal = parseFloat(dom.inputSpeed.value.trim());
        }

        const payload = {
            latitude: isNaN(latVal) ? null : latVal,
            longitude: isNaN(lngVal) ? null : lngVal,
            speed: speedVal
        };

        const validationErr = validateLocationPayload(payload);
        if (validationErr) {
            showLocationAlert(validationErr, 'error');
            return;
        }

        dom.locSubmitBtn.disabled = true;
        const origText = dom.locSubmitBtn.textContent;
        dom.locSubmitBtn.textContent = 'Updating location...';

        try {
            await window.EmergencyApi.apiFetch(`/api/ambulances/${ambId}/location`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

            closeLocationModal();
            showModuleAlert(`Ambulance #${ambId} (${selectedAmbulanceForLocation.vehicleNumber}) location updated successfully.`, 'success');
            await loadAmbulances();
        } catch (err) {
            showLocationAlert(err.message || 'Failed to update ambulance location.', 'error');
        } finally {
            dom.locSubmitBtn.disabled = false;
            dom.locSubmitBtn.textContent = origText;
        }
    }

    /**
     * Sets up event listeners once.
     */
    function setupEvents() {
        if (isInitialized) return;
        isInitialized = true;

        if (dom.refreshBtn) dom.refreshBtn.addEventListener('click', loadAmbulances);
        if (dom.retryBtn) dom.retryBtn.addEventListener('click', loadAmbulances);

        // Filters
        if (dom.searchInput) dom.searchInput.addEventListener('input', applyFilters);
        if (dom.availabilityFilter) dom.availabilityFilter.addEventListener('change', applyFilters);

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

        // Location Modal events
        if (dom.locCancelBtn) dom.locCancelBtn.addEventListener('click', closeLocationModal);
        if (dom.locHeaderCloseBtn) dom.locHeaderCloseBtn.addEventListener('click', closeLocationModal);
        if (dom.locationModal) {
            dom.locationModal.addEventListener('click', (e) => {
                if (e.target === dom.locationModal) closeLocationModal();
            });
        }
        if (dom.locationForm) dom.locationForm.addEventListener('submit', handleLocationSubmit);

        // Global Escape key
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (dom.locationModal && !dom.locationModal.classList.contains('hidden')) {
                    closeLocationModal();
                } else if (dom.detailsModal && !dom.detailsModal.classList.contains('hidden')) {
                    closeDetailsModal();
                }
            }
        });
    }

    /**
     * Initializes the view when navigating to Ambulances module.
     */
    function initView() {
        cacheDom();
        setupEvents();
        loadAmbulances();
    }

    window.EmergencyAmbulances = {
        initView,
        loadAmbulances,
        applyFilters,
        openDetailsModal,
        openLocationModal
    };

})(window);
