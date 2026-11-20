/* Dashboard: stats, today's arrivals/departures, recent bookings */

document.getElementById('today-label').textContent =
    new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

async function loadDashboard() {
    try {
        const [stats, bookings] = await Promise.all([api('/dashboard/stats'), api('/bookings')]);

        document.querySelectorAll('[data-stat]').forEach(el => {
            const key = el.dataset.stat;
            el.textContent = key === 'totalRevenue' ? formatMoney(stats[key]) : stats[key];
        });

        const today = todayIso();
        const arrivals = bookings.filter(b => b.status === 'BOOKED' && b.checkInDate === today);
        const departures = bookings.filter(b => b.status === 'CHECKED_IN' && b.checkOutDate <= today);

        document.getElementById('arrivals-count').textContent = `(${arrivals.length})`;
        document.getElementById('departures-count').textContent = `(${departures.length})`;

        document.getElementById('arrivals').innerHTML = arrivals.length ? arrivals.map(b => `
            <tr>
                <td>${esc(b.customerName)}<span class="sub">${esc(b.customerPhone)}</span></td>
                <td>${esc(b.roomNumber)}<span class="sub">${esc(titleCase(b.roomType))}</span></td>
                <td>${b.nights}</td>
                <td><button class="btn btn-success btn-sm" data-checkin="${b.id}">Check in</button></td>
            </tr>`).join('') : '<tr><td colspan="4" class="empty">No arrivals today</td></tr>';

        document.getElementById('departures').innerHTML = departures.length ? departures.map(b => `
            <tr>
                <td>${esc(b.customerName)}<span class="sub">${esc(b.customerPhone)}</span></td>
                <td>${esc(b.roomNumber)}</td>
                <td>${formatMoney(b.totalAmount)}</td>
                <td><button class="btn btn-primary btn-sm" data-checkout="${b.id}">Check out</button></td>
            </tr>`).join('') : '<tr><td colspan="4" class="empty">No departures today</td></tr>';

        const recent = bookings.slice(0, 8);
        document.getElementById('recent').innerHTML = recent.length ? recent.map(b => `
            <tr>
                <td>${b.id}</td>
                <td>${esc(b.customerName)}<span class="sub">${esc(b.customerEmail)}</span></td>
                <td>${esc(b.roomNumber)}<span class="sub">${esc(titleCase(b.roomType))}</span></td>
                <td>${formatDate(b.checkInDate)} → ${formatDate(b.checkOutDate)}<span class="sub">${b.nights} night(s)</span></td>
                <td>${formatMoney(b.totalAmount)}</td>
                <td>${badge(b.status)}</td>
            </tr>`).join('') : '<tr><td colspan="6" class="empty">No bookings yet. <a href="book.html">Create the first one</a>.</td></tr>';
    } catch (e) {
        toast(e.message, 'error');
        ['arrivals', 'departures', 'recent'].forEach(id => {
            document.getElementById(id).innerHTML = `<tr><td colspan="6" class="empty">${esc(e.message)}</td></tr>`;
        });
    }
}

document.addEventListener('click', async e => {
    const inBtn = e.target.closest('[data-checkin]');
    const outBtn = e.target.closest('[data-checkout]');
    if (!inBtn && !outBtn) return;
    const id = (inBtn || outBtn).dataset[inBtn ? 'checkin' : 'checkout'];
    try {
        await api(`/bookings/${id}/${inBtn ? 'check-in' : 'check-out'}`, { method: 'PATCH' });
        toast(inBtn ? 'Guest checked in' : 'Guest checked out', 'success');
        loadDashboard();
    } catch (err) {
        toast(err.message, 'error');
    }
});

loadDashboard();
