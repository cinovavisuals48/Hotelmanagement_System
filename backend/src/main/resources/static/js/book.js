/* New booking: check availability, pick/auto-allocate a room, save guest + booking */

const form = document.getElementById('booking-form');
const roomGrid = document.getElementById('room-grid');
const roomsHint = document.getElementById('rooms-hint');
const summaryEl = document.getElementById('summary');
const confirmationEl = document.getElementById('confirmation');
const checkIn = form.checkInDate;
const checkOut = form.checkOutDate;

let availableRooms = [];
let selectedRoomId = null;   // null = let the system allocate
let searchToken = 0;

function initDates() {
    checkIn.min = todayIso();
    checkIn.value = todayIso();
    checkOut.min = todayIso(1);
    checkOut.value = todayIso(1);
}

function stayIsValid() {
    return checkIn.value && checkOut.value && checkOut.value > checkIn.value;
}

async function searchRooms() {
    selectedRoomId = null;
    if (!stayIsValid()) {
        availableRooms = [];
        roomGrid.innerHTML = '';
        roomsHint.textContent = 'Check-out must be after check-in.';
        renderSummary();
        return;
    }
    const params = new URLSearchParams({
        checkIn: checkIn.value,
        checkOut: checkOut.value,
        guests: form.numberOfGuests.value || 1
    });
    if (form.roomType.value) params.set('type', form.roomType.value);

    const token = ++searchToken;
    roomsHint.textContent = 'Checking availability…';
    try {
        const rooms = await api('/rooms/available?' + params);
        if (token !== searchToken) return; // a newer search has started
        availableRooms = rooms;
        renderRooms();
    } catch (e) {
        if (token !== searchToken) return;
        availableRooms = [];
        roomGrid.innerHTML = '';
        roomsHint.textContent = e.message;
    }
    renderSummary();
}

function renderRooms() {
    const nights = nightsBetween(checkIn.value, checkOut.value);
    if (!availableRooms.length) {
        roomGrid.innerHTML = '';
        roomsHint.innerHTML = '<strong style="color:var(--red)">No rooms match these dates, guests and type.</strong> Try other dates or "Any type".';
        return;
    }
    roomsHint.textContent = `${availableRooms.length} room(s) free for ${nights} night(s). Select one, or keep "Auto-allocate".`;
    const cheapest = availableRooms[0];
    roomGrid.innerHTML = `
        <div class="room-card auto ${selectedRoomId === null ? 'selected' : ''}" data-room="">
            <div class="num">Auto-allocate</div>
            <div class="meta">Best-priced free room that fits — currently ${esc(cheapest.roomNumber)}</div>
            <div class="price">${formatMoney(cheapest.pricePerNight)} / night</div>
        </div>
        ${availableRooms.map(r => `
        <div class="room-card ${selectedRoomId === r.id ? 'selected' : ''}" data-room="${r.id}">
            <div class="num">Room ${esc(r.roomNumber)}</div>
            <span class="badge badge-type">${esc(titleCase(r.roomType))}</span>
            <div class="meta">Floor ${r.floor} · up to ${r.capacity} guest(s)</div>
            <div class="meta">${esc(r.description || '')}</div>
            <div class="price">${formatMoney(r.pricePerNight)} / night</div>
        </div>`).join('')}`;
}

function currentRoom() {
    if (!availableRooms.length) return null;
    return selectedRoomId === null ? availableRooms[0] : availableRooms.find(r => r.id === selectedRoomId);
}

function renderSummary() {
    const room = currentRoom();
    const nights = stayIsValid() ? nightsBetween(checkIn.value, checkOut.value) : 0;
    summaryEl.innerHTML = `
        <div><div class="k">Stay</div><div class="v">${nights ? `${formatDate(checkIn.value)} → ${formatDate(checkOut.value)}` : '–'}</div></div>
        <div><div class="k">Nights</div><div class="v">${nights || '–'}</div></div>
        <div><div class="k">Room</div><div class="v">${room ? `${esc(room.roomNumber)} (${esc(titleCase(room.roomType))})${selectedRoomId === null ? ' · auto' : ''}` : '–'}</div></div>
        <div><div class="k">Estimated total</div><div class="v">${room && nights ? formatMoney(room.pricePerNight * nights) : '–'}</div></div>`;
}

