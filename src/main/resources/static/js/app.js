/* ===== FinaTrack App Logic ===== */

// ===== State =====
let currentPage = 0;
let currentTxnType = 'EXPENSE';
let currentCatType = 'EXPENSE';
let allCategories = [];
let budgetMonth = new Date().getMonth() + 1;
let budgetYear = new Date().getFullYear();

const MONTHS = ['Januari','Februari','Maret','April','Mei','Juni',
    'Juli','Agustus','September','Oktober','November','Desember'];

const BADGE_ICONS = {
    'FIRST_TRANSACTION': 'fa-star',
    'WEEK_STREAK': 'fa-calendar-week',
    'MONTH_STREAK': 'fa-calendar-check',
    'BUDGET_MASTER': 'fa-shield-alt',
    'SAVINGS_STARTER': 'fa-seedling',
    'SAVINGS_ACHIEVER': 'fa-trophy',
    'CENTURY_TRANSACTIONS': 'fa-gem'
};

const CATEGORY_COLORS = [
    '#4A3AFF','#FF4757','#00C48C','#FFB800','#6C5CE7',
    '#FD79A8','#0984E3','#00B894','#E17055','#A29BFE'
];

// ===== Formatting =====
function formatRp(amount) {
    if (amount == null) return 'Rp 0';
    return 'Rp ' + Number(amount).toLocaleString('id-ID');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ===== Toast =====
function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type;
    setTimeout(() => { toast.className = 'toast hidden'; }, 3000);
}

// ===== Page / Section Navigation =====
function showPage(page) {
    document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
    document.getElementById('app-shell').classList.add('hidden');

    if (page === 'login' || page === 'register') {
        document.getElementById('page-' + page).classList.remove('hidden');
    } else {
        document.getElementById('app-shell').classList.remove('hidden');
        showSection(page);
    }
}

function showSection(section) {
    document.querySelectorAll('.section').forEach(s => s.classList.add('hidden'));
    document.getElementById('section-' + section).classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    const navBtn = document.querySelector('.nav-item[data-section="' + section + '"]');
    if (navBtn) navBtn.classList.add('active');

    // Load data for section
    switch (section) {
        case 'dashboard': loadDashboard(); break;
        case 'transactions': loadTransactions(); break;
        case 'categories': loadCategories(); break;
        case 'budgets': loadBudgetStatus(); break;
        case 'savings': loadSavingsGoals(); break;
        case 'gamification': loadGamification(); break;
        case 'profile': loadProfile(); break;
    }
}

// ===== Modal =====
function openModal(modalId) {
    document.getElementById('modal-overlay').classList.remove('hidden');
    document.getElementById(modalId).classList.remove('hidden');
}

function closeModal() {
    document.getElementById('modal-overlay').classList.add('hidden');
    document.querySelectorAll('.modal').forEach(m => m.classList.add('hidden'));
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    const token = getToken();
    if (token) {
        const user = getStoredUser();
        if (user) updateHeader(user);
        showPage('dashboard');
    } else {
        showPage('login');
    }

    // Form handlers
    document.getElementById('form-login').addEventListener('submit', handleLogin);
    document.getElementById('form-register').addEventListener('submit', handleRegister);
    document.getElementById('form-transaction').addEventListener('submit', handleTransactionSubmit);
    document.getElementById('form-category').addEventListener('submit', handleCategorySubmit);
    document.getElementById('form-budget').addEventListener('submit', handleBudgetSubmit);
    document.getElementById('form-savings').addEventListener('submit', handleSavingsSubmit);
    document.getElementById('form-contribute').addEventListener('submit', handleContributeSubmit);
    document.getElementById('form-update-profile').addEventListener('submit', handleProfileUpdate);
});

