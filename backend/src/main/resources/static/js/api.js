/* =========================================================
   Shared helpers: REST calls, formatting, toasts, modals
   ========================================================= */

// When the pages are served by Spring Boot the API is on the same origin.
// When opened from another dev server (e.g. Live Server on :5500) talk to localhost:8080.
const API_BASE = (location.protocol.startsWith('http') && location.port === '8080')
    ? '/api'
    : 'http://localhost:8080/api';

/** Calls the REST API and returns parsed JSON (or null for 204). Throws an Error with the server's message. */
async function api(path, { method = 'GET', body } = {}) {
    const options = { method, headers: {} };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    let response;
    try {
        response = await fetch(API_BASE + path, options);
    } catch (e) {
        throw new Error('Cannot reach the server. Is the Spring Boot application running on port 8080?');
    }
    if (response.status === 204) return null;
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const err = new Error((data && data.message) || `Request failed (${response.status})`);
        err.status = response.status;
        err.fieldErrors = (data && data.fieldErrors) || {};
        throw err;
    }
    return data;
}

/* ---------- Formatting ---------- */
const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
function formatMoney(value) { return money.format(Number(value || 0)); }

function formatDate(iso) {
    if (!iso) return '';
    const [y, m, d] = iso.substring(0, 10).split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function todayIso(offsetDays = 0) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function nightsBetween(checkIn, checkOut) {
    if (!checkIn || !checkOut) return 0;
    const ms = new Date(checkOut + 'T00:00:00') - new Date(checkIn + 'T00:00:00');
    return Math.round(ms / 86400000);
}

function titleCase(value) {
    return String(value || '').toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

/** Escapes text before putting it into innerHTML. */
function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

function badge(status) {
    return `<span class="badge badge-${esc(status)}">${esc(titleCase(status))}</span>`;
}

/* ---------- Toasts ---------- */
function toast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => el.remove(), type === 'error' ? 6000 : 3500);
}

/* ---------- Modals ---------- */
function openModal(id) { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

document.addEventListener('click', e => {
    if (e.target.classList.contains('modal-backdrop')) e.target.classList.remove('open');
    const closer = e.target.closest('[data-close]');
    if (closer) closeModal(closer.dataset.close);
});
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') document.querySelectorAll('.modal-backdrop.open').forEach(m => m.classList.remove('open'));
});

/* ---------- Forms ---------- */
/** Reads a form into a plain object; empty strings become null. */
function formData(form) {
    const obj = {};
    new FormData(form).forEach((value, key) => {
        const v = typeof value === 'string' ? value.trim() : value;
        obj[key] = v === '' ? null : v;
    });
    return obj;
}

function clearFieldErrors(form) {
    form.querySelectorAll('.field').forEach(f => f.classList.remove('invalid'));
    form.querySelectorAll('.error-text').forEach(e => e.textContent = '');
}

/** Shows server-side validation messages next to the matching inputs (supports "customer.email" style keys). */
function showFieldErrors(form, fieldErrors) {
    Object.entries(fieldErrors || {}).forEach(([key, message]) => {
        const name = key.includes('.') ? key.split('.').pop() : key;
        const input = form.querySelector(`[name="${name}"]`);
        if (!input) return;
        const field = input.closest('.field');
        field.classList.add('invalid');
        const errEl = field.querySelector('.error-text');
        if (errEl) errEl.textContent = message;
    });
}

/** Fills a form's inputs from an object by matching names. */
function fillForm(form, data) {
    Array.from(form.elements).forEach(el => {
        if (el.name && data[el.name] !== undefined) el.value = data[el.name] ?? '';
    });
}

/* ---------- Navigation highlight ---------- */
document.addEventListener('DOMContentLoaded', () => {
    const page = location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-links a').forEach(a => {
        if (a.getAttribute('href') === page) a.classList.add('active');
    });
});
