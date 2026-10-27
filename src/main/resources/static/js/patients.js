/**
 * Connected Ambulance System - Patient Management Module
 * Handles patient registry view, search, details inspection, and add/edit forms.
 */
(function(window) {
    'use strict';

    let allPatients = [];
    let currentFiltered = [];
    let currentRole = null;
    let currentEditingId = null;
    let isInitialized = false;

    // DOM cache
    let dom = {};

    function cacheDom() {
        dom = {
            view: document.getElementById('viewPatients'),
            roleNotice: document.getElementById('patientRoleNotice'),
            patientAlert: document.getElementById('patientAlert'),
            searchInput: document.getElementById('patientSearchInput'),
            clearSearchBtn: document.getElementById('clearPatientSearchBtn'),
            countBadge: document.getElementById('patientCountBadge'),
            refreshBtn: document.getElementById('refreshPatientsBtn'),
            openAddBtn: document.getElementById('openAddPatientBtn'),
            emptyAddBtn: document.getElementById('emptyAddPatientBtn'),
            retryBtn: document.getElementById('retryPatientsBtn'),

            // State containers
            loadingState: document.getElementById('patientLoadingState'),
            errorState: document.getElementById('patientErrorState'),
            errorMessage: document.getElementById('patientErrorMessage'),
            restrictedState: document.getElementById('patientRestrictedState'),
            emptyState: document.getElementById('patientEmptyState'),
            emptyDesc: document.getElementById('patientEmptyDesc'),
            tableWrapper: document.getElementById('patientTableWrapper'),
            tableBody: document.getElementById('patientTableBody'),

            // Details Modal
            detailsModal: document.getElementById('patientDetailsModal'),
            detailsTitle: document.getElementById('patientDetailsTitle'),
            detailsId: document.getElementById('detailPatientId'),
            detailsName: document.getElementById('detailPatientName'),
            detailsAgeGender: document.getElementById('detailPatientAgeGender'),
            detailsContact: document.getElementById('detailPatientContact'),
            detailsBloodGroup: document.getElementById('detailPatientBloodGroup'),
            detailsAllergies: document.getElementById('detailPatientAllergies'),
            detailsHistory: document.getElementById('detailPatientHistory'),
            detailsEditBtn: document.getElementById('detailEditBtn'),
            detailsCloseBtn: document.getElementById('detailCloseBtn'),
            detailsHeaderCloseBtn: document.getElementById('detailHeaderCloseBtn'),

            // Form Modal
            formModal: document.getElementById('patientFormModal'),
            formModalTitle: document.getElementById('patientFormModalTitle'),
            formAlert: document.getElementById('patientFormAlert'),
            form: document.getElementById('patientForm'),
            inputName: document.getElementById('patientFormName'),
            inputAge: document.getElementById('patientFormAge'),
            inputGender: document.getElementById('patientFormGender'),
            inputBloodGroup: document.getElementById('patientFormBloodGroup'),
            inputContact: document.getElementById('patientFormContact'),
            inputAllergies: document.getElementById('patientFormAllergies'),
            inputHistory: document.getElementById('patientFormHistory'),
            formSubmitBtn: document.getElementById('patientFormSubmitBtn'),
            formCancelBtn: document.getElementById('patientFormCancelBtn'),
            formHeaderCloseBtn: document.getElementById('patientFormHeaderCloseBtn')
        };
    }

    /**
     * Determines user role capabilities.
     */
    function getRolePermissions() {
        const user = window.EmergencyAuth ? window.EmergencyAuth.getCurrentUser() : null;
        currentRole = user ? user.role : '';

        return {
            canView: ['ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR'].includes(currentRole),
            canCreate: ['ADMIN', 'DISPATCHER', 'PARAMEDIC'].includes(currentRole),
            canEdit: ['ADMIN', 'PARAMEDIC', 'EMERGENCY_DOCTOR'].includes(currentRole),
            isDispatcher: currentRole === 'DISPATCHER',
            isMedical: ['ADMIN', 'PARAMEDIC', 'EMERGENCY_DOCTOR'].includes(currentRole)
        };
    }

    /**
     * Displays a dismissible alert on the patient view.
     */
    function showModuleAlert(message, type = 'error') {
        if (!dom.patientAlert) return;
        dom.patientAlert.className = `alert alert-${type}`;
        dom.patientAlert.textContent = message;
        dom.patientAlert.classList.remove('hidden');
    }

    function hideModuleAlert() {
        if (!dom.patientAlert) return;
        dom.patientAlert.classList.add('hidden');
        dom.patientAlert.textContent = '';
    }

    /**
     * Shows form-level validation alert inside the modal.
     */
    function showFormAlert(message, type = 'error') {
        if (!dom.formAlert) return;
        dom.formAlert.className = `alert alert-${type}`;
        dom.formAlert.textContent = message;
        dom.formAlert.classList.remove('hidden');
    }

    function hideFormAlert() {
        if (!dom.formAlert) return;
        dom.formAlert.classList.add('hidden');
        dom.formAlert.textContent = '';
    }

    /**
     * Updates visibility of state containers (loading, error, restricted, empty, table).
     */
    function setViewState(state) {
        if (dom.loadingState) dom.loadingState.classList.toggle('hidden', state !== 'loading');
        if (dom.errorState) dom.errorState.classList.toggle('hidden', state !== 'error');
        if (dom.restrictedState) dom.restrictedState.classList.toggle('hidden', state !== 'restricted');
        if (dom.emptyState) dom.emptyState.classList.toggle('hidden', state !== 'empty');
        if (dom.tableWrapper) dom.tableWrapper.classList.toggle('hidden', state !== 'table');
    }

    /**
     * Loads patients from /api/patients.
     */
    async function loadPatients() {
        const perms = getRolePermissions();
        hideModuleAlert();

        if (!perms.canView) {
            setViewState('restricted');
            return;
        }

        // Apply role notice for dispatchers
        if (dom.roleNotice) {
            if (perms.isDispatcher) {
                dom.roleNotice.textContent = 'Notice: Dispatcher view active. Clinical details (blood group, medical history, allergies) are restricted to medical personnel.';
                dom.roleNotice.classList.remove('hidden');
            } else {
                dom.roleNotice.classList.add('hidden');
            }
        }

        // Manage button visibility
        if (dom.openAddBtn) {
            dom.openAddBtn.classList.toggle('hidden', !perms.canCreate);
        }
        if (dom.emptyAddBtn) {
            dom.emptyAddBtn.classList.toggle('hidden', !perms.canCreate);
        }

        setViewState('loading');

        try {
            const res = await window.EmergencyApi.apiFetch('/api/patients', { method: 'GET' });
            allPatients = (res && Array.isArray(res.data)) ? res.data : [];
            currentFiltered = allPatients;

            const searchTerm = dom.searchInput ? dom.searchInput.value.trim() : '';
            if (searchTerm) {
                filterPatients(searchTerm);
            } else {
                renderTable(allPatients);
            }
        } catch (err) {
            setViewState('error');
            if (dom.errorMessage) {
                dom.errorMessage.textContent = err.message || 'Failed to load patient records from server.';
            }
        }
    }

    /**
     * Filters patients by name or patientId locally.
     */
    function filterPatients(query) {
        const term = (query || '').toLowerCase().trim();

        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.classList.toggle('hidden', !term);
        }

        if (!term) {
            currentFiltered = allPatients;
        } else {
            currentFiltered = allPatients.filter(p => {
                const nameMatch = p.patientName && p.patientName.toLowerCase().includes(term);
                const idMatch = p.patientId && p.patientId.toString() === term;
                return nameMatch || idMatch;
            });
        }

        renderTable(currentFiltered);
    }

    /**
     * Renders patient list into the table.
     */
    function renderTable(patients) {
        const perms = getRolePermissions();

        if (dom.countBadge) {
            const total = allPatients.length;
            const shown = patients.length;
            dom.countBadge.textContent = (shown === total) ? `${total} records` : `${shown} of ${total} records`;
        }

        if (patients.length === 0) {
            setViewState('empty');
            if (dom.emptyDesc) {
                const hasSearch = dom.searchInput && dom.searchInput.value.trim();
                dom.emptyDesc.textContent = hasSearch
                    ? 'No patient records matched your search query.'
                    : 'No patient records are registered in the system yet.';
            }
            return;
        }

        setViewState('table');
        if (!dom.tableBody) return;

        dom.tableBody.innerHTML = '';

        patients.forEach(patient => {
            const tr = document.createElement('tr');

            // ID
            const tdId = document.createElement('td');
            tdId.style.fontWeight = '600';
            tdId.textContent = `#${patient.patientId}`;
            tr.appendChild(tdId);

            // Name & Demographics
            const tdName = document.createElement('td');
            const nameEl = document.createElement('div');
            nameEl.style.fontWeight = '600';
            nameEl.style.color = 'var(--navy-900)';
            nameEl.textContent = patient.patientName;
            const demoEl = document.createElement('div');
            demoEl.style.fontSize = '12px';
            demoEl.style.color = 'var(--text-muted)';
            demoEl.textContent = `${patient.age} yrs • ${patient.gender}`;
            tdName.appendChild(nameEl);
            tdName.appendChild(demoEl);
            tr.appendChild(tdName);

            // Emergency Contact
            const tdContact = document.createElement('td');
            tdContact.textContent = patient.emergencyContact || 'None';
            tr.appendChild(tdContact);

            // Blood Group
            const tdBlood = document.createElement('td');
            if (patient.bloodGroup) {
                const badge = document.createElement('span');
                badge.className = 'badge badge-role';
                badge.textContent = patient.bloodGroup;
                tdBlood.appendChild(badge);
            } else if (perms.isDispatcher) {
                const badge = document.createElement('span');
                badge.className = 'badge badge-restricted';
                badge.textContent = 'Restricted';
                tdBlood.appendChild(badge);
            } else {
                tdBlood.textContent = 'Unknown';
            }
            tr.appendChild(tdBlood);

            // Allergies
            const tdAllergies = document.createElement('td');
            if (patient.allergies) {
                tdAllergies.textContent = patient.allergies;
            } else if (perms.isDispatcher) {
                const badge = document.createElement('span');
                badge.className = 'badge badge-restricted';
                badge.textContent = 'Restricted';
                tdAllergies.appendChild(badge);
            } else {
                tdAllergies.textContent = 'None';
            }
            tr.appendChild(tdAllergies);

            // Actions
            const tdActions = document.createElement('td');
            tdActions.style.textAlign = 'right';
            const actionContainer = document.createElement('div');
            actionContainer.className = 'table-actions';

            const viewBtn = document.createElement('button');
            viewBtn.type = 'button';
            viewBtn.className = 'btn btn-secondary btn-xs';
            viewBtn.textContent = 'View';
            viewBtn.addEventListener('click', () => openDetailsModal(patient));
            actionContainer.appendChild(viewBtn);

            if (perms.canEdit) {
                const editBtn = document.createElement('button');
                editBtn.type = 'button';
                editBtn.className = 'btn btn-primary btn-xs';
                editBtn.textContent = 'Edit';
                editBtn.addEventListener('click', () => openFormModal(patient));
                actionContainer.appendChild(editBtn);
            }

            tdActions.appendChild(actionContainer);
            tr.appendChild(tdActions);

            dom.tableBody.appendChild(tr);
        });
    }

    /**
     * Opens patient details modal.
     */
    function openDetailsModal(patient) {
        if (!patient || !dom.detailsModal) return;
        const perms = getRolePermissions();

        if (dom.detailsTitle) dom.detailsTitle.textContent = `Patient Details — ${patient.patientName}`;
        if (dom.detailsId) dom.detailsId.textContent = `#${patient.patientId}`;
        if (dom.detailsName) dom.detailsName.textContent = patient.patientName;
        if (dom.detailsAgeGender) dom.detailsAgeGender.textContent = `${patient.age} years old (${patient.gender})`;
        if (dom.detailsContact) dom.detailsContact.textContent = patient.emergencyContact || 'None';

        // Clinical fields - role aware
        if (dom.detailsBloodGroup) {
            dom.detailsBloodGroup.innerHTML = '';
            if (patient.bloodGroup) {
                dom.detailsBloodGroup.textContent = patient.bloodGroup;
            } else if (perms.isDispatcher) {
                dom.detailsBloodGroup.innerHTML = '<span class="badge badge-restricted">Restricted (Medical Personnel Only)</span>';
            } else {
                dom.detailsBloodGroup.textContent = 'Unknown / Not Recorded';
            }
        }

        if (dom.detailsAllergies) {
            dom.detailsAllergies.innerHTML = '';
            if (patient.allergies) {
                dom.detailsAllergies.textContent = patient.allergies;
            } else if (perms.isDispatcher) {
                dom.detailsAllergies.innerHTML = '<span class="badge badge-restricted">Restricted (Medical Personnel Only)</span>';
            } else {
                dom.detailsAllergies.textContent = 'None reported';
            }
        }

        if (dom.detailsHistory) {
            dom.detailsHistory.innerHTML = '';
            if (patient.medicalHistory) {
                dom.detailsHistory.textContent = patient.medicalHistory;
            } else if (perms.isDispatcher) {
                dom.detailsHistory.innerHTML = '<span class="badge badge-restricted">Restricted (Medical Personnel Only)</span>';
            } else {
                dom.detailsHistory.textContent = 'None recorded';
            }
        }

        if (dom.detailsEditBtn) {
            if (perms.canEdit) {
                dom.detailsEditBtn.classList.remove('hidden');
                dom.detailsEditBtn.onclick = () => {
                    closeDetailsModal();
                    openFormModal(patient);
                };
            } else {
                dom.detailsEditBtn.classList.add('hidden');
            }
        }

        dom.detailsModal.classList.remove('hidden');
    }

    function closeDetailsModal() {
        if (dom.detailsModal) {
            dom.detailsModal.classList.add('hidden');
        }
    }

    /**
     * Opens Add or Edit patient form modal.
     */
    function openFormModal(patientToEdit = null) {
        if (!dom.formModal) return;
        hideFormAlert();

        if (patientToEdit) {
            currentEditingId = patientToEdit.patientId;
            if (dom.formModalTitle) dom.formModalTitle.textContent = `Edit Patient Record (#${patientToEdit.patientId})`;
            if (dom.formSubmitBtn) dom.formSubmitBtn.textContent = 'Save Changes';

            dom.inputName.value = patientToEdit.patientName || '';
            dom.inputAge.value = patientToEdit.age || '';
            dom.inputGender.value = (patientToEdit.gender || 'OTHER').toUpperCase();
            dom.inputBloodGroup.value = patientToEdit.bloodGroup || '';
            dom.inputContact.value = patientToEdit.emergencyContact || '';
            dom.inputAllergies.value = patientToEdit.allergies || '';
            dom.inputHistory.value = patientToEdit.medicalHistory || '';
        } else {
            currentEditingId = null;
            if (dom.formModalTitle) dom.formModalTitle.textContent = 'Register New Patient';
            if (dom.formSubmitBtn) dom.formSubmitBtn.textContent = 'Register Patient';

            if (dom.form) dom.form.reset();
            dom.inputGender.value = 'MALE';
        }

        dom.formModal.classList.remove('hidden');
        if (dom.inputName) dom.inputName.focus();
    }

    function closeFormModal() {
        if (dom.formModal) {
            dom.formModal.classList.add('hidden');
        }
        currentEditingId = null;
    }

    /**
     * Client-side validation matching backend PatientRequest / ValidationUtil.
     */
    function validateForm(payload) {
        if (!payload.patientName || !payload.patientName.trim()) {
            return 'Patient name cannot be empty.';
        }
        if (!/^[a-zA-Z ]+$/.test(payload.patientName.trim())) {
            return 'Patient name must contain only letters and spaces.';
        }

        const ageNum = parseInt(payload.age, 10);
        if (isNaN(ageNum) || ageNum < 1 || ageNum > 120) {
            return 'Age must be between 1 and 120.';
        }

        const validGenders = ['MALE', 'FEMALE', 'OTHER'];
        if (!payload.gender || !validGenders.includes(payload.gender.toUpperCase())) {
            return 'Gender must be MALE, FEMALE, or OTHER.';
        }

        if (!payload.emergencyContact || !payload.emergencyContact.trim()) {
            return 'Emergency contact cannot be empty.';
        }
        if (!/^\d{10,15}$/.test(payload.emergencyContact.trim())) {
            return 'Emergency contact must be between 10 and 15 digits.';
        }

        return null;
    }

    /**
     * Handles patient form submission.
     */
    async function handleFormSubmit(e) {
        e.preventDefault();
        hideFormAlert();

        const payload = {
            patientName: dom.inputName.value.trim(),
            age: parseInt(dom.inputAge.value, 10),
            gender: dom.inputGender.value,
            bloodGroup: dom.inputBloodGroup.value.trim() || null,
            medicalHistory: dom.inputHistory.value.trim() || null,
            allergies: dom.inputAllergies.value.trim() || null,
            emergencyContact: dom.inputContact.value.trim()
        };

        const validationErr = validateForm(payload);
        if (validationErr) {
            showFormAlert(validationErr, 'error');
            return;
        }

        dom.formSubmitBtn.disabled = true;
        const origText = dom.formSubmitBtn.textContent;
        dom.formSubmitBtn.textContent = 'Saving patient record...';

        try {
            if (currentEditingId) {
                // PUT update
                const res = await window.EmergencyApi.apiFetch(`/api/patients/${currentEditingId}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                closeFormModal();
                showModuleAlert(`Patient #${currentEditingId} (${payload.patientName}) updated successfully.`, 'success');
            } else {
                // POST create
                const res = await window.EmergencyApi.apiFetch('/api/patients', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                const createdId = (res && res.data && res.data.patientId) ? res.data.patientId : '';
                closeFormModal();
                showModuleAlert(`Patient #${createdId} (${payload.patientName}) registered successfully.`, 'success');
            }

            await loadPatients();
        } catch (err) {
            showFormAlert(err.message || 'Operation failed. Please verify form details.', 'error');
        } finally {
            dom.formSubmitBtn.disabled = false;
            dom.formSubmitBtn.textContent = origText;
        }
    }

    /**
     * Sets up event listeners once.
     */
    function setupEvents() {
        if (isInitialized) return;
        isInitialized = true;

        if (dom.refreshBtn) {
            dom.refreshBtn.addEventListener('click', loadPatients);
        }
        if (dom.retryBtn) {
            dom.retryBtn.addEventListener('click', loadPatients);
        }
        if (dom.openAddBtn) {
            dom.openAddBtn.addEventListener('click', () => openFormModal(null));
        }
        if (dom.emptyAddBtn) {
            dom.emptyAddBtn.addEventListener('click', () => openFormModal(null));
        }

        // Search input
        if (dom.searchInput) {
            dom.searchInput.addEventListener('input', (e) => filterPatients(e.target.value));
        }
        if (dom.clearSearchBtn) {
            dom.clearSearchBtn.addEventListener('click', () => {
                dom.searchInput.value = '';
                filterPatients('');
                dom.searchInput.focus();
            });
        }

        // Details Modal close events
        if (dom.detailsCloseBtn) {
            dom.detailsCloseBtn.addEventListener('click', closeDetailsModal);
        }
        if (dom.detailsHeaderCloseBtn) {
            dom.detailsHeaderCloseBtn.addEventListener('click', closeDetailsModal);
        }
        if (dom.detailsModal) {
            dom.detailsModal.addEventListener('click', (e) => {
                if (e.target === dom.detailsModal) closeDetailsModal();
            });
        }

        // Form Modal close events
        if (dom.formCancelBtn) {
            dom.formCancelBtn.addEventListener('click', closeFormModal);
        }
        if (dom.formHeaderCloseBtn) {
            dom.formHeaderCloseBtn.addEventListener('click', closeFormModal);
        }
        if (dom.formModal) {
            dom.formModal.addEventListener('click', (e) => {
                if (e.target === dom.formModal) closeFormModal();
            });
        }

        // Form submit
        if (dom.form) {
            dom.form.addEventListener('submit', handleFormSubmit);
        }

        // Keyboard accessibility: Escape closes open modal
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (dom.formModal && !dom.formModal.classList.contains('hidden')) {
                    closeFormModal();
                } else if (dom.detailsModal && !dom.detailsModal.classList.contains('hidden')) {
                    closeDetailsModal();
                }
            }
        });
    }

    /**
     * Initializes the view when navigating to Patients module.
     */
    function initView() {
        cacheDom();
        setupEvents();
        loadPatients();
    }

    window.EmergencyPatients = {
        initView,
        loadPatients,
        filterPatients
    };

})(window);