// ===== Auth Handlers =====
async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    try {
        const res = await apiLogin(email, password);
        setToken(res.data.token);
        setStoredUser({ fullName: res.data.fullName, email: res.data.email });
        updateHeader({ fullName: res.data.fullName, email: res.data.email });
        showToast('Login berhasil!', 'success');
        showPage('dashboard');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const confirm = document.getElementById('reg-confirm').value;

    if (password !== confirm) {
        showToast('Password tidak cocok', 'error');
        return;
    }
    try {
        const res = await apiRegister(name, email, password);
        setToken(res.data.token);
        setStoredUser({ fullName: res.data.fullName, email: res.data.email });
        updateHeader({ fullName: res.data.fullName, email: res.data.email });
        showToast('Registrasi berhasil!', 'success');
        showPage('dashboard');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function logout() {
    clearToken();
    showPage('login');
    showToast('Berhasil logout', 'success');
}

function updateHeader(user) {
    const initial = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';
    document.getElementById('header-avatar').textContent = initial;
    document.getElementById('header-name').textContent = user.fullName || 'User';
}

// ===== Dashboard =====
async function loadDashboard() {
    try {
        const [dashRes, txnRes] = await Promise.all([
            apiGetDashboard(),
            apiGetTransactions(0, 5, 'date', 'desc', '', '', '')
        ]);

        const dash = dashRes.data;
        document.getElementById('dash-balance').textContent = formatRp(dash.balance);
        document.getElementById('dash-income').textContent = formatRp(dash.totalIncome);
        document.getElementById('dash-expense').textContent = formatRp(dash.totalExpense);

        // Category breakdown
        const catDiv = document.getElementById('dash-categories');
        const noCat = document.getElementById('dash-no-categories');
        if (dash.expenseByCategory && dash.expenseByCategory.length > 0) {
            noCat.classList.add('hidden');
            catDiv.innerHTML = dash.expenseByCategory.map((c, i) =>
                '<div class="breakdown-item">' +
                    '<span class="breakdown-color" style="background:' + CATEGORY_COLORS[i % CATEGORY_COLORS.length] + '"></span>' +
                    '<span class="breakdown-name">' + escapeHtml(c.categoryName) + '</span>' +
                    '<span class="breakdown-amount">' + formatRp(c.totalAmount) + '</span>' +
                '</div>'
            ).join('');
        } else {
            noCat.classList.remove('hidden');
            catDiv.innerHTML = '';
        }

        // Recent Transactions
        const txnDiv = document.getElementById('dash-recent-transactions');
        const noTxn = document.getElementById('dash-no-transactions');
        const txns = txnRes.data.content;
        if (txns && txns.length > 0) {
            noTxn.classList.add('hidden');
            txnDiv.innerHTML = txns.map(renderTransactionItem).join('');
        } else {
            noTxn.classList.remove('hidden');
            txnDiv.innerHTML = '';
        }

        // Budget alerts
        loadBudgetAlerts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadBudgetAlerts() {
    try {
        const now = new Date();
        const res = await apiGetBudgetStatus(now.getMonth() + 1, now.getFullYear());
        const statuses = res.data;
        const alerts = statuses.filter(s => s.status === 'PERINGATAN' || s.status === 'MELEBIHI');
        const container = document.getElementById('dash-budget-alerts');
        const list = document.getElementById('budget-alerts-list');

        if (alerts.length > 0) {
            container.classList.remove('hidden');
            list.innerHTML = alerts.map(a => {
                const cls = a.status === 'MELEBIHI' ? 'exceeded' : 'warning';
                const icon = a.status === 'MELEBIHI' ? 'fa-exclamation-circle' : 'fa-exclamation-triangle';
                return '<div class="alert-item ' + cls + '">' +
                    '<i class="fas ' + icon + '"></i>' +
                    '<span>' + escapeHtml(a.categoryName) + ': ' + a.percentageUsed.toFixed(0) + '% (' + a.status + ')</span>' +
                '</div>';
            }).join('');
        } else {
            container.classList.add('hidden');
        }
    } catch (_) {
        // Silent fail - alerts are optional
    }
}

// ===== Transactions =====
async function loadTransactions() {
    const type = document.getElementById('filter-type').value;
    const startDate = document.getElementById('filter-start').value;
    const endDate = document.getElementById('filter-end').value;

    try {
        const res = await apiGetTransactions(currentPage, 10, 'date', 'desc', type, startDate, endDate);
        const paged = res.data;
        const list = document.getElementById('transactions-list');

        if (paged.content && paged.content.length > 0) {
            list.innerHTML = paged.content.map(t => renderTransactionItemFull(t)).join('');
        } else {
            list.innerHTML = '<p class="empty-state">Tidak ada transaksi</p>';
        }

        renderPagination(paged);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderTransactionItem(t) {
    const typeClass = t.type === 'INCOME' ? 'income' : 'expense';
    const icon = t.type === 'INCOME' ? 'fa-arrow-down' : 'fa-arrow-up';
    const sign = t.type === 'INCOME' ? '+' : '-';
    return '<div class="txn-item">' +
        '<div class="txn-icon ' + typeClass + '"><i class="fas ' + icon + '"></i></div>' +
        '<div class="txn-info">' +
            '<div class="txn-desc">' + escapeHtml(t.description || t.categoryName) + '</div>' +
            '<div class="txn-cat">' + escapeHtml(t.categoryName) + '</div>' +
        '</div>' +
        '<div class="txn-right">' +
            '<div class="txn-amount ' + typeClass + '">' + sign + formatRp(t.amount) + '</div>' +
            '<div class="txn-date">' + formatDate(t.date) + '</div>' +
        '</div>' +
    '</div>';
}

function renderTransactionItemFull(t) {
    const typeClass = t.type === 'INCOME' ? 'income' : 'expense';
    const icon = t.type === 'INCOME' ? 'fa-arrow-down' : 'fa-arrow-up';
    const sign = t.type === 'INCOME' ? '+' : '-';
    return '<div class="txn-item">' +
        '<div class="txn-icon ' + typeClass + '"><i class="fas ' + icon + '"></i></div>' +
        '<div class="txn-info">' +
            '<div class="txn-desc">' + escapeHtml(t.description || t.categoryName) + '</div>' +
            '<div class="txn-cat">' + escapeHtml(t.categoryName) + '</div>' +
        '</div>' +
        '<div class="txn-right">' +
            '<div class="txn-amount ' + typeClass + '">' + sign + formatRp(t.amount) + '</div>' +
            '<div class="txn-date">' + formatDate(t.date) + '</div>' +
        '</div>' +
        '<div class="txn-actions">' +
            '<button class="btn btn-xs btn-outline" onclick="editTransaction(' + t.id + ')"><i class="fas fa-pen"></i></button>' +
            '<button class="btn btn-xs btn-danger" onclick="deleteTransaction(' + t.id + ')"><i class="fas fa-trash"></i></button>' +
        '</div>' +
    '</div>';
}

function renderPagination(paged) {
    const div = document.getElementById('transactions-pagination');
    if (paged.totalPages <= 1) { div.innerHTML = ''; return; }
    let html = '<button ' + (currentPage === 0 ? 'disabled' : '') + ' onclick="goToPage(' + (currentPage - 1) + ')"><i class="fas fa-chevron-left"></i></button>';
    for (let i = 0; i < paged.totalPages; i++) {
        html += '<button class="' + (i === currentPage ? 'active' : '') + '" onclick="goToPage(' + i + ')">' + (i + 1) + '</button>';
    }
    html += '<button ' + (paged.last ? 'disabled' : '') + ' onclick="goToPage(' + (currentPage + 1) + ')"><i class="fas fa-chevron-right"></i></button>';
    div.innerHTML = html;
}

function goToPage(page) {
    currentPage = page;
    loadTransactions();
}

function setTxnType(type, btn) {
    currentTxnType = type;
    btn.parentElement.querySelectorAll('.toggle').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    loadCategoryOptions('txn-category', type);
}

async function openTransactionModal(editData) {
    document.getElementById('txn-edit-id').value = '';
    document.getElementById('txn-amount').value = '';
    document.getElementById('txn-description').value = '';
    document.getElementById('txn-date').value = new Date().toISOString().split('T')[0];
    currentTxnType = 'EXPENSE';

    const toggles = document.querySelectorAll('#form-transaction .toggle');
    toggles.forEach(t => t.classList.remove('active'));
    toggles[0].classList.add('active');

    document.getElementById('modal-transaction-title').textContent = 'Tambah Transaksi';

    await loadCategoryOptions('txn-category', 'EXPENSE');

    if (editData) {
        document.getElementById('modal-transaction-title').textContent = 'Edit Transaksi';
        document.getElementById('txn-edit-id').value = editData.id;
        document.getElementById('txn-amount').value = editData.amount;
        document.getElementById('txn-description').value = editData.description || '';
        document.getElementById('txn-date').value = editData.date;
        currentTxnType = editData.type;
        toggles.forEach(t => t.classList.remove('active'));
        toggles.forEach(t => { if (t.dataset.value === editData.type) t.classList.add('active'); });
        await loadCategoryOptions('txn-category', editData.type);
        document.getElementById('txn-category').value = editData.categoryId;
    }
    openModal('modal-transaction');
}

async function handleTransactionSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('txn-edit-id').value;
    const data = {
        amount: Number(document.getElementById('txn-amount').value),
        type: currentTxnType,
        description: document.getElementById('txn-description').value,
        date: document.getElementById('txn-date').value,
        categoryId: Number(document.getElementById('txn-category').value)
    };
    try {
        if (id) {
            await apiUpdateTransaction(id, data);
            showToast('Transaksi berhasil diperbarui', 'success');
        } else {
            await apiCreateTransaction(data);
            showToast('Transaksi berhasil ditambahkan', 'success');
        }
        closeModal();
        loadTransactions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function editTransaction(id) {
    try {
        const res = await apiRequest('/transactions/' + id);
        const t = res.data;
        await openTransactionModal(t);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteTransaction(id) {
    if (!confirm('Yakin ingin menghapus transaksi ini?')) return;
    try {
        await apiDeleteTransaction(id);
        showToast('Transaksi berhasil dihapus', 'success');
        loadTransactions();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Categories =====
let categoryFilter = 'all';

async function loadCategories() {
    try {
        const res = await apiGetCategories();
        allCategories = res.data;
        renderCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function filterCategories(filter, btn) {
    categoryFilter = filter;
    document.querySelectorAll('.tab-bar .tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    renderCategories();
}

function renderCategories() {
    const list = document.getElementById('categories-list');
    let filtered = allCategories;
    if (categoryFilter !== 'all') {
        filtered = allCategories.filter(c => c.type === categoryFilter);
    }
    if (filtered.length === 0) {
        list.innerHTML = '<p class="empty-state">Tidak ada kategori</p>';
        return;
    }
    list.innerHTML = filtered.map(c => {
        const typeClass = c.type === 'INCOME' ? 'income' : 'expense';
        const icon = c.type === 'INCOME' ? 'fa-arrow-down' : 'fa-arrow-up';
        const typeLabel = c.type === 'INCOME' ? 'Pemasukan' : 'Pengeluaran';
        const isSystem = !c.isCustom;
        const badge = isSystem ? '<span class="cat-badge system">Sistem</span>' : '<span class="cat-badge">Kustom</span>';
        const actions = isSystem ? '' :
            '<div class="cat-actions">' +
                '<button class="btn btn-xs btn-outline" onclick="editCategory(' + c.id + ')"><i class="fas fa-pen"></i></button>' +
                '<button class="btn btn-xs btn-danger" onclick="deleteCategory(' + c.id + ')"><i class="fas fa-trash"></i></button>' +
            '</div>';
        return '<div class="cat-item">' +
            '<div class="cat-icon ' + typeClass + '"><i class="fas ' + icon + '"></i></div>' +
            '<div class="cat-info">' +
                '<div class="cat-name">' + escapeHtml(c.name) + '</div>' +
                '<div class="cat-type">' + typeLabel + ' ' + badge + '</div>' +
            '</div>' +
            actions +
        '</div>';
    }).join('');
}

async function loadCategoryOptions(selectId, type) {
    try {
        const res = await apiGetCategories();
        allCategories = res.data;
        const sel = document.getElementById(selectId);
        const filtered = allCategories.filter(c => c.type === type);
        sel.innerHTML = '<option value="">Pilih kategori</option>' +
            filtered.map(c => '<option value="' + c.id + '">' + escapeHtml(c.name) + '</option>').join('');
    } catch (_) {
        // silent
    }
}

function setCatType(type, btn) {
    currentCatType = type;
    btn.parentElement.querySelectorAll('.toggle').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
}

function openCategoryModal(editData) {
    document.getElementById('cat-edit-id').value = '';
    document.getElementById('cat-name').value = '';
    currentCatType = 'EXPENSE';
    const toggles = document.querySelectorAll('#form-category .toggle');
    toggles.forEach(t => t.classList.remove('active'));
    toggles[0].classList.add('active');
    document.getElementById('modal-category-title').textContent = 'Tambah Kategori';

    if (editData) {
        document.getElementById('modal-category-title').textContent = 'Edit Kategori';
        document.getElementById('cat-edit-id').value = editData.id;
        document.getElementById('cat-name').value = editData.name;
        currentCatType = editData.type;
        toggles.forEach(t => t.classList.remove('active'));
        toggles.forEach(t => { if (t.dataset.value === editData.type) t.classList.add('active'); });
    }
    openModal('modal-category');
}

async function handleCategorySubmit(e) {
    e.preventDefault();
    const id = document.getElementById('cat-edit-id').value;
    const name = document.getElementById('cat-name').value;
    try {
        if (id) {
            await apiUpdateCategory(id, name, currentCatType);
            showToast('Kategori berhasil diperbarui', 'success');
        } else {
            await apiCreateCategory(name, currentCatType);
            showToast('Kategori berhasil dibuat', 'success');
        }
        closeModal();
        loadCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function editCategory(id) {
    const cat = allCategories.find(c => c.id === id);
    if (cat) openCategoryModal(cat);
}

async function deleteCategory(id) {
    if (!confirm('Yakin ingin menghapus kategori ini?')) return;
    try {
        await apiDeleteCategory(id);
        showToast('Kategori berhasil dihapus', 'success');
        loadCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Budgets =====
function changeBudgetMonth(delta) {
    budgetMonth += delta;
    if (budgetMonth > 12) { budgetMonth = 1; budgetYear++; }
    if (budgetMonth < 1) { budgetMonth = 12; budgetYear--; }
    loadBudgetStatus();
}

async function loadBudgetStatus() {
    document.getElementById('budget-month-label').textContent = MONTHS[budgetMonth - 1] + ' ' + budgetYear;
    try {
        const res = await apiGetBudgetStatus(budgetMonth, budgetYear);
        const list = document.getElementById('budgets-list');
        const empty = document.getElementById('budgets-empty');
        const statuses = res.data;

        if (statuses && statuses.length > 0) {
            empty.classList.add('hidden');
            list.innerHTML = statuses.map(b => {
                const pct = Math.min(b.percentageUsed, 100);
                return '<div class="budget-item">' +
                    '<div class="budget-top">' +
                        '<span class="budget-cat">' + escapeHtml(b.categoryName) + '</span>' +
                        '<span class="budget-status ' + b.status + '">' + b.status + '</span>' +
                    '</div>' +
                    '<div class="budget-progress"><div class="budget-progress-bar ' + b.status + '" style="width:' + pct + '%"></div></div>' +
                    '<div class="budget-detail">' +
                        '<span>' + formatRp(b.amountSpent) + ' / ' + formatRp(b.monthlyLimit) + '</span>' +
                        '<span>' + b.percentageUsed.toFixed(1) + '%</span>' +
                    '</div>' +
                    '<div class="budget-actions">' +
                        '<button class="btn btn-xs btn-outline" onclick="editBudget(' + b.budgetId + ')"><i class="fas fa-pen"></i></button>' +
                        '<button class="btn btn-xs btn-danger" onclick="deleteBudget(' + b.budgetId + ')"><i class="fas fa-trash"></i></button>' +
                    '</div>' +
                '</div>';
            }).join('');
        } else {
            empty.classList.remove('hidden');
            list.innerHTML = '';
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function openBudgetModal(editData) {
    document.getElementById('bgt-edit-id').value = '';
    document.getElementById('bgt-limit').value = '';
    document.getElementById('bgt-month').value = budgetMonth;
    document.getElementById('bgt-year').value = budgetYear;
    document.getElementById('modal-budget-title').textContent = 'Tambah Anggaran';

    await loadCategoryOptions('bgt-category', 'EXPENSE');

    if (editData) {
        document.getElementById('modal-budget-title').textContent = 'Edit Anggaran';
        document.getElementById('bgt-edit-id').value = editData.id;
        document.getElementById('bgt-limit').value = editData.monthlyLimit;
        document.getElementById('bgt-month').value = editData.month;
        document.getElementById('bgt-year').value = editData.year;
        document.getElementById('bgt-category').value = editData.categoryId;
    }
    openModal('modal-budget');
}

async function handleBudgetSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('bgt-edit-id').value;
    const data = {
        categoryId: Number(document.getElementById('bgt-category').value),
        monthlyLimit: Number(document.getElementById('bgt-limit').value),
        month: Number(document.getElementById('bgt-month').value),
        year: Number(document.getElementById('bgt-year').value)
    };
    try {
        if (id) {
            await apiUpdateBudget(id, data);
            showToast('Anggaran berhasil diperbarui', 'success');
        } else {
            await apiCreateBudget(data);
            showToast('Anggaran berhasil dibuat', 'success');
        }
        closeModal();
        loadBudgetStatus();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function editBudget(id) {
    try {
        const res = await apiGetBudgets();
        const budget = res.data.find(b => b.id === id);
        if (budget) await openBudgetModal(budget);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteBudget(id) {
    if (!confirm('Yakin ingin menghapus anggaran ini?')) return;
    try {
        await apiDeleteBudget(id);
        showToast('Anggaran berhasil dihapus', 'success');
        loadBudgetStatus();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Savings Goals =====
async function loadSavingsGoals() {
    try {
        const res = await apiGetSavingsGoals();
        const list = document.getElementById('savings-list');
        const empty = document.getElementById('savings-empty');
        const goals = res.data;

        if (goals && goals.length > 0) {
            empty.classList.add('hidden');
            list.innerHTML = goals.map(g => {
                const pct = g.progressPercentage || 0;
                const achieved = pct >= 100;
                return '<div class="savings-item">' +
                    '<div class="savings-top">' +
                        '<span class="savings-name">' + escapeHtml(g.name) + '</span>' +
                        '<span class="savings-pct' + (achieved ? ' achieved' : '') + '">' +
                            (achieved ? '<i class="fas fa-check-circle"></i> ' : '') +
                            pct.toFixed(1) + '%' +
                        '</span>' +
                    '</div>' +
                    '<div class="savings-progress"><div class="savings-progress-bar' + (achieved ? ' achieved' : '') + '" style="width:' + Math.min(pct, 100) + '%"></div></div>' +
                    '<div class="savings-detail">' +
                        '<span>' + formatRp(g.currentAmount) + ' / ' + formatRp(g.targetAmount) + '</span>' +
                        '<span>Target: ' + formatDate(g.targetDate) + '</span>' +
                    '</div>' +
                    '<div class="savings-actions">' +
                        (!achieved ? '<button class="btn btn-xs btn-primary" onclick="openContributeModal(' + g.id + ')"><i class="fas fa-plus"></i> Kontribusi</button>' : '') +
                        '<button class="btn btn-xs btn-outline" onclick="editSavingsGoal(' + g.id + ')"><i class="fas fa-pen"></i></button>' +
                        '<button class="btn btn-xs btn-danger" onclick="deleteSavingsGoal(' + g.id + ')"><i class="fas fa-trash"></i></button>' +
                    '</div>' +
                '</div>';
            }).join('');
        } else {
            empty.classList.remove('hidden');
            list.innerHTML = '';
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openSavingsModal(editData) {
    document.getElementById('sav-edit-id').value = '';
    document.getElementById('sav-name').value = '';
    document.getElementById('sav-target').value = '';
    document.getElementById('sav-date').value = '';
    document.getElementById('modal-savings-title').textContent = 'Tambah Target Tabungan';

    if (editData) {
        document.getElementById('modal-savings-title').textContent = 'Edit Target Tabungan';
        document.getElementById('sav-edit-id').value = editData.id;
        document.getElementById('sav-name').value = editData.name;
        document.getElementById('sav-target').value = editData.targetAmount;
        document.getElementById('sav-date').value = editData.targetDate;
    }
    openModal('modal-savings');
}

async function handleSavingsSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('sav-edit-id').value;
    const data = {
        name: document.getElementById('sav-name').value,
        targetAmount: Number(document.getElementById('sav-target').value),
        targetDate: document.getElementById('sav-date').value
    };
    try {
        if (id) {
            await apiUpdateSavingsGoal(id, data);
            showToast('Target tabungan berhasil diperbarui', 'success');
        } else {
            await apiCreateSavingsGoal(data);
            showToast('Target tabungan berhasil dibuat', 'success');
        }
        closeModal();
        loadSavingsGoals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function editSavingsGoal(id) {
    try {
        const res = await apiRequest('/savings-goals/' + id);
        openSavingsModal(res.data);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteSavingsGoal(id) {
    if (!confirm('Yakin ingin menghapus target tabungan ini?')) return;
    try {
        await apiDeleteSavingsGoal(id);
        showToast('Target tabungan berhasil dihapus', 'success');
        loadSavingsGoals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openContributeModal(id) {
    document.getElementById('contrib-id').value = id;
    document.getElementById('contrib-amount').value = '';
    openModal('modal-contribute');
}

async function handleContributeSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('contrib-id').value;
    const amount = Number(document.getElementById('contrib-amount').value);
    try {
        await apiContribute(id, amount);
        showToast('Kontribusi berhasil ditambahkan', 'success');
        closeModal();
        loadSavingsGoals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Gamification =====
async function loadGamification() {
    try {
        const res = await apiGetGamification();
        const gam = res.data;
        document.getElementById('gam-current-streak').textContent = gam.currentStreak + ' hari';
        document.getElementById('gam-longest-streak').textContent = gam.longestStreak + ' hari';

        const allBadgeTypes = [
            { type: 'FIRST_TRANSACTION', name: 'Transaksi Pertama', desc: 'Mencatat transaksi pertama' },
            { type: 'WEEK_STREAK', name: 'Seminggu Konsisten', desc: 'Streak 7 hari berturut-turut' },
            { type: 'MONTH_STREAK', name: 'Sebulan Konsisten', desc: 'Streak 30 hari berturut-turut' },
            { type: 'BUDGET_MASTER', name: 'Ahli Anggaran', desc: 'Tidak melebihi anggaran 3 bulan' },
            { type: 'SAVINGS_STARTER', name: 'Penabung Pemula', desc: 'Membuat target tabungan pertama' },
            { type: 'SAVINGS_ACHIEVER', name: 'Target Tercapai', desc: 'Mencapai target tabungan' },
            { type: 'CENTURY_TRANSACTIONS', name: 'Transaksi Seabad', desc: '100 total transaksi' }
        ];

        const earnedMap = {};
        if (gam.badges) {
            gam.badges.forEach(b => { earnedMap[b.name] = b; });
        }

        document.getElementById('badges-list').innerHTML = allBadgeTypes.map(bt => {
            const earned = earnedMap[bt.type];
            const cls = earned ? 'earned' : '';
            const icon = BADGE_ICONS[bt.type] || 'fa-medal';
            const dateStr = earned ? '<div class="badge-date">Diperoleh ' + formatDate(earned.awardedAt) + '</div>' : '';
            return '<div class="badge-item ' + cls + '">' +
                '<div class="badge-icon"><i class="fas ' + icon + '"></i></div>' +
                '<div class="badge-name">' + bt.name + '</div>' +
                '<div class="badge-desc">' + bt.desc + '</div>' +
                dateStr +
            '</div>';
        }).join('');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Profile =====
async function loadProfile() {
    try {
        const res = await apiGetProfile();
        const user = res.data;
        const initial = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';
        document.getElementById('profile-avatar').textContent = initial;
        document.getElementById('profile-name').textContent = user.fullName;
        document.getElementById('profile-email').textContent = user.email;
        document.getElementById('profile-edit-name').value = user.fullName;
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleProfileUpdate(e) {
    e.preventDefault();
    const name = document.getElementById('profile-edit-name').value;
    try {
        const res = await apiUpdateProfile(name);
        setStoredUser({ fullName: res.data.fullName, email: res.data.email });
        updateHeader({ fullName: res.data.fullName });
        showToast('Profil berhasil diperbarui', 'success');
        loadProfile();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ===== Export =====
async function exportTransactions() {
    try {
        const response = await apiExportTransactions();
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'transaksi_finatrack.csv';
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
        showToast('File CSV berhasil diunduh', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}
