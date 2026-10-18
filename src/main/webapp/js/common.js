const LOGO_SVG =
    '<svg viewBox="0 0 64 64" aria-hidden="true">' +
    '<circle cx="32" cy="32" r="31" fill="#ffffff"/>' +
    '<path d="M14 40a18 18 0 0 1 36 0z" fill="#f2994a"/>' +
    '<g stroke="#f2994a" stroke-width="3" stroke-linecap="round">' +
    '<path d="M32 10v6M13 21l4 4M51 21l-4 4M6 36h5M53 36h5"/></g>' +
    '<path d="M10 44h44M16 50h32" stroke="#0b5d66" stroke-width="3" stroke-linecap="round"/>' +
    '</svg>';

const NAV_ITEMS = [
    { key: 'menu',     href: 'index.html',    label: 'Main Menu' },
    { key: 'register', href: 'register.html', label: 'Register Appointment' },
    { key: 'search',   href: 'search.html',   label: 'Find Appointment' },
    { key: 'billing',  href: 'billing.html',  label: 'Billing' },
    { key: 'schedule', href: 'schedule.html', label: 'Daily Schedule' },
    { key: 'reports',  href: 'reports.html',  label: 'Reports', adminOnly: true },
    { key: 'help',     href: 'help.html',     label: 'Help' }
];

async function api(method, path, body) {
    const options = { method: method, headers: { 'Accept': 'application/json' }, credentials: 'same-origin' };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(path, options);
    } catch (networkProblem) {
        const error = new Error('Cannot reach the server. Please check that it is running and try again.');
        error.status = 0;
        throw error;
    }

    let data = null;
    const text = await response.text();
    if (text) {
        try { data = JSON.parse(text); } catch (notJson) { data = null; }
    }

    if (response.ok) {
        return data;
    }

    if (response.status === 401 && !location.pathname.endsWith('login.html')) {
        location.href = 'login.html?expired=1';
    }

    const error = new Error((data && data.error) || 'The request failed (code ' + response.status + ').');
    error.status = response.status;
    error.fields = (data && data.fields) || {};
    throw error;
}

function escapeHtml(value) {
    return String(value === null || value === undefined ? '' : value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function formatMoney(amount) {
    const number = Number(amount || 0);
    return 'Rs. ' + number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function formatDate(isoDate) {
    if (!isoDate) { return ''; }
    const parts = String(isoDate).substring(0, 10).split('-');
    return Number(parts[2]) + ' ' + MONTHS[Number(parts[1]) - 1] + ' ' + parts[0];
}

function formatTime(isoTime) {
    if (!isoTime) { return ''; }
    const parts = String(isoTime).split(':');
    const hour = Number(parts[0]);
    const suffix = hour >= 12 ? 'PM' : 'AM';
    const hour12 = hour % 12 === 0 ? 12 : hour % 12;
    return hour12 + ':' + parts[1] + ' ' + suffix;
}

function formatDateTime(isoDateTime) {
    if (!isoDateTime) { return ''; }
    const parts = String(isoDateTime).split('T');
    return formatDate(parts[0]) + ', ' + formatTime(parts[1]);
}

function toIsoDate(date) {
    const d = date || new Date();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return d.getFullYear() + '-' + month + '-' + day;
}

function queryParam(name) {
    return new URLSearchParams(location.search).get(name);
}

function showMessage(element, type, text) {
    element.className = 'msg ' + type;
    element.textContent = text;
    element.setAttribute('role', type === 'error' ? 'alert' : 'status');
}

function hideMessage(element) {
    element.className = 'hidden';
    element.textContent = '';
}

function showFieldErrors(form, errors) {
    form.querySelectorAll('.field').forEach(function (field) {
        field.classList.remove('invalid');
        const slot = field.querySelector('.field-error');
        if (slot) { slot.textContent = ''; }
    });
    let first = null;
    Object.keys(errors || {}).forEach(function (name) {
        const input = form.elements[name];
        if (!input) { return; }
        const field = input.closest('.field');
        field.classList.add('invalid');
        const slot = field.querySelector('.field-error');
        if (slot) { slot.textContent = errors[name]; }
        if (!first) { first = input; }
    });
    if (first) { first.focus(); }
}

function statusBadge(status) {
    return '<span class="badge ' + escapeHtml(status) + '">' + escapeHtml(status) + '</span>';
}

function confirmDialog(title, message, confirmLabel) {
    return new Promise(function (resolve) {
        const overlay = document.createElement('div');
        overlay.className = 'overlay';
        overlay.innerHTML =
            '<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="dlg-title">' +
            '<h2 id="dlg-title">' + escapeHtml(title) + '</h2>' +
            '<p>' + escapeHtml(message) + '</p>' +
            '<div class="actions">' +
            '<button type="button" class="btn secondary" data-answer="no">Go back</button>' +
            '<button type="button" class="btn accent" data-answer="yes">' + escapeHtml(confirmLabel) + '</button>' +
            '</div></div>';
        function close(answer) {
            document.removeEventListener('keydown', onKey);
            overlay.remove();
            resolve(answer);
        }
        function onKey(event) {
            if (event.key === 'Escape') { close(false); }
        }
        overlay.addEventListener('click', function (event) {
            const answer = event.target.getAttribute('data-answer');
            if (answer) { close(answer === 'yes'); }
        });
        document.addEventListener('keydown', onKey);
        document.body.appendChild(overlay);
        overlay.querySelector('[data-answer="no"]').focus();
    });
}

async function exitSystem() {
    const sure = await confirmDialog('Exit the system?',
        'You will be logged out. Anything you have not saved will be lost.', 'Yes, exit');
    if (!sure) { return; }
    try {
        await api('POST', 'api/auth/logout', {});
    } catch (ignored) {
    }
    location.replace('exit.html');
}

async function initPage(activeKey) {
    const user = await api('GET', 'api/auth/me');
    const isAdmin = user.role === 'ADMIN';

    const links = NAV_ITEMS
        .filter(function (item) { return !item.adminOnly || isAdmin; })
        .map(function (item) {
            const current = item.key === activeKey;
            return '<li><a href="' + item.href + '"' + (current ? ' class="active" aria-current="page"' : '') + '>' +
                item.label + '</a></li>';
        }).join('');

    document.getElementById('app-header').innerHTML =
        '<div class="topbar"><div class="topbar-inner">' +
        '<a class="brand" href="index.html">' + LOGO_SVG +
        '<span><span class="brand-name">Sunrise Dental Clinic</span>' +
        '<span class="brand-sub">Appointment &amp; Patient Management System</span></span></a>' +
        '<div class="userbox"><strong>' + escapeHtml(user.fullName) + '</strong>' +
        '<span class="role-tag">' + escapeHtml(user.role) + '</span></div>' +
        '</div></div>' +
        '<nav class="mainnav" aria-label="Main menu"><ul>' + links +
        '<li class="push-right"><button type="button" class="exit" id="nav-exit">Exit System</button></li>' +
        '</ul></nav>';

    document.getElementById('nav-exit').addEventListener('click', exitSystem);
    return user;
}
