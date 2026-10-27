/**
 * Connected Ambulance System - Reports & Analytics Module
 * Aggregates real-time operational statistics and KPIs from verified Spring Boot REST endpoints:
 *   GET /api/cases (Emergency cases status, priority, and volume)
 *   GET /api/ambulances (Ambulance fleet availability and readiness)
 *   GET /api/dispatches (Dispatch outcomes and response time analytics for Admin/Dispatcher)
 * Transparently discloses backend API boundaries and Desktop ReportService parity.
 */
(function(window) {
    'use strict';

    // Module State
    let casesData = [];
    let ambulancesData = [];
    let dispatchesData = [];
    let canAccessDispatches = false;
    let currentRole = '';
    let isInitialized = false;

    // DOM Cache
    let dom = {};

    function cacheDom() {
        dom = {
            view: document.getElementById('viewReports'),
            refreshBtn: document.getElementById('refreshReportsBtn'),
            noticeBanner: document.getElementById('reportsNoticeBanner'),
            driverScopeNotice: document.getElementById('reportDriverScopeNotice'),

            // States
            loadingState: document.getElementById('reportLoadingState'),
            errorState: document.getElementById('reportErrorState'),
            errorMessage: document.getElementById('reportErrorMessage'),
            retryBtn: document.getElementById('retryReportsBtn'),
            restrictedState: document.getElementById('reportRestrictedState'),
            contentArea: document.getElementById('reportContentArea'),

            // KPI Grid
            kpiGrid: document.getElementById('reportKpiGrid'),

            // Cases Card
            casesCard: document.getElementById('reportCasesCard'),
            casesCardSubtitle: document.querySelector('#reportCasesCard .report-card-subtitle'),
            casesTotalBadge: document.getElementById('reportCasesTotalBadge'),
            casesPriorityList: document.getElementById('reportCasesPriorityList'),
            casesStatusBody: document.getElementById('reportCasesStatusBody'),

            // Ambulances Card
            ambulancesCard: document.getElementById('reportAmbulancesCard'),
            ambulancesCardSubtitle: document.querySelector('#reportAmbulancesCard .report-card-subtitle'),
            ambulancesTotalBadge: document.getElementById('reportAmbulancesTotalBadge'),
            ambulanceBarList: document.getElementById('reportAmbulanceBarList'),
            ambulanceStatusBody: document.getElementById('reportAmbulanceStatusBody'),

            // Dispatches Card
            dispatchesCard: document.getElementById('reportDispatchesCard'),
            dispatchesTotalBadge: document.getElementById('reportDispatchesTotalBadge'),
            dispatchesRestrictedNotice: document.getElementById('reportDispatchesRestrictedNotice'),
            dispatchesBodyContainer: document.getElementById('reportDispatchesBodyContainer'),
            dispatchBarList: document.getElementById('reportDispatchBarList'),
            dispatchStatusBody: document.getElementById('reportDispatchStatusBody'),

            // Desktop Parity Card
            gapsCard: document.getElementById('reportGapsCard')
        };
    }

    /**
     * Determines role capabilities for reports access.
     */
    function getRolePermissions() {
        const user = window.EmergencyAuth ? window.EmergencyAuth.getCurrentUser() : null;
        currentRole = user ? user.role : '';

        const canView = ['ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR', 'AMBULANCE_DRIVER'].includes(currentRole);
        const canViewDispatches = ['ADMIN', 'DISPATCHER'].includes(currentRole);

        return {
            canView,
            canViewDispatches,
            role: currentRole
        };
    }

    /**
     * Loads live operational records from verified REST endpoints.
     */
    async function loadReports() {
        const perms = getRolePermissions();

        if (!perms.canView) {
            if (dom.loadingState) dom.loadingState.classList.add('hidden');
            if (dom.errorState) dom.errorState.classList.add('hidden');
            if (dom.contentArea) dom.contentArea.classList.add('hidden');
            if (dom.restrictedState) dom.restrictedState.classList.remove('hidden');
            return;
        }

        if (dom.restrictedState) dom.restrictedState.classList.add('hidden');
        if (dom.errorState) dom.errorState.classList.add('hidden');
        if (dom.contentArea) dom.contentArea.classList.add('hidden');
        if (dom.loadingState) dom.loadingState.classList.remove('hidden');

        try {
            // 1. Mandatory Core operational endpoints: Cases and Ambulances
            // Failures must NOT be silently swallowed into empty arrays.
            const casesPromise = window.EmergencyApi.apiFetch('/api/cases', { method: 'GET' });
            const ambPromise = window.EmergencyApi.apiFetch('/api/ambulances', { method: 'GET' });

            // 2. Dispatch metrics: requested for Admin/Dispatcher roles
            let dispatchesPromise;
            if (perms.canViewDispatches) {
                dispatchesPromise = window.EmergencyApi.apiFetch('/api/dispatches', { method: 'GET' })
                    .catch(err => {
                        // HTTP 403 Forbidden indicates role restriction rather than server failure
                        if (err && err.status === 403) {
                            console.warn('Reports: /api/dispatches is restricted (403)', err);
                            return { isForbidden: true, data: [] };
                        }
                        // Network errors (status 0), server 500s, etc. must propagate to trigger error state
                        throw err;
                    });
            } else {
                dispatchesPromise = Promise.resolve({ isForbidden: true, data: [] });
            }

            const [casesRes, ambRes, dispatchesRes] = await Promise.all([casesPromise, ambPromise, dispatchesPromise]);

            // Validate that required operational datasets were successfully returned
            if (!casesRes || !Array.isArray(casesRes.data)) {
                throw new Error('Malformed emergency cases response received from server.');
            }
            if (!ambRes || !Array.isArray(ambRes.data)) {
                throw new Error('Malformed ambulance fleet response received from server.');
            }

            casesData = casesRes.data;
            ambulancesData = ambRes.data;

            if (dispatchesRes && !dispatchesRes.isForbidden && Array.isArray(dispatchesRes.data)) {
                dispatchesData = dispatchesRes.data;
                canAccessDispatches = true;
            } else {
                dispatchesData = [];
                canAccessDispatches = false;
            }

            if (dom.loadingState) dom.loadingState.classList.add('hidden');
            if (dom.contentArea) dom.contentArea.classList.remove('hidden');

            renderReportKpis();
            renderCasesAnalytics();
            renderAmbulanceAnalytics();
            renderDispatchAnalytics();

        } catch (err) {
            console.error('Reports: Failed to load analytics data', err);
            if (dom.loadingState) dom.loadingState.classList.add('hidden');
            if (dom.contentArea) dom.contentArea.classList.add('hidden');
            if (dom.errorState) {
                dom.errorState.classList.remove('hidden');
                if (dom.errorMessage) {
                    dom.errorMessage.textContent = err.message || 'Failed to aggregate report metrics. Please verify network connectivity or contact administrator.';
                }
            }
        }
    }

    /**
     * Renders executive KPI summary cards.
     */
    function renderReportKpis() {
        if (!dom.kpiGrid) return;
        dom.kpiGrid.innerHTML = '';

        const isDriver = currentRole === 'AMBULANCE_DRIVER';

        // Toggle Driver Scope Notice Banner
        if (dom.driverScopeNotice) {
            if (isDriver) {
                dom.driverScopeNotice.classList.remove('hidden');
            } else {
                dom.driverScopeNotice.classList.add('hidden');
            }
        }

        const totalCases = casesData.length;
        const criticalCount = casesData.filter(c => (c.priorityLevel || '').toUpperCase() === 'CRITICAL').length;

        // Case Completion Rate calculation:
        // Formula: (completedCount / totalCases) * 100
        // Denominator: total registered cases currently in view (including active, completed, and cancelled cases).
        // Terminal State: caseStatus === 'COMPLETED'
        const completedCount = casesData.filter(c => (c.caseStatus || '').toUpperCase() === 'COMPLETED').length;
        const completionRate = totalCases > 0 ? ((completedCount / totalCases) * 100).toFixed(1) : '0.0';

        const totalAmbulances = ambulancesData.length;
        const availableAmbCount = ambulancesData.filter(a => (a.availability || '').toUpperCase() === 'AVAILABLE').length;
        const readinessRate = totalAmbulances > 0 ? Math.round((availableAmbCount / totalAmbulances) * 100) : 0;

        let activeDispatchesCount = 0;
        let avgEta = 0;
        if (canAccessDispatches && dispatchesData.length > 0) {
            const activeDispatches = dispatchesData.filter(d => ['DISPATCHED', 'EN_ROUTE', 'ARRIVED'].includes((d.dispatchStatus || '').toUpperCase()));
            activeDispatchesCount = activeDispatches.length;

            const dispatchesWithEta = dispatchesData.filter(d => typeof d.estimatedArrivalMinutes === 'number' && d.estimatedArrivalMinutes >= 0);
            if (dispatchesWithEta.length > 0) {
                const totalEta = dispatchesWithEta.reduce((acc, cur) => acc + cur.estimatedArrivalMinutes, 0);
                avgEta = totalEta / dispatchesWithEta.length;
            }
        }

        const kpis = [
            {
                title: isDriver ? 'Assigned Incidents' : 'Total Incidents',
                icon: '🚨',
                value: totalCases,
                subtext: isDriver
                    ? `${criticalCount} marked CRITICAL triage (Assigned)`
                    : `${criticalCount} marked CRITICAL triage`
            },
            {
                title: isDriver ? 'Assigned Vehicle Status' : 'Fleet Readiness',
                icon: '🚑',
                value: isDriver
                    ? (totalAmbulances > 0 ? (availableAmbCount > 0 ? 'AVAILABLE' : (ambulancesData[0].availability || 'DISPATCHED')) : 'NO UNIT')
                    : `${readinessRate}%`,
                subtext: isDriver
                    ? `${availableAmbCount} of ${totalAmbulances} assigned vehicle(s) ready`
                    : `${availableAmbCount} of ${totalAmbulances} units available`
            },
            {
                title: isDriver ? 'Active Mission' : 'Active Missions',
                icon: '📡',
                value: canAccessDispatches ? activeDispatchesCount : 'Restricted',
                subtext: canAccessDispatches ? `Avg Planned Dispatch ETA: ${avgEta.toFixed(1)}m` : 'Dispatcher / Admin scope'
            },
            {
                title: 'Case Completion Rate',
                icon: '✅',
                value: `${completionRate}%`,
                subtext: isDriver
                    ? `${completedCount} of ${totalCases} assigned cases completed`
                    : `${completedCount} of ${totalCases} registered cases completed`
            }
        ];

        kpis.forEach(kpi => {
            const card = document.createElement('div');
            card.className = 'report-kpi-card';

            const header = document.createElement('div');
            header.className = 'report-kpi-header';

            const title = document.createElement('span');
            title.className = 'report-kpi-title';
            title.textContent = kpi.title;

            const icon = document.createElement('span');
            icon.className = 'report-kpi-icon';
            icon.textContent = kpi.icon;

            header.appendChild(title);
            header.appendChild(icon);
            card.appendChild(header);

            const value = document.createElement('div');
            value.className = 'report-kpi-value';
            value.textContent = kpi.value;
            card.appendChild(value);

            const sub = document.createElement('div');
            sub.className = 'report-kpi-subtext';
            sub.textContent = kpi.subtext;
            card.appendChild(sub);

            dom.kpiGrid.appendChild(card);
        });
    }

    /**
     * Renders Emergency Cases analytics (Priority breakdown & Lifecycle status).
     */
    function renderCasesAnalytics() {
        const total = casesData.length;
        const isDriver = currentRole === 'AMBULANCE_DRIVER';

        if (dom.casesTotalBadge) {
            dom.casesTotalBadge.textContent = isDriver
                ? (total === 1 ? '1 assigned case' : `${total} assigned cases`)
                : (total === 1 ? '1 case' : `${total} cases`);
        }

        if (dom.casesCardSubtitle) {
            dom.casesCardSubtitle.textContent = isDriver
                ? 'Distribution of your assigned emergency cases'
                : 'Distribution by priority triage and lifecycle status';
        }

        // 1. Priority Bars
        if (dom.casesPriorityList) {
            dom.casesPriorityList.innerHTML = '';
            const priorities = [
                { key: 'CRITICAL', label: 'CRITICAL Priority', color: 'var(--red-500)' },
                { key: 'HIGH', label: 'HIGH Priority', color: 'var(--red-600)' },
                { key: 'MODERATE', label: 'MODERATE Priority', color: 'var(--navy-600)' },
                { key: 'LOW', label: 'LOW Priority', color: 'var(--text-muted)' }
            ];

            priorities.forEach(p => {
                const count = casesData.filter(c => (c.priorityLevel || '').toUpperCase() === p.key).length;
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;

                const item = document.createElement('div');
                item.className = 'report-bar-item';

                const labels = document.createElement('div');
                labels.className = 'report-bar-labels';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = p.label;

                const valSpan = document.createElement('span');
                valSpan.textContent = `${count} (${pct}%)`;

                labels.appendChild(nameSpan);
                labels.appendChild(valSpan);
                item.appendChild(labels);

                const track = document.createElement('div');
                track.className = 'report-bar-track';

                const fill = document.createElement('div');
                fill.className = 'report-bar-fill';
                fill.style.backgroundColor = p.color;
                fill.style.width = `${pct}%`;
                track.appendChild(fill);

                item.appendChild(track);
                dom.casesPriorityList.appendChild(item);
            });
        }

        // 2. Status Breakdown Table
        if (dom.casesStatusBody) {
            dom.casesStatusBody.innerHTML = '';
            const statuses = ['REGISTERED', 'DISPATCHED', 'IN_TRANSIT', 'ARRIVED', 'COMPLETED', 'CANCELLED'];

            statuses.forEach(s => {
                const count = casesData.filter(c => (c.caseStatus || '').toUpperCase() === s).length;
                const pct = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';

                const tr = document.createElement('tr');

                const tdStatus = document.createElement('td');
                const badge = document.createElement('span');
                badge.className = 'badge';
                badge.textContent = s;
                if (s === 'REGISTERED') badge.classList.add('badge-status-registered');
                else if (s === 'DISPATCHED') badge.classList.add('badge-status-dispatched');
                else if (s === 'IN_TRANSIT') badge.classList.add('badge-status-intransit');
                else if (s === 'ARRIVED') badge.classList.add('badge-status-arrived');
                else if (s === 'COMPLETED') badge.classList.add('badge-status-completed');
                else if (s === 'CANCELLED') badge.classList.add('badge-status-cancelled');
                tdStatus.appendChild(badge);
                tr.appendChild(tdStatus);

                const tdCount = document.createElement('td');
                tdCount.style.textAlign = 'right';
                tdCount.style.fontWeight = '600';
                tdCount.textContent = count;
                tr.appendChild(tdCount);

                const tdPct = document.createElement('td');
                tdPct.style.textAlign = 'right';
                tdPct.style.color = 'var(--text-muted)';
                tdPct.textContent = `${pct}%`;
                tr.appendChild(tdPct);

                dom.casesStatusBody.appendChild(tr);
            });
        }
    }

    /**
     * Renders Ambulance fleet availability metrics.
     */
    function renderAmbulanceAnalytics() {
        const total = ambulancesData.length;
        const isDriver = currentRole === 'AMBULANCE_DRIVER';

        if (dom.ambulancesTotalBadge) {
            dom.ambulancesTotalBadge.textContent = isDriver
                ? (total === 1 ? '1 assigned vehicle' : `${total} assigned vehicles`)
                : (total === 1 ? '1 vehicle' : `${total} vehicles`);
        }

        if (dom.ambulancesCardSubtitle) {
            dom.ambulancesCardSubtitle.textContent = isDriver
                ? 'Operational status of your assigned vehicle'
                : 'Operational fleet distribution and ready units';
        }

        // Availability Bars
        if (dom.ambulanceBarList) {
            dom.ambulanceBarList.innerHTML = '';
            const availabilities = [
                { key: 'AVAILABLE', label: 'AVAILABLE (Ready for Dispatch)', color: '#16a34a' },
                { key: 'DISPATCHED', label: 'DISPATCHED (On Active Mission)', color: '#2563eb' },
                { key: 'MAINTENANCE', label: 'MAINTENANCE (Out of Service)', color: '#ca8a04' }
            ];

            availabilities.forEach(a => {
                const count = ambulancesData.filter(amb => (amb.availability || '').toUpperCase() === a.key).length;
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;

                const item = document.createElement('div');
                item.className = 'report-bar-item';

                const labels = document.createElement('div');
                labels.className = 'report-bar-labels';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = a.label;

                const valSpan = document.createElement('span');
                valSpan.textContent = `${count} (${pct}%)`;

                labels.appendChild(nameSpan);
                labels.appendChild(valSpan);
                item.appendChild(labels);

                const track = document.createElement('div');
                track.className = 'report-bar-track';

                const fill = document.createElement('div');
                fill.className = 'report-bar-fill';
                fill.style.backgroundColor = a.color;
                fill.style.width = `${pct}%`;
                track.appendChild(fill);

                item.appendChild(track);
                dom.ambulanceBarList.appendChild(item);
            });
        }

        // Table
        if (dom.ambulanceStatusBody) {
            dom.ambulanceStatusBody.innerHTML = '';
            const keys = ['AVAILABLE', 'DISPATCHED', 'MAINTENANCE'];

            keys.forEach(k => {
                const count = ambulancesData.filter(amb => (amb.availability || '').toUpperCase() === k).length;
                const pct = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';

                const tr = document.createElement('tr');

                const tdStatus = document.createElement('td');
                const badge = document.createElement('span');
                badge.className = 'badge';
                badge.textContent = k;
                if (k === 'AVAILABLE') badge.classList.add('badge-availability-available');
                else if (k === 'DISPATCHED') badge.classList.add('badge-availability-dispatched');
                else if (k === 'MAINTENANCE') badge.classList.add('badge-availability-maintenance');
                tdStatus.appendChild(badge);
                tr.appendChild(tdStatus);

                const tdCount = document.createElement('td');
                tdCount.style.textAlign = 'right';
                tdCount.style.fontWeight = '600';
                tdCount.textContent = count;
                tr.appendChild(tdCount);

                const tdPct = document.createElement('td');
                tdPct.style.textAlign = 'right';
                tdPct.style.color = 'var(--text-muted)';
                tdPct.textContent = `${pct}%`;
                tr.appendChild(tdPct);

                dom.ambulanceStatusBody.appendChild(tr);
            });
        }
    }

    /**
     * Renders Dispatch mission performance and response times.
     */
    function renderDispatchAnalytics() {
        if (!canAccessDispatches) {
            if (dom.dispatchesRestrictedNotice) dom.dispatchesRestrictedNotice.classList.remove('hidden');
            if (dom.dispatchesBodyContainer) dom.dispatchesBodyContainer.classList.add('hidden');
            if (dom.dispatchesTotalBadge) dom.dispatchesTotalBadge.textContent = 'Restricted';
            return;
        }

        if (dom.dispatchesRestrictedNotice) dom.dispatchesRestrictedNotice.classList.add('hidden');
        if (dom.dispatchesBodyContainer) dom.dispatchesBodyContainer.classList.remove('hidden');

        const total = dispatchesData.length;
        if (dom.dispatchesTotalBadge) {
            dom.dispatchesTotalBadge.textContent = total === 1 ? '1 dispatch' : `${total} dispatches`;
        }

        // Dispatch Outcome Bars
        if (dom.dispatchBarList) {
            dom.dispatchBarList.innerHTML = '';
            const outcomes = [
                { key: 'COMPLETED', label: 'COMPLETED Missions', color: '#16a34a' },
                { key: 'ARRIVED', label: 'ARRIVED at Incident', color: '#b45309' },
                { key: 'EN_ROUTE', label: 'EN ROUTE to Scene', color: '#3730a3' },
                { key: 'DISPATCHED', label: 'DISPATCHED (Awaiting Departure)', color: '#2563eb' },
                { key: 'CANCELLED', label: 'CANCELLED Dispatches', color: '#64748b' }
            ];

            outcomes.forEach(o => {
                const count = dispatchesData.filter(d => (d.dispatchStatus || '').toUpperCase() === o.key).length;
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;

                const item = document.createElement('div');
                item.className = 'report-bar-item';

                const labels = document.createElement('div');
                labels.className = 'report-bar-labels';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = o.label;

                const valSpan = document.createElement('span');
                valSpan.textContent = `${count} (${pct}%)`;

                labels.appendChild(nameSpan);
                labels.appendChild(valSpan);
                item.appendChild(labels);

                const track = document.createElement('div');
                track.className = 'report-bar-track';

                const fill = document.createElement('div');
                fill.className = 'report-bar-fill';
                fill.style.backgroundColor = o.color;
                fill.style.width = `${pct}%`;
                track.appendChild(fill);

                item.appendChild(track);
                dom.dispatchBarList.appendChild(item);
            });
        }

        // Table
        if (dom.dispatchStatusBody) {
            dom.dispatchStatusBody.innerHTML = '';
            const statuses = ['COMPLETED', 'ARRIVED', 'EN_ROUTE', 'DISPATCHED', 'CANCELLED'];

            statuses.forEach(s => {
                const count = dispatchesData.filter(d => (d.dispatchStatus || '').toUpperCase() === s).length;
                const pct = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';

                const tr = document.createElement('tr');

                const tdStatus = document.createElement('td');
                const badge = document.createElement('span');
                badge.className = 'badge';
                badge.textContent = s;
                if (s === 'DISPATCHED') badge.classList.add('badge-dispatch-dispatched');
                else if (s === 'EN_ROUTE') badge.classList.add('badge-dispatch-enroute');
                else if (s === 'ARRIVED') badge.classList.add('badge-dispatch-arrived');
                else if (s === 'COMPLETED') badge.classList.add('badge-dispatch-completed');
                else if (s === 'CANCELLED') badge.classList.add('badge-dispatch-cancelled');
                tdStatus.appendChild(badge);
                tr.appendChild(tdStatus);

                const tdCount = document.createElement('td');
                tdCount.style.textAlign = 'right';
                tdCount.style.fontWeight = '600';
                tdCount.textContent = count;
                tr.appendChild(tdCount);

                const tdPct = document.createElement('td');
                tdPct.style.textAlign = 'right';
                tdPct.style.color = 'var(--text-muted)';
                tdPct.textContent = `${pct}%`;
                tr.appendChild(tdPct);

                dom.dispatchStatusBody.appendChild(tr);
            });
        }
    }

    /**
     * Attaches event listeners once.
     */
    function setupEvents() {
        if (isInitialized) return;
        isInitialized = true;

        if (dom.refreshBtn) dom.refreshBtn.addEventListener('click', loadReports);
        if (dom.retryBtn) dom.retryBtn.addEventListener('click', loadReports);
    }

    /**
     * Initializes the view when navigating to Reports & Analytics.
     */
    function initView() {
        cacheDom();
        setupEvents();
        loadReports();
    }

    // Public API on window
    window.EmergencyReports = {
        initView,
        loadReports,
        renderReportKpis,
        renderCasesAnalytics,
        renderAmbulanceAnalytics,
        renderDispatchAnalytics
    };

})(window);
