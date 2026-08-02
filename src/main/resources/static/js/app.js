/* ============================================================
   Club Projects — frontend
   Talks to the Spring Boot API at /api. No framework, no build step.
   ============================================================ */
(() => {
  'use strict';

  /* ---------- tiny helpers ---------- */
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (m) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
  const initials = (name) => {
    const parts = String(name || '').trim().split(/\s+/);
    return ((parts[0] || '')[0] || '') + ((parts[1] || '')[0] || '');
  };
  const debounce = (fn, ms) => {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
  };
  const fmtDate = (iso) => {
    if (!iso) return '';
    const d = new Date(iso + 'T00:00:00');
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  /* ---------- status vocabulary ---------- */
  const STATUS = {
    'In progress': { label: 'In progress', group: 'Going on now', tone: 'var(--accent)' },
    'Done':        { label: 'Completed',   group: 'Completed',    tone: 'var(--ok)' },
    'Not started': { label: 'Not started', group: 'To do',        tone: 'var(--idle)' },
    'On hold':     { label: 'On hold',     group: 'On hold',      tone: 'var(--warn)' },
  };
  const STATUS_ORDER = ['In progress', 'Done', 'Not started', 'On hold'];
  const tone = (status) => (STATUS[status] || STATUS['Not started']).tone;

  /** Where the admin session token is kept — per tab, cleared when it closes. */
  const ADMIN_KEY = 'cp-admin-token';

  /** Thumbnail URL, stamped so a replaced image is never served from cache. */
  const thumbUrl = (p) => `/api/projects/${p.id}/thumbnail?v=${p.thumbnailVersion || 0}`;

  /** Committee members arrive as {id, name, role, projectCount}. */
  const committeeNames = () => (state.meta.committee || []).map((m) => m.name);

  /**
   * Record completeness — the share of reporting fields that have something in
   * them. The field list comes from /api/meta, so this mirrors exactly what the
   * server stores; recomputing here just makes the bar move as you type.
   */
  function completion(p) {
    const fields = state.meta.progressFields || [];
    if (!fields.length) {
      return { filled: 0, total: 0, percent: p.progress || 0 };
    }
    const filled = fields.reduce((n, key) => {
      const done = key === 'assignees'
        ? (p.assignees || []).length > 0
        : String(p[key] ?? '').trim() !== '';
      return n + (done ? 1 : 0);
    }, 0);
    return { filled, total: fields.length, percent: Math.round((filled / fields.length) * 100) };
  }

  /* ---------- form layout ---------- */
  const SECTIONS = [
    ['The basics', [
      ['type',      'Project type',    'select'],
      ['category',  'Project category', 'datalist'],
      ['startDate', 'Start date',      'date'],
      ['dueDate',   'End date',        'date'],
      ['duration',  'Project duration', 'text', 'How long it runs, e.g. 4 hours'],
      ['venue',     'Venue',           'text', 'Where it happens'],
    ]],
    ['Who is running it', [
      ['chair',         'Project chairman(s)', 'members', 'Pick from the committee, or type a name'],
      ['secretary',     'Project secretary(s)', 'members', 'Pick from the committee, or type a name'],
      ['treasurer',     'Project treasurer(s)', 'members', 'Pick from the committee, or type a name'],
      ['participation', 'Project participation', 'text', 'No. of club members taking part'],
    ]],
    ['Impact & reporting', [
      ['beneficiaries', 'No. of beneficiaries', 'text', 'How many people benefited'],
      ['serviceHours',  'Service hours',        'text', 'Total hours across all members'],
      ['projectValue',  'Project value',        'text', 'Total value in LKR'],
      ['funds',         'Mode of funds raised', 'text', 'e.g. Sponsorships, stall sales'],
      ['community',     'Benefiting community', 'text', 'Who this helps'],
      ['collection',    'Mode of data collection', 'text', 'e.g. Forms, headcount, photos'],
      ['need',          'Identified community need', 'area', 'What problem does this project answer?'],
      ['opportunity',   'Service opportunity',  'area', 'What did members get to do?'],
    ]],
    ['Guests & notes', [
      ['chiefGuest',  'Chief guest(s)', 'names', 'Type a name and press Enter'],
      ['otherGuests', 'Other guests',   'names', 'Type a name and press Enter'],
      ['note',        'Special note',  'area', 'Anything the next person should know'],
    ]],
  ];

  /* ---------- app state ---------- */
  const state = {
    projects: [],
    stats: null,
    meta: { types: [], categories: [], statuses: STATUS_ORDER, committee: [], progressFields: [] },
    query: '',
    filter: 'all',
    current: null,
    loading: true,
    admin: false,
  };

  /* ============================================================
     API
     ============================================================ */
  const api = {
    async req(path, options = {}) {
      const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
      // Multipart uploads must set their own boundary, so let the browser do it.
      if (options.body instanceof FormData) {
        delete headers['Content-Type'];
      }
      const token = sessionStorage.getItem(ADMIN_KEY);
      if (token) {
        headers['X-Admin-Token'] = token;
      }
      const res = await fetch(`/api${path}`, { ...options, headers });
      if (!res.ok) {
        let message = `Request failed (${res.status})`;
        try { message = (await res.json()).message || message; } catch { /* not json */ }
        throw new Error(message);
      }
      if (res.status === 204) return null;
      const type = res.headers.get('content-type') || '';
      return type.includes('application/json') ? res.json() : res.text();
    },
    meta:    ()        => api.req('/meta'),
    committee:    ()      => api.req('/committee'),
    catalog:        ()        => api.req('/catalog'),
    addCatalog:     (kind, label) => api.req(`/catalog/${kind}`, { method: 'POST', body: JSON.stringify({ label }) }),
    removeCatalog:  (id)      => api.req(`/catalog/${id}`, { method: 'DELETE' }),
    adminLogin:   (password) => api.req('/admin/login', { method: 'POST', body: JSON.stringify({ password }) }),
    adminLogout:  ()      => api.req('/admin/logout', { method: 'POST' }),
    adminSession: ()      => api.req('/admin/session'),
    addMember:    (name)  => api.req('/committee', { method: 'POST', body: JSON.stringify({ name }) }),
    removeMember: (id)    => api.req(`/committee/${id}`, { method: 'DELETE' }),
    list:    (q)       => api.req(`/projects${q ? `?q=${encodeURIComponent(q)}` : ''}`),
    get:     (id)      => api.req(`/projects/${id}`),
    stats:   ()        => api.req('/projects/stats'),
    create:  (body)    => api.req('/projects', { method: 'POST', body: JSON.stringify(body || {}) }),
    update:  (id, body) => api.req(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove:  (id)      => api.req(`/projects/${id}`, { method: 'DELETE' }),
    exportTsv: ()      => api.req('/projects/export.tsv'),
  };

  /* ============================================================
     Feedback: toast + save pill
     ============================================================ */
  let toastTimer;
  function toast(message, kind = '') {
    const el = $('#toast');
    el.textContent = message;
    el.className = `toast is-on${kind ? ` is-${kind}` : ''}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('is-on'), 2400);
  }

  /**
   * Stands in for window.confirm(), which some browsers suppress entirely (the
   * page then reads the refusal as "cancel" and nothing happens) and none of
   * them let you style. Resolves true when the user confirms.
   */
  function confirmAction({ title, text, note = '', confirmLabel = 'Remove' }) {
    const dialog = $('#confirmModal');
    $('#confirmTitle').textContent = title;
    $('#confirmText').textContent = text;
    $('#confirmNote').textContent = note;
    $('#confirmNote').hidden = !note;
    $('#confirmOk').textContent = confirmLabel;
    dialog.returnValue = '';

    return new Promise((resolve) => {
      dialog.addEventListener('close', function done() {
        dialog.removeEventListener('close', done);
        resolve(dialog.returnValue === 'ok');
      });
      dialog.showModal();
    });
  }

  /** Same dialog, one button — for telling rather than asking. */
  function notice({ title, text, label = 'Got it' }) {
    const dialog = $('#confirmModal');
    $('#confirmTitle').textContent = title;
    $('#confirmText').textContent = text;
    $('#confirmNote').hidden = true;
    $('#confirmCancel').hidden = true;
    $('#confirmOk').textContent = label;
    $('#confirmOk').className = 'btn btn--primary';

    return new Promise((resolve) => {
      dialog.addEventListener('close', function done() {
        dialog.removeEventListener('close', done);
        // put the dialog back the way confirmAction expects it
        $('#confirmCancel').hidden = false;
        $('#confirmOk').className = 'btn btn--solid-danger';
        resolve();
      });
      dialog.showModal();
    });
  }

  /** The message non-admins get when they reach for a delete button. */
  const denied = (what) => notice({
    title: 'Admins only',
    text: `Only admins can delete a ${what}. Ask a club admin if this needs to go.`,
  });

  let pillTimer;
  function pill(text, kind) {
    const el = $('#savePill');
    el.textContent = text;
    el.className = `savepill is-on${kind ? ` is-${kind}` : ''}`;
    clearTimeout(pillTimer);
    if (kind !== 'saving') {
      pillTimer = setTimeout(() => el.classList.remove('is-on'), 1800);
    }
  }

  /* ============================================================
     Theme
     ============================================================ */
  function initTheme() {
    const saved = localStorage.getItem('cp-theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.dataset.theme = saved || (prefersDark ? 'dark' : 'light');
  }
  function toggleTheme() {
    const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    localStorage.setItem('cp-theme', next);
  }

  /* ============================================================
     HOME — stats, filters, board
     ============================================================ */
  function renderStats() {
    const s = state.stats;
    if (!s) return;
    const tiles = [
      { label: 'Total projects', value: s.total, meta: `${s.notStarted} not started yet`, tone: 'var(--accent)' },
      { label: 'In progress',    value: s.inProgress, meta: s.onHold ? `${s.onHold} on hold` : 'Moving along', tone: 'var(--accent-2)' },
      { label: 'Completed',      value: s.completed, meta: s.total ? `${Math.round((s.completed / s.total) * 100)}% of the year` : '—', tone: 'var(--ok)' },
      {
        label: s.overdue ? 'Needs attention' : 'Records filled',
        value: s.overdue ? s.overdue : s.averageProgress,
        suffix: s.overdue ? '' : '%',
        meta: s.overdue ? 'past their due date' : 'average completeness',
        tone: s.overdue ? 'var(--danger)' : 'var(--warn)',
      },
    ];
    $('#stats').innerHTML = tiles.map((t) => `
      <div class="stat" style="--tone:${t.tone}">
        <div class="stat__label">${esc(t.label)}</div>
        <div class="stat__value mono">${t.value}${t.suffix ? `<small>${t.suffix}</small>` : ''}</div>
        <div class="stat__meta">${esc(t.meta)}</div>
      </div>`).join('');
  }

  function renderFilters() {
    const counts = STATUS_ORDER.reduce((acc, k) => {
      acc[k] = state.projects.filter((p) => p.status === k).length;
      return acc;
    }, {});
    const chips = [
      { key: 'all', label: 'All', n: state.projects.length, tone: 'var(--accent)' },
      ...STATUS_ORDER.map((k) => ({ key: k, label: STATUS[k].label, n: counts[k], tone: STATUS[k].tone })),
    ];
    $('#filters').innerHTML = chips.map((c) => `
      <button class="chip${state.filter === c.key ? ' is-on' : ''}" data-filter="${esc(c.key)}" style="--tone:${c.tone}" type="button">
        <span class="dot"></span>${esc(c.label)}<span class="n">${c.n}</span>
      </button>`).join('');

    $$('#filters .chip').forEach((chip) => {
      chip.onclick = () => {
        state.filter = chip.dataset.filter;
        renderFilters();
        renderBoard();
      };
    });
  }

  function dueLabel(p) {
    if (p.status === 'Done') return { text: p.dueDate ? `Held ${fmtDate(p.dueDate)}` : 'Completed', cls: '' };
    if (!p.dueDate) return { text: 'No date set', cls: '' };
    const n = p.daysToDue;
    if (n === null || n === undefined) return { text: fmtDate(p.dueDate), cls: '' };
    if (n < 0) return { text: `Overdue · ${fmtDate(p.dueDate)}`, cls: 'is-over' };
    if (n === 0) return { text: 'Due today', cls: 'is-over' };
    if (n <= 14) return { text: `Due in ${n} day${n === 1 ? '' : 's'}`, cls: 'is-soon' };
    return { text: `Due ${fmtDate(p.dueDate)}`, cls: '' };
  }

  function cardHtml(p, index) {
    const d = dueLabel(p);
    const people = p.assignees || [];
    const shown = people.slice(0, 3);
    const doneTasks = (p.tasks || []).filter((t) => t.done).length;

    return `
      <button class="card" type="button" data-id="${esc(p.id)}"
              style="--tone:${tone(p.status)};animation-delay:${Math.min(index, 12) * 35}ms">
        ${p.hasThumbnail ? `<span class="card__cover"><img src="${thumbUrl(p)}" alt="" loading="lazy"></span>` : ''}
        <div class="card__body">
          <div class="card__top">
            <span class="pill">${esc(p.category || 'Uncategorised')}</span>
            ${p.type && p.type !== 'Club Project' ? `<span class="pill pill--tone" style="--tone:var(--accent-2)">${esc(p.type)}</span>` : ''}
          </div>
          <div class="card__name">${esc(p.name || 'Untitled project')}</div>
          <div class="card__meta">
            <span class="${d.cls}">
              <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15.5" rx="3"/><path d="M3.5 9.5h17M8 3.5v3M16 3.5v3"/></svg>
              ${esc(d.text)}
            </span>
            ${p.venue ? `<span><svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21s6.5-5.6 6.5-10.3A6.5 6.5 0 0 0 5.5 10.7C5.5 15.4 12 21 12 21Z"/><circle cx="12" cy="10.5" r="2.3"/></svg>${esc(p.venue)}</span>` : ''}
            ${p.tasks && p.tasks.length ? `<span><svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="m4.5 12.5 3.5 3.5 11-11"/></svg>${doneTasks}/${p.tasks.length}</span>` : ''}
          </div>
        </div>
        <div class="card__foot">
          <span class="avatars">
            ${shown.map((n) => `<span class="avatar" title="${esc(n)}">${esc(initials(n).toUpperCase())}</span>`).join('')}
            ${people.length > 3 ? `<span class="avatar avatar--more">+${people.length - 3}</span>` : ''}
          </span>
          <span class="meter"><i data-width="${p.progress || 0}"></i></span>
          <span class="card__pct mono">${p.progress || 0}%</span>
        </div>
      </button>`;
  }

  function renderBoard() {
    const board = $('#board');

    if (state.loading) {
      board.innerHTML = `<div class="cards">${'<div class="skeleton"></div>'.repeat(6)}</div>`;
      return;
    }

    const visible = state.filter === 'all'
      ? STATUS_ORDER
      : STATUS_ORDER.filter((k) => k === state.filter);

    let index = 0;
    let html = visible.map((key) => {
      const items = state.projects.filter((p) => p.status === key);
      if (!items.length && state.filter === 'all' && key === 'On hold') return '';

      const cards = items.length
        ? items.map((p) => cardHtml(p, index++)).join('')
        : `<div class="emptybox">Nothing here${state.query ? ' matches your search' : ' yet'}.</div>`;

      return `
        <section class="group">
          <div class="group__head" style="--tone:${STATUS[key].tone}">
            <span class="group__dot"></span>
            <span class="group__name">${esc(STATUS[key].group)}</span>
            <span class="group__count">${items.length}</span>
            <span class="group__rule"></span>
          </div>
          <div class="cards">${cards}</div>
        </section>`;
    }).join('');

    if (!state.projects.length && state.query) {
      html = `<div class="emptybox">No projects match “${esc(state.query)}”.</div>`;
    }

    board.innerHTML = html + `
      <button class="addcard" id="addCard" type="button">
        <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5.5v13M5.5 12h13"/></svg>
        Add another project
      </button>`;

    $$('#board .card').forEach((c) => { c.onclick = () => go(`#/p/${c.dataset.id}`); });
    $('#addCard').onclick = createProject;

    requestAnimationFrame(() => {
      $$('#board .meter i').forEach((bar) => { bar.style.width = `${bar.dataset.width}%`; });
    });
  }

  function renderHeroLine() {
    const s = state.stats;
    if (!s) return;
    const open = s.total - s.completed;
    $('#heroLine').textContent = s.total
      ? `${s.total} projects tracked · ${open} still open · ${s.completed} wrapped up.`
      : 'No projects yet — create your first one to get started.';
  }

  function renderCommittee() {
    const list = state.meta.committee || [];
    $('#memberCount').textContent = `${list.length} member${list.length === 1 ? '' : 's'}`;

    $('#members').innerHTML = list.length ? list.map((m) => `
      <span class="person" data-id="${esc(m.id)}">
        <span class="avatar">${esc(initials(m.name).toUpperCase())}</span>
        ${esc(m.name)}
        ${m.projectCount ? `<span class="person__count" title="On ${m.projectCount} project${m.projectCount === 1 ? '' : 's'}">${m.projectCount}</span>` : ''}
        <button class="person__x" type="button" data-remove="${esc(m.id)}" aria-label="Remove ${esc(m.name)} from the committee">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
        </button>
      </span>`).join('')
      : `<div class="emptybox">No one on the committee yet — add your first member.</div>`;

    $$('#members [data-remove]').forEach((btn) => {
      btn.onclick = () => removeMember(btn.dataset.remove);
    });
  }

  async function reloadCommittee() {
    state.meta.committee = await api.committee();
    renderCommittee();
  }

  function openMemberForm(open) {
    $('#memberForm').hidden = !open;
    $('#addMemberBtn').hidden = open;
    if (open) {
      $('#memberName').value = '';
      $('#memberName').focus();
    }
  }

  async function addMember(event) {
    event.preventDefault();
    const name = $('#memberName').value.trim();
    if (!name) return;
    try {
      const added = await api.addMember(name);
      await reloadCommittee();
      openMemberForm(false);
      toast(`${added.name} added to the committee`);
    } catch (err) {
      toast(err.message, 'error');
      $('#memberName').focus();
    }
  }

  async function removeMember(id) {
    const member = (state.meta.committee || []).find((m) => m.id === id);
    if (!member) return;
    if (!state.admin) {
      await denied('committee member');
      return;
    }

    // Their name stays on any project they already ran, so say so up front.
    const n = member.projectCount;
    const ok = await confirmAction({
      title: `Remove ${member.name}?`,
      text: 'They will no longer appear as a choice when assigning people to projects.',
      note: n
        ? `${member.name} is assigned to ${n} project${n === 1 ? '' : 's'}. Those keep their name — nothing is lost from the record.`
        : '',
      confirmLabel: 'Remove member',
    });
    if (!ok) return;

    try {
      await api.removeMember(id);
      await reloadCommittee();
      toast(`${member.name} removed`);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  /* ============================================================
     DETAIL
     ============================================================ */
  /* ============================================================
     Combo box — a dropdown we can actually style
     ------------------------------------------------------------
     Native <select> popups and <datalist> suggestions are drawn by the browser
     and ignore CSS entirely, so both are rebuilt here. A hidden input carries
     the value, which keeps the existing autosave wiring working untouched.
     ============================================================ */
  const CHEVRON = '<svg class="i combo__chev" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 10 5 5 5-5"/></svg>';
  const TICK = '<svg class="i combo__tick" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5"/></svg>';

  function optionRow(label, selected) {
    return `<button class="combo__opt${selected ? ' is-on' : ''}" type="button" role="option"
              aria-selected="${selected}" data-value="${esc(label)}">
        <span class="combo__opt-label">${esc(label)}</span>
        ${TICK}
      </button>`;
  }

  function comboHtml(key, value, options, { editable = false, placeholder = '' } = {}) {
    const list = [...options];
    // An option an admin has since removed still has to appear on projects
    // using it, or selecting anything else would quietly rewrite the record.
    if (value && !list.includes(value)) list.push(value);
    const searchable = editable || list.length > 6;

    return `
      <div class="combo${editable ? ' combo--editable' : ''}" data-combo="${esc(key)}" data-placeholder="${esc(placeholder)}">
        <input type="hidden" data-key="${esc(key)}" value="${esc(value)}">
        <button class="combo__field" type="button" aria-haspopup="listbox" aria-expanded="false">
          <span class="combo__value${value ? '' : ' is-empty'}">${esc(value || placeholder)}</span>
          ${CHEVRON}
        </button>
        <div class="combo__pop" hidden>
          ${searchable ? `
            <label class="combo__search">
              <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="m20 20-3.6-3.6"/></svg>
              <input type="text" placeholder="${editable ? 'Search or type your own' : 'Search'}" autocomplete="off" spellcheck="false">
            </label>` : ''}
          <div class="combo__list" role="listbox">
            ${list.map((o) => optionRow(o, o === value)).join('')}
          </div>
          ${editable ? '<div class="combo__hint">Type anything and press <kbd>Enter</kbd> to use it</div>' : ''}
        </div>
      </div>`;
  }

  /** Wires up every combo inside a freshly rendered container. */
  function initCombos(root) {
    $$('.combo', root).forEach((combo) => {
      const hidden = $('input[type="hidden"]', combo);
      const field = $('.combo__field', combo);
      const pop = $('.combo__pop', combo);
      const label = $('.combo__value', combo);
      const search = $('.combo__search input', combo);
      const list = $('.combo__list', combo);
      const editable = combo.classList.contains('combo--editable');

      const options = () => $$('.combo__opt', list).filter((o) => !o.hidden);

      const commit = (value) => {
        hidden.value = value;
        label.textContent = value || combo.dataset.placeholder;
        label.classList.toggle('is-empty', !value);
        $$('.combo__opt', list).forEach((o) => {
          const on = o.dataset.value === value;
          o.classList.toggle('is-on', on);
          o.setAttribute('aria-selected', on);
        });
        // The autosave layer listens for this on [data-key].
        hidden.dispatchEvent(new Event('input', { bubbles: true }));
        close();
      };

      const highlight = (el) => {
        $$('.combo__opt', list).forEach((o) => o.classList.remove('is-active'));
        if (el) {
          el.classList.add('is-active');
          el.scrollIntoView({ block: 'nearest' });
        }
      };

      function open() {
        closeAllCombos(combo);
        pop.hidden = false;
        combo.classList.add('is-open');
        field.setAttribute('aria-expanded', 'true');
        if (search) {
          search.value = '';
          filter('');
          search.focus();
        }
        highlight($('.combo__opt.is-on', list) || options()[0]);
      }

      function close() {
        pop.hidden = true;
        combo.classList.remove('is-open');
        field.setAttribute('aria-expanded', 'false');
      }
      combo._close = close;

      function filter(term) {
        const q = term.trim().toLowerCase();
        let shown = 0;
        $$('.combo__opt', list).forEach((o) => {
          const hit = !q || o.dataset.value.toLowerCase().includes(q);
          o.hidden = !hit;
          if (hit) shown += 1;
        });

        // Offer the typed text itself when it is genuinely new.
        let custom = $('.combo__custom', pop);
        const exact = $$('.combo__opt', list).some((o) => o.dataset.value.toLowerCase() === q);
        if (editable && q && !exact) {
          if (!custom) {
            custom = document.createElement('button');
            custom.type = 'button';
            custom.className = 'combo__opt combo__custom';
            list.prepend(custom);
          }
          custom.dataset.value = term.trim();
          custom.innerHTML = `<span class="combo__opt-label">Use “${esc(term.trim())}”</span>`;
          custom.hidden = false;
          shown += 1;
        } else if (custom) {
          custom.remove();
        }

        let empty = $('.combo__empty', pop);
        if (!shown) {
          if (!empty) {
            empty = document.createElement('div');
            empty.className = 'combo__empty';
            empty.textContent = 'Nothing matches';
            list.after(empty);
          }
        } else if (empty) {
          empty.remove();
        }
        highlight(options()[0]);
      }

      field.onclick = () => (pop.hidden ? open() : close());

      list.onclick = (e) => {
        const opt = e.target.closest('.combo__opt');
        if (opt) commit(opt.dataset.value);
      };

      if (search) {
        search.addEventListener('input', () => filter(search.value));
      }

      const onKeys = (e) => {
        if (e.key === 'Escape') { e.stopPropagation(); close(); field.focus(); return; }
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
          e.preventDefault();
          if (pop.hidden) { open(); return; }
          const items = options();
          const at = items.indexOf($('.combo__opt.is-active', list));
          const next = e.key === 'ArrowDown'
            ? Math.min(items.length - 1, at + 1)
            : Math.max(0, at - 1);
          highlight(items[next]);
          return;
        }
        if (e.key === 'Enter') {
          e.preventDefault();
          if (pop.hidden) { open(); return; }
          const active = $('.combo__opt.is-active', list) || options()[0];
          if (active) commit(active.dataset.value);
          else if (editable && search && search.value.trim()) commit(search.value.trim());
        }
      };
      field.addEventListener('keydown', onKeys);
      if (search) search.addEventListener('keydown', onKeys);
    });
  }

  function closeAllCombos(except) {
    $$('.combo.is-open').forEach((c) => { if (c !== except && c._close) c._close(); });
  }

  /* ============================================================
     Name chips — several names in one field
     ------------------------------------------------------------
     Used for the three committee roles and for guests. The value is still the
     comma-separated string the database and the spreadsheet export expect; only
     the editing experience changes.
     ============================================================ */
  const XMARK = '<svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>';

  const splitNames = (s) => String(s ?? '').split(',').map((n) => n.trim()).filter(Boolean);

  function chipHtml(name, known) {
    return `<span class="chip-name${known ? ' is-member' : ''}" data-name="${esc(name)}">
        ${known ? `<span class="avatar">${esc(initials(name).toUpperCase())}</span>` : ''}
        <span class="chip-name__text">${esc(name)}</span>
        <button class="chip-name__x" type="button" data-drop="${esc(name)}" aria-label="Remove ${esc(name)}">${XMARK}</button>
      </span>`;
  }

  function namesHtml(key, value, { suggest = false, placeholder = '' } = {}) {
    const names = splitNames(value);
    const roster = suggest ? committeeNames() : [];
    return `
      <div class="names" data-names="${esc(key)}" data-suggest="${suggest}">
        <input type="hidden" data-key="${esc(key)}" value="${esc(names.join(', '))}">
        <div class="names__box">
          <span class="names__chips">${names.map((n) => chipHtml(n, roster.includes(n))).join('')}</span>
          <input class="names__input" type="text" placeholder="${esc(placeholder)}" autocomplete="off" spellcheck="false" aria-label="Add a name">
        </div>
        ${suggest ? '<div class="names__pop" hidden role="listbox"></div>' : ''}
      </div>`;
  }

  function initNames(root) {
    $$('.names', root).forEach((box) => {
      const hidden = $('input[type="hidden"]', box);
      const chips = $('.names__chips', box);
      const entry = $('.names__input', box);
      const pop = $('.names__pop', box);
      const suggest = box.dataset.suggest === 'true';

      const current = () => splitNames(hidden.value);

      const paint = () => {
        const roster = suggest ? committeeNames() : [];
        chips.innerHTML = current().map((n) => chipHtml(n, roster.includes(n))).join('');
        $$('[data-drop]', chips).forEach((btn) => {
          btn.onclick = (e) => { e.stopPropagation(); drop(btn.dataset.drop); };
        });
      };

      const write = (names) => {
        hidden.value = names.join(', ');
        paint();
        hidden.dispatchEvent(new Event('input', { bubbles: true }));
      };

      const add = (raw) => {
        const name = String(raw || '').trim().replace(/\s+/g, ' ');
        if (!name) return;
        const names = current();
        // Same person twice helps nobody, whatever the casing.
        if (names.some((n) => n.toLowerCase() === name.toLowerCase())) {
          entry.value = '';
          closePop();
          return;
        }
        write([...names, name]);
        entry.value = '';
        closePop();
      };

      const drop = (name) => write(current().filter((n) => n !== name));

      function closePop() {
        if (pop) { pop.hidden = true; pop.innerHTML = ''; }
      }

      function openPop(term) {
        if (!pop) return;
        const taken = current().map((n) => n.toLowerCase());
        const q = term.trim().toLowerCase();
        const matches = committeeNames()
          .filter((n) => !taken.includes(n.toLowerCase()))
          .filter((n) => !q || n.toLowerCase().includes(q))
          .slice(0, 8);

        if (!matches.length) { closePop(); return; }
        pop.innerHTML = matches.map((n) => `
          <button class="names__opt" type="button" role="option" data-pick="${esc(n)}">
            <span class="avatar">${esc(initials(n).toUpperCase())}</span>${esc(n)}
          </button>`).join('');
        pop.hidden = false;
        $$('[data-pick]', pop).forEach((btn) => {
          btn.onmousedown = (e) => { e.preventDefault(); add(btn.dataset.pick); };
        });
      }

      entry.addEventListener('focus', () => openPop(entry.value));
      entry.addEventListener('input', () => openPop(entry.value));
      entry.addEventListener('blur', () => setTimeout(closePop, 120));

      entry.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ',') {
          e.preventDefault();
          add(entry.value);
        } else if (e.key === 'Backspace' && !entry.value) {
          const names = current();
          if (names.length) write(names.slice(0, -1));
        } else if (e.key === 'Escape') {
          e.stopPropagation();
          closePop();
        }
      });

      // Losing focus should not quietly discard what was typed.
      entry.addEventListener('blur', () => { if (entry.value.trim()) add(entry.value); });

      $('.names__box', box).addEventListener('click', () => entry.focus());
      paint();
    });
  }

  function fieldHtml(def) {
    const [key, label, type, hint = ''] = def;
    const value = state.current[key] ?? '';
    let control;

    if (type === 'select') {
      control = comboHtml(key, value, state.meta.types.length ? state.meta.types : ['Club Project'],
        { placeholder: 'Choose a type' });
    } else if (type === 'datalist') {
      control = comboHtml(key, value, state.meta.categories || [],
        { editable: true, placeholder: 'Choose or type your own' });
    } else if (type === 'members') {
      control = namesHtml(key, value, { suggest: true, placeholder: 'Pick or type a name…' });
    } else if (type === 'names') {
      control = namesHtml(key, value, { placeholder: 'Type a name and press Enter' });
    } else if (type === 'area') {
      control = `<textarea data-key="${key}" placeholder="${esc(hint)}">${esc(value)}</textarea>`;
    } else {
      control = `<input data-key="${key}" type="${type === 'date' ? 'date' : 'text'}" value="${esc(value)}" placeholder="${esc(hint)}">`;
    }

    return `
      <div class="field${type === 'area' ? ' field--wide' : ''}">
        <label>${esc(label)}${hint && type !== 'area' ? ` <span class="hint">— ${esc(hint)}</span>` : ''}</label>
        ${control}
      </div>`;
  }

  function renderDetail() {
    const p = state.current;

    $('#pName').value = p.name || '';
    autoGrow($('#pName'));
    renderThumb();
    renderProgress();
    renderBadge();
    renderStatusSegment();

    // Anyone already assigned who has since left the committee still has to be
    // shown — the save rebuilds the list from these buttons, so leaving them out
    // would quietly wipe them off the project.
    const roster = committeeNames();
    const former = (p.assignees || []).filter((n) => !roster.includes(n));

    let html = `
      <section class="section">
        <div class="section__title"><span class="num">1</span><h2>Assigned to</h2></div>
        <div class="picker" id="picker">
          ${[...roster, ...former].map((n) => {
            const isPast = former.includes(n);
            return `<button type="button" data-name="${esc(n)}"
              class="${(p.assignees || []).includes(n) ? 'is-on' : ''}${isPast ? ' is-past' : ''}"
              ${isPast ? 'title="No longer on the committee"' : ''}>
              <span class="avatar">${esc(initials(n).toUpperCase())}</span>${esc(n)}
            </button>`;
          }).join('')}
        </div>
      </section>`;

    SECTIONS.forEach(([title, fields], i) => {
      html += `
        <section class="section">
          <div class="section__title"><span class="num">${i + 2}</span><h2>${esc(title)}</h2></div>
          <div class="fields">${fields.map(fieldHtml).join('')}</div>
        </section>`;
    });

    html += `
      <section class="section">
        <div class="section__title"><span class="num">${SECTIONS.length + 2}</span><h2>Checklist</h2></div>
        <div class="checklist__progress" id="taskSummary"></div>
        <div class="checklist" id="checklist"></div>
        <button class="btn btn--sm" id="addTask" type="button" style="margin-top:14px">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5.5v13M5.5 12h13"/></svg>
          Add a step
        </button>
      </section>`;

    $('#detailBody').innerHTML = html;
    initCombos($('#detailBody'));
    initNames($('#detailBody'));

    $$('#picker button').forEach((btn) => {
      btn.onclick = () => {
        btn.classList.toggle('is-on');
        state.current.assignees = $$('#picker button.is-on').map((b) => b.dataset.name);
        renderProgress();
        queueSave();
      };
    });

    $$('#detailBody [data-key]').forEach((el) => {
      const event = el.tagName === 'SELECT' ? 'change' : 'input';
      el.addEventListener(event, () => {
        state.current[el.dataset.key] = el.value;
        renderProgress();
        queueSave();
      });
    });

    $('#addTask').onclick = () => {
      state.current.tasks = state.current.tasks || [];
      state.current.tasks.push({ id: null, title: '', done: false });
      renderChecklist(true);
      queueSave();
    };

    renderChecklist();
  }

  /** Recomputes the completeness bar from what is currently in the form. */
  function renderProgress() {
    const c = completion(state.current);
    state.current.progress = c.percent;
    $('#pPct').textContent = `${c.percent}%`;
    $('#pFill').style.width = `${c.percent}%`;
    $('#pCount').textContent = c.total
      ? `${c.filled} of ${c.total} details filled in`
      : 'Fill in the details below to move this along';
  }

  function renderBadge() {
    const p = state.current;
    const badge = $('#pBadge');
    badge.style.setProperty('--tone', tone(p.status));
    badge.textContent = (STATUS[p.status] || STATUS['Not started']).label;
  }

  function renderStatusSegment() {
    const seg = $('#statusSeg');
    seg.innerHTML = STATUS_ORDER.map((k) => `
      <button type="button" data-status="${esc(k)}" style="--tone:${STATUS[k].tone}"
              class="${state.current.status === k ? 'is-on' : ''}">
        <span class="dot"></span>${esc(STATUS[k].label)}
      </button>`).join('');

    $$('#statusSeg button').forEach((btn) => {
      btn.onclick = () => {
        // Status is the committee's call; the bar stays a measure of paperwork.
        state.current.status = btn.dataset.status;
        renderStatusSegment();
        renderBadge();
        queueSave();
      };
    });
  }

  function renderChecklist(focusLast) {
    const tasks = state.current.tasks || [];
    const box = $('#checklist');
    const done = tasks.filter((t) => t.done).length;

    $('#taskSummary').textContent = tasks.length
      ? `${done} of ${tasks.length} steps done`
      : 'Break the project into small steps so nothing gets missed.';

    box.innerHTML = tasks.length ? tasks.map((t, i) => `
      <div class="check${t.done ? ' is-done' : ''}">
        <input type="checkbox" data-done="${i}" ${t.done ? 'checked' : ''} aria-label="Mark step done">
        <input type="text" data-title="${i}" value="${esc(t.title)}" placeholder="What needs doing?">
        <button class="check__x" type="button" data-remove="${i}" aria-label="Remove step">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
        </button>
      </div>`).join('') : '';

    $$('[data-done]', box).forEach((el) => {
      el.onchange = () => { tasks[+el.dataset.done].done = el.checked; renderChecklist(); queueSave(); };
    });
    $$('[data-title]', box).forEach((el) => {
      el.oninput = () => { tasks[+el.dataset.title].title = el.value; queueSave(); };
    });
    $$('[data-remove]', box).forEach((el) => {
      el.onclick = () => { tasks.splice(+el.dataset.remove, 1); renderChecklist(); queueSave(); };
    });

    if (focusLast) box.querySelector('.check:last-child [data-title]')?.focus();
  }

  function autoGrow(el) {
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  }

  /* ============================================================
     Thumbnail
     ============================================================ */
  const THUMB_MAX_EDGE = 480;   // plenty for a 62px card tile on a retina screen
  const THUMB_QUALITY = 0.82;

  /**
   * Shrinks the chosen photo in the browser before it is sent. A 4 MB phone
   * snap becomes a ~40 KB square, which keeps the database small and the upload
   * quick. Falls back to the original file if anything here is unsupported.
   */
  async function shrinkImage(file) {
    if (file.type === 'image/gif') return file;   // resizing would kill the animation
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
      const scale = Math.min(1, THUMB_MAX_EDGE / Math.max(bitmap.width, bitmap.height));
      const w = Math.max(1, Math.round(bitmap.width * scale));
      const h = Math.max(1, Math.round(bitmap.height * scale));

      const canvas = document.createElement('canvas');
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(bitmap, 0, 0, w, h);
      bitmap.close?.();

      const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', THUMB_QUALITY));
      if (!blob) return file;
      // If the original was already smaller, keep it.
      return blob.size < file.size ? new File([blob], 'thumbnail.jpg', { type: 'image/jpeg' }) : file;
    } catch {
      return file;
    }
  }

  function renderThumb() {
    const p = state.current;
    const box = $('#thumb');
    const img = $('#thumbImg');

    box.classList.toggle('has-image', !!p.hasThumbnail);
    $('#thumbRemove').hidden = !p.hasThumbnail;
    $('#thumbEmpty').hidden = !!p.hasThumbnail;
    img.hidden = !p.hasThumbnail;
    if (p.hasThumbnail) {
      img.src = thumbUrl(p);
      img.alt = `Photo for ${p.name || 'this project'}`;
      $('#thumbBtn').setAttribute('aria-label', 'Change the photo for this project');
    } else {
      img.removeAttribute('src');
      $('#thumbBtn').setAttribute('aria-label', 'Add a photo for this project');
    }
  }

  async function uploadThumb(file) {
    if (!file || !state.current) return;
    if (!file.type.startsWith('image/')) {
      toast('That file is not an image', 'error');
      return;
    }

    const box = $('#thumb');
    box.classList.add('is-busy');
    try {
      const body = new FormData();
      body.append('file', await shrinkImage(file));
      const saved = await api.req(`/projects/${state.current.id}/thumbnail`, { method: 'POST', body });
      state.current.hasThumbnail = saved.hasThumbnail;
      state.current.thumbnailVersion = saved.thumbnailVersion;
      renderThumb();
      toast('Photo added');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      box.classList.remove('is-busy');
    }
  }

  async function removeThumb() {
    if (!state.current?.hasThumbnail) return;
    const ok = await confirmAction({
      title: 'Remove this photo?',
      text: 'The project keeps everything else — only the picture goes.',
      confirmLabel: 'Remove photo',
    });
    if (!ok) return;

    try {
      await api.req(`/projects/${state.current.id}/thumbnail`, { method: 'DELETE' });
      state.current.hasThumbnail = false;
      state.current.thumbnailVersion = null;
      renderThumb();
      toast('Photo removed');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  /* ============================================================
     Saving
     ============================================================ */
  let saveTimer;
  let dirty = false;

  /** Schedules a save; repeated edits collapse into one request. */
  function queueSave() {
    dirty = true;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(saveNow, 600);
  }

  /** Writes the open project immediately — used before navigating away. */
  async function saveNow() {
    clearTimeout(saveTimer);
    const project = state.current;
    if (!project || !dirty) return;
    dirty = false;

    pill('Saving…', 'saving');
    try {
      const saved = await api.update(project.id, project);
      // Keep the server's derived fields without disturbing what is on screen.
      project.daysToDue = saved.daysToDue;
      project.overdue = saved.overdue;
      project.updatedAt = saved.updatedAt;
      project.progress = saved.progress;
      // Newly added steps come back with a real id — remember it for the next save.
      (project.tasks || []).forEach((t, i) => { t.id = saved.tasks[i]?.id ?? t.id; });
      pill('Saved', 'saved');
    } catch (err) {
      dirty = true;
      pill('Not saved', 'error');
      toast(err.message, 'error');
    }
  }

  /* ============================================================
     Actions
     ============================================================ */
  async function refresh() {
    try {
      const [projects, stats] = await Promise.all([api.list(state.query), api.stats()]);
      state.projects = projects;
      state.stats = stats;
      state.loading = false;
      renderHeroLine();
      renderStats();
      renderFilters();
      renderBoard();
    } catch (err) {
      state.loading = false;
      $('#board').innerHTML = `<div class="emptybox">Could not reach the server — ${esc(err.message)}</div>`;
      toast('Could not load projects', 'error');
    }
  }

  async function createProject() {
    try {
      const p = await api.create({ name: '', status: 'Not started' });
      state.projects.push(p);
      go(`#/p/${p.id}`);
      setTimeout(() => $('#pName').focus(), 200);
      toast('New project created');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function deleteProject() {
    const p = state.current;
    if (!p) return;
    if (!state.admin) {
      await denied('project');
      return;
    }
    const ok = await confirmAction({
      title: `Delete “${p.name || 'this project'}”?`,
      text: 'The project and everything recorded against it will be removed. This cannot be undone.',
      confirmLabel: 'Delete project',
    });
    if (!ok) return;

    try {
      await api.remove(p.id);
      state.current = null;
      go('#/');
      toast('Project deleted');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function exportSheet() {
    try {
      const tsv = await api.exportTsv();
      await navigator.clipboard.writeText(tsv);
      toast('Copied — paste straight into your sheet');
    } catch {
      // Clipboard API needs a secure context; fall back to a file download.
      window.location.href = '/api/projects/export.tsv';
      toast('Downloading the spreadsheet file');
    }
  }

  /* ============================================================
     Admin
     ============================================================ */
  async function checkAdminSession() {
    if (!sessionStorage.getItem(ADMIN_KEY)) {
      state.admin = false;
      return;
    }
    try {
      const { signedIn } = await api.adminSession();
      state.admin = !!signedIn;
      if (!signedIn) sessionStorage.removeItem(ADMIN_KEY);
    } catch {
      state.admin = false;
    }
  }

  function openAdminLogin() {
    const dialog = $('#adminModal');
    $('#adminPassword').value = '';
    $('#adminError').hidden = true;
    dialog.showModal();
    $('#adminPassword').focus();
  }

  async function submitAdminLogin(event) {
    event.preventDefault();
    const password = $('#adminPassword').value;
    if (!password) return;
    try {
      const { token } = await api.adminLogin(password);
      sessionStorage.setItem(ADMIN_KEY, token);
      state.admin = true;
      $('#adminModal').close();
      toast('Signed in as admin');
      go('#/admin');
    } catch (err) {
      $('#adminError').textContent = err.message;
      $('#adminError').hidden = false;
      $('#adminPassword').select();
    }
  }

  async function adminSignOut() {
    try { await api.adminLogout(); } catch { /* the token dies either way */ }
    sessionStorage.removeItem(ADMIN_KEY);
    state.admin = false;
    toast('Signed out');
    go('#/');
  }

  async function renderAdmin() {
    const [projects, members, catalog] = await Promise.all([api.list(''), api.committee(), api.catalog()]);
    state.projects = projects;
    state.meta.committee = members;
    renderCatalog(catalog);

    $('#adminProjectCount').textContent = `${projects.length} project${projects.length === 1 ? '' : 's'}`;
    $('#adminMemberCount').textContent = `${members.length} member${members.length === 1 ? '' : 's'}`;

    $('#adminProjects').innerHTML = projects.map((p) => `
      <div class="adminrow">
        <span class="adminrow__thumb">${p.hasThumbnail ? `<img src="${thumbUrl(p)}" alt="">` : ''}</span>
        <span class="adminrow__main">
          <span class="adminrow__name">${esc(p.name || 'Untitled project')}</span>
          <span class="adminrow__meta">${esc(p.category || '—')} · ${esc((STATUS[p.status] || STATUS['Not started']).label)} · ${p.progress}%</span>
        </span>
        <button class="adminrow__x" type="button" data-project="${esc(p.id)}" aria-label="Delete ${esc(p.name || 'this project')}">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M4.5 7h15M9.5 7V5.4a1.4 1.4 0 0 1 1.4-1.4h2.2a1.4 1.4 0 0 1 1.4 1.4V7M6.5 7l.8 11.6A1.6 1.6 0 0 0 8.9 20h6.2a1.6 1.6 0 0 0 1.6-1.4L17.5 7"/></svg>
        </button>
      </div>`).join('') || '<div class="emptybox">No projects yet.</div>';

    $('#adminMembers').innerHTML = members.map((m) => `
      <span class="person" data-id="${esc(m.id)}">
        <span class="avatar">${esc(initials(m.name).toUpperCase())}</span>
        ${esc(m.name)}
        ${m.projectCount ? `<span class="person__count">${m.projectCount}</span>` : ''}
        <button class="person__x" type="button" data-remove="${esc(m.id)}" aria-label="Remove ${esc(m.name)} from the committee">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
        </button>
      </span>`).join('') || '<div class="emptybox">No committee members.</div>';

    $$('#adminProjects [data-project]').forEach((btn) => {
      btn.onclick = () => adminDeleteProject(btn.dataset.project);
    });
    $$('#adminMembers [data-remove]').forEach((btn) => {
      btn.onclick = async () => { await removeMember(btn.dataset.remove); renderAdmin(); };
    });
  }

  /** Renders the two pick-lists in the admin panel. */
  function renderCatalog(catalog) {
    const optionHtml = (item) => `
      <span class="opt" data-id="${esc(item.id)}">
        ${esc(item.label)}
        ${item.usageCount ? `<span class="opt__n" title="Used by ${item.usageCount} project${item.usageCount === 1 ? '' : 's'}">${item.usageCount}</span>` : ''}
        <button class="opt__x" type="button" data-catalog="${esc(item.id)}" data-label="${esc(item.label)}" data-used="${item.usageCount}" aria-label="Remove ${esc(item.label)}">
          <svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
        </button>
      </span>`;

    $('#typeList').innerHTML = catalog.types.map(optionHtml).join('');
    $('#categoryList').innerHTML = catalog.categories.map(optionHtml).join('');
    $('#typeCount').textContent = `${catalog.types.length} option${catalog.types.length === 1 ? '' : 's'}`;
    $('#categoryCount').textContent = `${catalog.categories.length} option${catalog.categories.length === 1 ? '' : 's'}`;

    $$('#viewAdmin [data-catalog]').forEach((btn) => {
      btn.onclick = () => removeCatalogItem(btn.dataset.catalog, btn.dataset.label, +btn.dataset.used);
    });
  }

  async function removeCatalogItem(id, label, used) {
    const ok = await confirmAction({
      title: `Remove “${label}”?`,
      text: 'It will no longer be offered on the project form.',
      note: used
        ? `${used} project${used === 1 ? ' is' : 's are'} using it. They keep the value — it just stops being an option for new work.`
        : '',
      confirmLabel: 'Remove option',
    });
    if (!ok) return;
    try {
      await api.removeCatalog(id);
      toast(`“${label}” removed`);
      await refreshMeta();
      renderAdmin();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function addCatalogItem(kind, input) {
    const label = input.value.trim();
    if (!label) return;
    try {
      await api.addCatalog(kind, label);
      input.value = '';
      toast(`“${label}” added`);
      await refreshMeta();
      renderAdmin();
    } catch (err) {
      toast(err.message, 'error');
      input.select();
    }
  }

  /** Keeps the project form's pick-lists in step after a structure change. */
  async function refreshMeta() {
    try { state.meta = await api.meta(); } catch { /* keep what we have */ }
  }

  async function adminDeleteProject(id) {
    const p = state.projects.find((x) => x.id === id);
    if (!p) return;
    const ok = await confirmAction({
      title: `Delete “${p.name || 'this project'}”?`,
      text: 'The project and everything recorded against it will be removed. This cannot be undone.',
      confirmLabel: 'Delete project',
    });
    if (!ok) return;
    try {
      await api.remove(id);
      toast('Project deleted');
      renderAdmin();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  /* ============================================================
     Routing (#/, #/p/<id> and #/admin)
     ============================================================ */
  function go(hash) {
    if (window.location.hash === hash) route();
    else window.location.hash = hash;
  }

  async function route() {
    const hash = window.location.hash;
    const match = hash.match(/^#\/p\/(.+)$/);

    // Never leave the screen with edits still sitting in the debounce window.
    await saveNow();

    const show = (id) => {
      ['viewHome', 'viewDetail', 'viewAdmin'].forEach((v) => {
        $(`#${v}`).classList.toggle('view--on', v === id);
      });
      $('#detailBar').hidden = id !== 'viewDetail';
      window.scrollTo({ top: 0, behavior: 'instant' });
    };

    // The unmarked door. Signed in it opens the panel; otherwise it asks.
    if (hash === '#/admin') {
      state.current = null;
      await checkAdminSession();
      if (!state.admin) {
        show('viewHome');
        refresh();
        openAdminLogin();
        return;
      }
      show('viewAdmin');
      renderAdmin();
      return;
    }

    if (!match) {
      state.current = null;
      show('viewHome');
      refresh();
      return;
    }

    try {
      state.current = await api.get(match[1]);
    } catch (err) {
      toast(err.message, 'error');
      go('#/');
      return;
    }

    show('viewDetail');
    renderDetail();
  }

  /* ============================================================
     Wiring
     ============================================================ */
  function bind() {
    $('#themeBtn').onclick = toggleTheme;
    $('#newBtn').onclick = createProject;
    $('#exportBtn').onclick = exportSheet;
    // The badge goes home normally. Five quick clicks opens the admin door —
    // unmarked on purpose, so it is out of the way of everyday use.
    let badgeClicks = 0;
    let badgeTimer;
    $('#brandBtn').onclick = () => {
      badgeClicks += 1;
      clearTimeout(badgeTimer);
      badgeTimer = setTimeout(() => { badgeClicks = 0; }, 1200);
      if (badgeClicks >= 5) {
        badgeClicks = 0;
        go('#/admin');
        return;
      }
      go('#/');
    };

    $('#adminBack').onclick = () => go('#/');
    $('#adminLogout').onclick = adminSignOut;
    $('#typeForm').addEventListener('submit', (e) => {
      e.preventDefault();
      addCatalogItem('types', $('#typeInput'));
    });
    $('#categoryForm').addEventListener('submit', (e) => {
      e.preventDefault();
      addCatalogItem('categories', $('#categoryInput'));
    });
    $('#adminForm').addEventListener('submit', submitAdminLogin);
    $('#adminCancel').onclick = () => $('#adminModal').close();
    $('#backBtn').onclick = () => go('#/');

    // photo tile: click to browse, or drop a file straight onto it
    $('#thumbBtn').onclick = () => $('#thumbInput').click();
    $('#thumbRemove').onclick = removeThumb;
    $('#thumbInput').addEventListener('change', (e) => {
      uploadThumb(e.target.files[0]);
      e.target.value = '';   // so picking the same file twice still fires
    });
    ['dragenter', 'dragover'].forEach((evt) => {
      $('#thumb').addEventListener(evt, (e) => { e.preventDefault(); $('#thumb').classList.add('is-over'); });
    });
    ['dragleave', 'drop'].forEach((evt) => {
      $('#thumb').addEventListener(evt, () => $('#thumb').classList.remove('is-over'));
    });
    $('#thumb').addEventListener('drop', (e) => {
      e.preventDefault();
      uploadThumb(e.dataTransfer.files[0]);
    });

    $('#addMemberBtn').onclick = () => openMemberForm(true);
    $('#cancelMemberBtn').onclick = () => openMemberForm(false);
    $('#memberForm').addEventListener('submit', addMember);
    $('#memberName').addEventListener('keydown', (e) => {
      if (e.key === 'Escape') { e.stopPropagation(); openMemberForm(false); }
    });
    $('#deleteBtn').onclick = deleteProject;
    $('#saveBtn').onclick = async () => { await saveNow(); go('#/'); toast('Project saved'); };

    $('#search').addEventListener('input', debounce((e) => {
      state.query = e.target.value.trim();
      refresh();
    }, 250));

    $('#pName').addEventListener('input', (e) => {
      state.current.name = e.target.value;
      autoGrow(e.target);
      queueSave();
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && state.current) go('#/');
      if (e.key === '/' && !state.current && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
        e.preventDefault();
        $('#search').focus();
      }
    });

    // one listener for every combo: click anywhere else and they all close
    document.addEventListener('pointerdown', (e) => {
      if (!e.target.closest('.combo')) closeAllCombos();
    });

    window.addEventListener('hashchange', route);
  }

  /* ============================================================
     Boot
     ============================================================ */
  async function start() {
    initTheme();
    bind();
    renderBoard();
    await checkAdminSession();
    try {
      state.meta = await api.meta();
    } catch {
      toast('Could not load the committee list', 'error');
    }
    renderCommittee();
    route();
  }

  start();
})();
