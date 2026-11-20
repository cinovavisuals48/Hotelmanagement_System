/* Rooms CRUD */

const roomsBody = document.getElementById('rooms-body');
const roomForm = document.getElementById('room-form');
const searchInput = document.getElementById('search');
const typeFilter = document.getElementById('type-filter');
const statusFilter = document.getElementById('status-filter');
let rooms = [];

async function loadRooms() {
    try {
        rooms = await api('/rooms');
        renderRooms();
    } catch (e) {
        roomsBody.innerHTML = `<tr><td colspan="7" class="empty">${esc(e.message)}</td></tr>`;
        toast(e.message, 'error');
    }
}

function renderRooms() {
    const q = searchInput.value.trim().toLowerCase();
    const list = rooms.filter(r =>
        (!q || r.roomNumber.toLowerCase().includes(q) || (r.description || '').toLowerCase().includes(q)) &&
        (!typeFilter.value || r.roomType === typeFilter.value) &&
        (!statusFilter.value || r.status === statusFilter.value));

    roomsBody.innerHTML = list.length ? list.map(r => `
        <tr>
            <td><strong>${esc(r.roomNumber)}</strong><span class="sub">${esc(r.description || '')}</span></td>
            <td><span class="badge badge-type">${esc(titleCase(r.roomType))}</span></td>
            <td>${r.floor}</td>
            <td>${r.capacity}</td>
            <td>${formatMoney(r.pricePerNight)}</td>
            <td>${badge(r.status)}</td>
            <td><div class="actions">
                <button class="btn btn-outline btn-sm" data-edit="${r.id}">Edit</button>
                <button class="btn btn-danger btn-sm" data-delete="${r.id}">Delete</button>
            </div></td>
        </tr>`).join('') : '<tr><td colspan="7" class="empty">No rooms found</td></tr>';
}

function openRoomModal(room) {
    roomForm.reset();
    clearFieldErrors(roomForm);
    const statusSelect = roomForm.status;
    document.getElementById('room-modal-title').textContent = room ? `Edit Room ${room.roomNumber}` : 'Add Room';
    if (room) {
        fillForm(roomForm, room);
        statusSelect.disabled = room.status === 'OCCUPIED';
    } else {
        roomForm.elements.id.value = '';
        statusSelect.disabled = false;
        statusSelect.value = 'AVAILABLE';
    }
    openModal('room-modal');
    roomForm.roomNumber.focus();
}

document.getElementById('add-btn').addEventListener('click', () => openRoomModal(null));
[searchInput, typeFilter, statusFilter].forEach(el => el.addEventListener('input', renderRooms));

roomsBody.addEventListener('click', async e => {
    const editBtn = e.target.closest('[data-edit]');
    const delBtn = e.target.closest('[data-delete]');
    if (editBtn) {
        openRoomModal(rooms.find(r => r.id === Number(editBtn.dataset.edit)));
    } else if (delBtn) {
        const room = rooms.find(r => r.id === Number(delBtn.dataset.delete));
        if (!confirm(`Delete room ${room.roomNumber}? This cannot be undone.`)) return;
        try {
            await api(`/rooms/${room.id}`, { method: 'DELETE' });
            toast(`Room ${room.roomNumber} deleted`, 'success');
            loadRooms();
        } catch (err) {
            toast(err.message, 'error');
        }
    }
});

roomForm.addEventListener('submit', async e => {
    e.preventDefault();
    clearFieldErrors(roomForm);
    if (!roomForm.checkValidity()) {
        roomForm.reportValidity();
        return;
    }
    const d = formData(roomForm);
    const payload = {
        roomNumber: d.roomNumber,
        roomType: d.roomType,
        pricePerNight: Number(d.pricePerNight),
        capacity: Number(d.capacity),
        floor: Number(d.floor),
        status: roomForm.status.disabled ? null : d.status,
        description: d.description
    };
    try {
        if (d.id) {
            await api(`/rooms/${d.id}`, { method: 'PUT', body: payload });
            toast(`Room ${payload.roomNumber} updated`, 'success');
        } else {
            await api('/rooms', { method: 'POST', body: payload });
            toast(`Room ${payload.roomNumber} added`, 'success');
        }
        closeModal('room-modal');
        loadRooms();
    } catch (err) {
        showFieldErrors(roomForm, err.fieldErrors);
        toast(err.message, 'error');
    }
});

loadRooms();
