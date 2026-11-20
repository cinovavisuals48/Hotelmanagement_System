/* Customers CRUD + booking history */

const customersBody = document.getElementById('customers-body');
const customerForm = document.getElementById('customer-form');
const searchInput = document.getElementById('search');
let customers = [];
let searchTimer;

async function loadCustomers() {
    try {
        const q = searchInput.value.trim();
        customers = await api('/customers' + (q ? '?search=' + encodeURIComponent(q) : ''));
        renderCustomers();
    } catch (e) {
        customersBody.innerHTML = `<tr><td colspan="6" class="empty">${esc(e.message)}</td></tr>`;
        toast(e.message, 'error');
    }
}

function renderCustomers() {
    customersBody.innerHTML = customers.length ? customers.map(c => `
        <tr>
            <td>${c.id}</td>
            <td><strong>${esc(c.firstName)} ${esc(c.lastName)}</strong><span class="sub">${esc(c.address || '')}</span></td>
            <td>${esc(c.email)}<span class="sub">${esc(c.phone)}</span></td>
            <td>${c.idProofType ? `${esc(c.idProofType)}<span class="sub">${esc(c.idProofNumber || '')}</span>` : '<span class="hint">–</span>'}</td>
            <td>${formatDate(c.createdAt)}</td>
            <td><div class="actions">
                <button class="btn btn-outline btn-sm" data-history="${c.id}">Bookings</button>
                <button class="btn btn-outline btn-sm" data-edit="${c.id}">Edit</button>
                <button class="btn btn-danger btn-sm" data-delete="${c.id}">Delete</button>
            </div></td>
        </tr>`).join('') : '<tr><td colspan="6" class="empty">No customers found</td></tr>';
}

function openCustomerModal(customer) {
    customerForm.reset();
    clearFieldErrors(customerForm);
    document.getElementById('customer-modal-title').textContent =
        customer ? `Edit ${customer.firstName} ${customer.lastName}` : 'Add Customer';
    if (customer) fillForm(customerForm, customer); else customerForm.elements.id.value = '';
    openModal('customer-modal');
    customerForm.firstName.focus();
}

async function showHistory(customer) {
    document.getElementById('history-title').textContent = `Bookings — ${customer.firstName} ${customer.lastName}`;
    const body = document.getElementById('history-body');
    body.innerHTML = '<tr><td colspan="5" class="empty">Loading…</td></tr>';
    openModal('history-modal');
    try {
        const bookings = await api(`/customers/${customer.id}/bookings`);
        body.innerHTML = bookings.length ? bookings.map(b => `
            <tr>
                <td>${b.id}</td>
                <td>${esc(b.roomNumber)}<span class="sub">${esc(titleCase(b.roomType))}</span></td>
                <td>${formatDate(b.checkInDate)} → ${formatDate(b.checkOutDate)}<span class="sub">${b.nights} night(s)</span></td>
                <td>${formatMoney(b.totalAmount)}</td>
                <td>${badge(b.status)}</td>
            </tr>`).join('') : '<tr><td colspan="5" class="empty">No bookings yet</td></tr>';
    } catch (e) {
        body.innerHTML = `<tr><td colspan="5" class="empty">${esc(e.message)}</td></tr>`;
    }
}

document.getElementById('add-btn').addEventListener('click', () => openCustomerModal(null));
searchInput.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadCustomers, 250);
});

customersBody.addEventListener('click', async e => {
    const btn = e.target.closest('button');
    if (!btn) return;
    const id = Number(btn.dataset.edit || btn.dataset.delete || btn.dataset.history);
    const customer = customers.find(c => c.id === id);
    if (btn.dataset.edit) openCustomerModal(customer);
    if (btn.dataset.history) showHistory(customer);
    if (btn.dataset.delete) {
        if (!confirm(`Delete ${customer.firstName} ${customer.lastName}? This cannot be undone.`)) return;
        try {
            await api(`/customers/${id}`, { method: 'DELETE' });
            toast('Customer deleted', 'success');
            loadCustomers();
        } catch (err) {
            toast(err.message, 'error');
        }
    }
});

customerForm.addEventListener('submit', async e => {
    e.preventDefault();
    clearFieldErrors(customerForm);
    if (!customerForm.checkValidity()) {
        customerForm.reportValidity();
        return;
    }
    const { id, ...payload } = formData(customerForm);
    try {
        if (id) {
            await api(`/customers/${id}`, { method: 'PUT', body: payload });
            toast('Customer updated', 'success');
        } else {
            await api('/customers', { method: 'POST', body: payload });
            toast('Customer added', 'success');
        }
        closeModal('customer-modal');
        loadCustomers();
    } catch (err) {
        showFieldErrors(customerForm, err.fieldErrors);
        toast(err.message, 'error');
    }
});

loadCustomers();
