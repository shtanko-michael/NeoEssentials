/* NeoEssentials docs — theme toggle, site search, in-table filtering, TOC highlighting.
   No dependencies: the search index is a flat JSON array built at site-generation time. */

(function () {
  'use strict';

  var base = document.documentElement.getAttribute('data-base') || '';

  /* ── theme ─────────────────────────────────────────────────────────────── */

  var btn = document.getElementById('theme');
  var STORE = 'ne-docs-theme';

  function stored() {
    try { return localStorage.getItem(STORE); } catch (e) { return null; }
  }

  var saved = stored();
  if (saved === 'dark' || saved === 'light') {
    document.documentElement.setAttribute('data-theme', saved);
  }

  if (btn) {
    btn.addEventListener('click', function () {
      var cur = document.documentElement.getAttribute('data-theme');
      if (!cur) {
        cur = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
      var next = cur === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem(STORE, next); } catch (e) { /* private mode */ }
    });
  }

  /* ── site search ───────────────────────────────────────────────────────── */

  var q = document.getElementById('q');
  var box = document.getElementById('results');
  var index = null;
  var cursor = -1;

  function load() {
    if (index) return Promise.resolve(index);
    return fetch(base + 'search.json')
      .then(function (r) { return r.json(); })
      .then(function (data) { index = data; return index; })
      .catch(function () { index = []; return index; });
  }

  function score(entry, needle) {
    var t = entry.t.toLowerCase();
    var d = (entry.d || '').toLowerCase();
    if (t === needle) return 0;
    if (t.indexOf(needle) === 0) return 1;
    if (t.indexOf('/' + needle) === 0) return 1;
    if (t.indexOf(needle) !== -1) return 2;
    if (d.indexOf(needle) !== -1) return 3;
    return -1;
  }

  function render(items, needle) {
    if (!items.length) {
      box.innerHTML = '<p class="empty">Ничего не найдено по запросу «' + esc(needle) + '»</p>';
      box.hidden = false;
      return;
    }
    box.innerHTML = items.map(function (it, i) {
      return '<a href="' + base + it.u + '"' + (i === 0 ? ' class="sel"' : '') + '>' +
        '<span class="rt">' + esc(it.t) + '</span>' +
        '<span class="rk">' + esc(it.k) + '</span>' +
        (it.d ? '<span class="rd">' + esc(it.d) + '</span>' : '') +
        '</a>';
    }).join('');
    cursor = 0;
    box.hidden = false;
  }

  function esc(s) {
    return String(s).replace(/[&<>"]/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
    });
  }

  function run() {
    var needle = q.value.trim().toLowerCase().replace(/^\//, '');
    if (needle.length < 2) { box.hidden = true; return; }
    load().then(function (data) {
      var hits = [];
      for (var i = 0; i < data.length; i++) {
        var s = score(data[i], needle);
        if (s >= 0) hits.push([s, data[i]]);
      }
      hits.sort(function (a, b) { return a[0] - b[0] || a[1].t.length - b[1].t.length; });
      render(hits.slice(0, 30).map(function (h) { return h[1]; }), needle);
    });
  }

  if (q && box) {
    q.addEventListener('input', run);
    q.addEventListener('focus', function () { if (q.value.trim().length >= 2) run(); });

    q.addEventListener('keydown', function (ev) {
      var links = box.querySelectorAll('a');
      if (ev.key === 'Escape') { box.hidden = true; q.blur(); return; }
      if (!links.length || box.hidden) return;
      if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') {
        ev.preventDefault();
        if (cursor >= 0 && links[cursor]) links[cursor].classList.remove('sel');
        cursor += ev.key === 'ArrowDown' ? 1 : -1;
        if (cursor < 0) cursor = links.length - 1;
        if (cursor >= links.length) cursor = 0;
        links[cursor].classList.add('sel');
        links[cursor].scrollIntoView({ block: 'nearest' });
      } else if (ev.key === 'Enter' && links[cursor]) {
        ev.preventDefault();
        window.location.href = links[cursor].getAttribute('href');
      }
    });

    document.addEventListener('click', function (ev) {
      if (!box.contains(ev.target) && ev.target !== q) box.hidden = true;
    });

    document.addEventListener('keydown', function (ev) {
      if (ev.key === '/' && document.activeElement !== q &&
          !/^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName)) {
        ev.preventDefault();
        q.focus();
        q.select();
      }
    });
  }

  /* ── in-page table filter + category chips ─────────────────────────────── */

  var tq = document.getElementById('tq');
  var chips = document.querySelectorAll('.chip');
  var activeSys = '*';

  function applyFilter() {
    var needle = tq ? tq.value.trim().toLowerCase() : '';
    var rows = document.querySelectorAll('table.grid tbody tr');
    var shown = 0;
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i];
      if (row.classList.contains('grp')) { row.hidden = !!needle; continue; }
      var hay = row.getAttribute('data-search') || row.textContent.toLowerCase();
      var sys = row.getAttribute('data-sys');
      var okText = !needle || hay.indexOf(needle) !== -1;
      var okSys = activeSys === '*' || !sys || sys === activeSys;
      row.hidden = !(okText && okSys);
      if (!row.hidden) shown++;
    }
    var tally = document.getElementById('tally');
    if (tally) tally.textContent = shown + ' стр.';
  }

  if (tq) tq.addEventListener('input', applyFilter);

  for (var i = 0; i < chips.length; i++) {
    chips[i].addEventListener('click', function (ev) {
      for (var j = 0; j < chips.length; j++) chips[j].classList.remove('on');
      ev.currentTarget.classList.add('on');
      activeSys = ev.currentTarget.getAttribute('data-filter');
      applyFilter();
    });
  }

  /* ── on-this-page highlighting ─────────────────────────────────────────── */

  var tocLinks = document.querySelectorAll('.toc a');
  if (tocLinks.length && 'IntersectionObserver' in window) {
    var map = {};
    for (var k = 0; k < tocLinks.length; k++) {
      map[tocLinks[k].getAttribute('href').slice(1)] = tocLinks[k];
    }
    var obs = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        var link = map[en.target.id];
        if (!link) return;
        if (en.isIntersecting) {
          for (var m in map) { if (map[m]) map[m].classList.remove('on'); }
          link.classList.add('on');
        }
      });
    }, { rootMargin: '-70px 0px -75% 0px' });
    Object.keys(map).forEach(function (id) {
      var el = document.getElementById(id);
      if (el) obs.observe(el);
    });
  }
})();
