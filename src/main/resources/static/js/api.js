/* ===== API Service Layer ===== */
const API_BASE = '/api';

function getToken() {
    return localStorage.getItem('finatrack_token');
}

function setToken(token) {
    localStorage.setItem('finatrack_token', token);
}

function clearToken() {
    localStorage.removeItem('finatrack_token');
    localStorage.removeItem('finatrack_user');
}

function getStoredUser() {
    const u = localStorage.getItem('finatrack_user');
    return u ? JSON.parse(u) : null;
}

function setStoredUser(user) {
    localStorage.setItem('finatrack_user', JSON.stringify(user));
}

async function apiRequest(endpoint, options = {}) {
    const url = API_BASE + endpoint;
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const config = {
        ...options,
        headers: { ...headers, ...options.headers }
    };

    const response = await fetch(url, config);

    if (response.status === 401) {
        clearToken();
        showPage('login');
        throw new Error('Sesi telah berakhir, silakan login kembali');
    }

    // For CSV export
    if (response.headers.get('content-type')?.includes('text/csv')) {
        return response;
    }

    const data = await response.json();

    if (!response.ok) {
        const msg = data.message || data.data?.toString() || 'Terjadi kesalahan';
        throw new Error(msg);
    }

    return data;
}

// ===== Auth API =====
async function apiRegister(fullName, email, password) {
    return apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ fullName, email, password })
    });
}

async function apiLogin(email, password) {
    return apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
    });
}

// ===== User API =====
async function apiGetProfile() {
    return apiRequest('/users/profile');
}

async function apiUpdateProfile(fullName) {
    return apiRequest('/users/profile', {
        method: 'PUT',
        body: JSON.stringify({ fullName })
    });
}

// ===== Category API =====
async function apiGetCategories() {
    return apiRequest('/categories');
}

async function apiCreateCategory(name, type) {
    return apiRequest('/categories', {
        method: 'POST',
        body: JSON.stringify({ name, type })
    });
}

async function apiUpdateCategory(id, name, type) {
    return apiRequest('/categories/' + id, {
        method: 'PUT',
        body: JSON.stringify({ name, type })
    });
}

async function apiDeleteCategory(id) {
    return apiRequest('/categories/' + id, {
        method: 'DELETE'
    });
}

// ===== Transaction API =====
async function apiGetTransactions(page, size, sortBy, sortDir, type, startDate, endDate) {
    let params = new URLSearchParams({ page, size, sortBy, sortDir });
    if (type) params.append('type', type);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    return apiRequest('/transactions?' + params.toString());
}

async function apiCreateTransaction(data) {
    return apiRequest('/transactions', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function apiUpdateTransaction(id, data) {
    return apiRequest('/transactions/' + id, {
        method: 'PUT',
        body: JSON.stringify(data)
    });
}

async function apiDeleteTransaction(id) {
    return apiRequest('/transactions/' + id, {
        method: 'DELETE'
    });
}

async function apiGetDashboard() {
    return apiRequest('/transactions/dashboard');
}

async function apiGetMonthlySummary(month, year) {
    return apiRequest('/transactions/summary?month=' + month + '&year=' + year);
}

// ===== Budget API =====
async function apiGetBudgets() {
    return apiRequest('/budgets');
}

async function apiCreateBudget(data) {
    return apiRequest('/budgets', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function apiUpdateBudget(id, data) {
    return apiRequest('/budgets/' + id, {
        method: 'PUT',
        body: JSON.stringify(data)
    });
}

async function apiDeleteBudget(id) {
    return apiRequest('/budgets/' + id, {
        method: 'DELETE'
    });
}

async function apiGetBudgetStatus(month, year) {
    return apiRequest('/budgets/status?month=' + month + '&year=' + year);
}

// ===== Savings Goal API =====
async function apiGetSavingsGoals() {
    return apiRequest('/savings-goals');
}

async function apiCreateSavingsGoal(data) {
    return apiRequest('/savings-goals', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function apiUpdateSavingsGoal(id, data) {
    return apiRequest('/savings-goals/' + id, {
        method: 'PUT',
        body: JSON.stringify(data)
    });
}

async function apiDeleteSavingsGoal(id) {
    return apiRequest('/savings-goals/' + id, {
        method: 'DELETE'
    });
}

async function apiContribute(id, amount) {
    return apiRequest('/savings-goals/' + id + '/contribute', {
        method: 'POST',
        body: JSON.stringify({ amount })
    });
}

// ===== Gamification API =====
async function apiGetGamification() {
    return apiRequest('/gamification/status');
}

// ===== Export API =====
async function apiExportTransactions() {
    const token = getToken();
    const response = await fetch(API_BASE + '/export/transactions', {
        headers: { 'Authorization': 'Bearer ' + token }
    });
    if (!response.ok) throw new Error('Gagal mengexport data');
    return response;
}
