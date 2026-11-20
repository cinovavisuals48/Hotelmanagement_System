/* Bookings list: check-in / check-out / cancel / edit / delete */

const bookingsBody = document.getElementById('bookings-body');
const searchInput = document.getElementById('search');
const statusFilter = document.getElementById('status-filter');
const editForm = document.getElementById('edit-form');
let bookings = [];

// Allow pre-filtering via bookings.html?status=BOOKED
const initialStatus = new URLSearchParams(location.search).get('status');
if (initialStatus) statusFilter.value = initialStatus;

async function loadBookings() {
    try {
        bookings = await api('/bookings');
        renderBookings();
    } catch (e) {
        bookingsBody.innerHTML = `<tr><td colspan="8" class="empty">${esc(e.message)}</td></tr>`;
        toast(e.message, 'error');
    }
}

function actionsFor(b) {
    const today = todayIso();
    const buttons = [];
    if (b.status === 'BOOKED') {
        if (b.checkInDate <= today && b.checkOutDate > today) {
            buttons.push(`<button class="btn btn-success btn-sm" data-action="check-in" data-id="${b.id}">Check in</button>`);
        }
        buttons.push(`<button class="btn btn-outline btn-sm" data-action="edit" data-id="${b.id}">Edit</button>`);
        buttons.push(`<button class="btn btn-outline btn-sm" data-action="cancel" data-id="${b.id}">Cancel</button>`);
    }
    if (b.status === 'CHECKED_IN') {
        buttons.push(`<button class="btn btn-primary btn-sm" data-action="check-out" data-id="${b.id}">Check out</button>`);
    }
    if (b.status !== 'CHECKED_IN') {
        buttons.push(`<button class="btn btn-danger btn-sm" data-action="delete" data-id="${b.id}">Delete</button>`);
    }
    return buttons.join('');
}

function renderBookings() {
    const q = searchInput.value.trim().toLowerCase();
    const list = bookings.filter(b =>
        (!statusFilter.value || b.status === statusFilter.value) &&
        (!q || b.customerName.toLowerCase().includes(q) || b.customerEmail.toLowerCase().includes(q)
            || b.roomNumber.toLowerCase().includes(q) || String(b.id) === q));

    bookingsBody.innerHTML = list.length ? list.map(b => `
        <tr>
            <td>${b.id}</td>
            <td><strong>${esc(b.customerName)}</strong><span class="sub">${esc(b.customerEmail)} · ${esc(b.customerPhone)}</span></td>
            <td>${esc(b.roomNumber)}<span class="sub">${esc(titleCase(b.roomType))}</span></td>
            <td>${formatDate(b.checkInDate)} → ${formatDate(b.checkOutDate)}<span class="sub">${b.nights} night(s)</span></td>
            <td>${b.numberOfGuests}</td>
            <td>${formatMoney(b.totalAmount)}</td>
            <td>${badge(b.status)}</td>
            <td><div class="actions">${actionsFor(b)}</div></td>
        </tr>`).join('') : '<tr><td colspan="8" class="empty">No bookings found</td></tr>';
}

async function openEdit(b) {
    editForm.reset();
    clearFieldErrors(editForm);
    document.getElementById('edit-title').textContent = `Edit booking #${b.id} — ${b.customerName}`;
    fillForm(editForm, b);
    editForm.checkInDate.min = b.checkInDate < todayIso() ? b.checkInDate : todayIso();
    await refreshRoomOptions(b);
    openModal('edit-modal');
}

/** Lists the booking's current room plus every room free for the chosen dates. */
async function refreshRoomOptions(b) {
    const select = editForm.roomId;
    const ci = editForm.checkInDate.value, co = editForm.checkOutDate.value;
    const keep = select.value || String(b.roomId);
    let options = [{ id: b.roomId, roomNumber: b.roomNumber, roomType: b.roomType, pricePerNight: b.pricePerNight, current: true }];
    if (ci && co && co > ci) {
        try {
            const params = new URLSearchParams({ checkIn: ci, checkOut: co, guests: editForm.numberOfGuests.value || 1 });
            const free = await api('/rooms/available?' + params);
            options = options.concat(free.filter(r => r.id !== b.roomId));
        } catch (_) { /* keep the current room only */ }
    }
    select.innerHTML = options.map(r =>
        `<option value="${r.id}">${esc(r.roomNumber)} · ${esc(titleCase(r.roomType))} · ${formatMoney(r.pricePerNight)}${r.current ? ' (current)' : ''}</option>`).join('');
    select.value = options.some(o => String(o.id) === keep) ? keep : String(b.roomId);
}

['checkInDate', 'checkOutDate', 'numberOfGuests'].forEach(name =>
    editForm[name].addEventListener('change', () => {
        const b = bookings.find(x => x.id === Number(editForm.elements.id.value));
        if (b) refreshRoomOptions(b);
    }));

editForm.addEventListener('submit', async e => {
    e.preventDefault();
    clearFieldErrors(editForm);
    if (!editForm.checkValidity()) {
        editForm.reportValidity();
        return;
    }
    const d = formData(editForm);
    try {
        await api(`/bookings/${d.id}`, {
            method: 'PUT',
            body: {
                checkInDate: d.checkInDate,
                checkOutDate: d.checkOutDate,
                numberOfGuests: Number(d.numberOfGuests),
                roomId: d.roomId ? Number(d.roomId) : null,
                specialRequests: d.specialRequests
            }
        });
        toast(`Booking #${d.id} updated`, 'success');
        closeModal('edit-modal');
        loadBookings();
    } catch (err) {
        showFieldErrors(editForm, err.fieldErrors);
        toast(err.message, 'error');
    }
});

bookingsBody.addEventListener('click', async e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const b = bookings.find(x => x.id === id);
    const action = btn.dataset.action;

    if (action === 'edit') return openEdit(b);
    if (action === 'cancel' && !confirm(`Cancel booking #${id} for ${b.customerName}?`)) return;
    if (action === 'delete' && !confirm(`Permanently delete booking #${id}?`)) return;

    try {
        if (action === 'delete') {
            await api(`/bookings/${id}`, { method: 'DELETE' });
            toast(`Booking #${id} deleted`, 'success');
        } else {
            await api(`/bookings/${id}/${action}`, { method: 'PATCH' });
            const msg = { 'check-in': `${b.customerName} checked in to room ${b.roomNumber}`,
                          'check-out': `${b.customerName} checked out — room ${b.roomNumber} is free`,
                          'cancel': `Booking #${id} cancelled` }[action];
            toast(msg, 'success');
        }
        loadBookings();
    } catch (err) {
        toast(err.message, 'error');
    }
});

[searchInput, statusFilter].forEach(el => el.addEventListener('input', renderBookings));

loadBookings();
