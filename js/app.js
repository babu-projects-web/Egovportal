// ===== CONFIG =====
const API_BASE = 'http://localhost:8080/api';

// ===== MOBILE MENU =====
const mobileMenuBtn = document.getElementById('mobileMenuBtn');
const mobileMenu = document.getElementById('mobileMenu');
if (mobileMenuBtn && mobileMenu) {
  mobileMenuBtn.addEventListener('click', () => {
    mobileMenu.classList.toggle('hidden');
  });
}

// ===== AUTH STATE =====
function getUser() {
  try { return JSON.parse(sessionStorage.getItem('user') || localStorage.getItem('user') || 'null'); }
  catch { return null; }
}

function updateNavAuth() {
  const user = getUser();
  const authButtons = document.getElementById('authButtons');
  const userMenuEl = document.getElementById('userMenu');
  const userNameEl = document.getElementById('userName');

  if (user && authButtons && userMenuEl) {
    authButtons.style.display = 'none';
    userMenuEl.style.removeProperty('display');
    if (userNameEl) userNameEl.textContent = user.fullName || user.email;
  }
}
updateNavAuth();

function logout() {
  localStorage.removeItem('user');
  sessionStorage.removeItem('user');
  window.location.href = '/index.html';
}

// ===== API HELPER =====
async function apiCall(endpoint, method = 'GET', data = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };
  const user = getUser();
  if (user?.token) options.headers['Authorization'] = `Bearer ${user.token}`;
  if (data) options.body = JSON.stringify(data);

  const res = await fetch(`${API_BASE}${endpoint}`, options);
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Request failed');
  return json;
}

// ===== SHOW TOAST =====
function showToast(msg, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `fixed bottom-6 right-6 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium transition-all duration-300 ${
    type === 'success' ? 'bg-[#1a6b3c] text-white' : 'bg-red-600 text-white'
  }`;
  toast.textContent = msg;
  document.body.appendChild(toast);
  setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, 3500);
}

// ===== FORMAT DATE =====
function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}