roomGrid.addEventListener('click', e => {
    const card = e.target.closest('.room-card');
    if (!card) return;
    selectedRoomId = card.dataset.room ? Number(card.dataset.room) : null;
    renderRooms();
    renderSummary();
});

checkIn.addEventListener('change', () => {
    if (checkIn.value) {
        const next = new Date(checkIn.value + 'T00:00:00');
        next.setDate(next.getDate() + 1);
        const nextIso = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-${String(next.getDate()).padStart(2, '0')}`;
        checkOut.min = nextIso;
        if (!checkOut.value || checkOut.value <= checkIn.value) checkOut.value = nextIso;
    }
    searchRooms();
});
[checkOut, form.numberOfGuests, form.roomType].forEach(el => el.addEventListener('change', searchRooms));

// Returning guest: fill their details from the database by email
form.email.addEventListener('blur', async () => {
    const email = form.email.value.trim().toLowerCase();
    if (!email || !email.includes('@')) return;
    try {
        const matches = await api('/customers?search=' + encodeURIComponent(email));
        const customer = matches.find(c => c.email.toLowerCase() === email);
        if (customer) {
            ['firstName', 'lastName', 'phone', 'address', 'idProofType', 'idProofNumber']
                .forEach(k => { if (!form[k].value) form[k].value = customer[k] || ''; });
            toast(`Welcome back, ${customer.firstName}! Saved details filled in.`, 'success');
        }
    } catch (_) { /* lookup is optional */ }
});

form.addEventListener('submit', async e => {
    e.preventDefault();
    clearFieldErrors(form);
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    if (!stayIsValid()) {
        toast('Check-out date must be after check-in date', 'error');
        return;
    }
    const d = formData(form);
    const payload = {
        checkInDate: d.checkInDate,
        checkOutDate: d.checkOutDate,
        numberOfGuests: Number(d.numberOfGuests),
        roomType: d.roomType,
        roomId: selectedRoomId,
        specialRequests: d.specialRequests,
        customer: {
            firstName: d.firstName,
            lastName: d.lastName,
            email: d.email,
            phone: d.phone,
            address: d.address,
            idProofType: d.idProofType,
            idProofNumber: d.idProofNumber
        }
    };

    const btn = document.getElementById('submit-btn');
    btn.disabled = true;
    btn.textContent = 'Booking…';
    try {
        const booking = await api('/bookings', { method: 'POST', body: payload });
        showConfirmation(booking);
    } catch (err) {
        showFieldErrors(form, err.fieldErrors);
        toast(err.message, 'error');
        if (err.status === 409) searchRooms(); // room was taken meanwhile - refresh list
    } finally {
        btn.disabled = false;
        btn.textContent = 'Confirm booking';
    }
});

form.addEventListener('reset', () => {
    setTimeout(() => { clearFieldErrors(form); initDates(); searchRooms(); }, 0);
});

function showConfirmation(b) {
    form.classList.add('hidden');
    confirmationEl.classList.remove('hidden');
    confirmationEl.innerHTML = `
        <h2>Booking confirmed — #${b.id}</h2>
        <p>${esc(b.customerName)} has been allocated</p>
        <div class="big-room">Room ${esc(b.roomNumber)}</div>
        <p><span class="badge badge-type">${esc(titleCase(b.roomType))}</span> ${badge(b.status)}</p>
        <div class="summary">
            <div><div class="k">Check-in</div><div class="v">${formatDate(b.checkInDate)}</div></div>
            <div><div class="k">Check-out</div><div class="v">${formatDate(b.checkOutDate)}</div></div>
            <div><div class="k">Nights</div><div class="v">${b.nights}</div></div>
            <div><div class="k">Guests</div><div class="v">${b.numberOfGuests}</div></div>
            <div><div class="k">Total</div><div class="v">${formatMoney(b.totalAmount)}</div></div>
        </div>
        <div class="form-actions">
            <a class="btn btn-outline" href="bookings.html">View all bookings</a>
            <button class="btn btn-gold" id="another-btn">Book another room</button>
        </div>`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    document.getElementById('another-btn').addEventListener('click', () => {
        confirmationEl.classList.add('hidden');
        form.classList.remove('hidden');
        form.reset();
    });
}

initDates();
searchRooms();
