/* Waypoint site motion. First-party only: no network, no analytics, no storage.
   Without this file (or with it failing) every section is already readable. */
(() => {
  const d = document, r = d.documentElement;
  const calm = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const fine = matchMedia('(hover: hover) and (pointer: fine)').matches;
  const $$ = (s, c = d) => [...c.querySelectorAll(s)];
  const clamp = (v) => Math.min(1, Math.max(0, v));
  window.__wp = 1;

  /* 1. Split headings and statements into words so each can fly in or light up. */
  $$('[data-split]').forEach((el) => {
    let i = 0;
    if (el.dataset.scrub !== undefined) el.classList.add('scrub');
    const walk = (n) => [...n.childNodes].forEach((c) => {
      if (c.nodeType === 3) {
        const f = d.createDocumentFragment();
        c.textContent.split(/(\s+)/).forEach((t) => {
          if (!t) return;
          if (/^\s+$/.test(t)) return f.append(' ');
          const w = d.createElement('span'), s = d.createElement('span');
          w.className = 'w'; s.className = 'wi';
          s.style.setProperty('--i', i++); s.textContent = t;
          w.append(s); f.append(w);
        });
        c.replaceWith(f);
      } else if (c.nodeType === 1 && c.tagName !== 'BR') walk(c);
    });
    walk(el);
    el.style.setProperty('--n', i);
  });

  /* 2. Reveal on entry. */
  const io = new IntersectionObserver((es) => es.forEach((e) => {
    if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
  }), { rootMargin: '0px 0px -9% 0px' });
  $$('[data-split]:not(.scrub), .rv, .fx, [data-in]').forEach((el) => calm ? el.classList.add('in') : io.observe(el));

  /* 3. Scroll progress as CSS variables: --pg (page), --p (per element), data-step (scenes). */
  const live = new Set();
  const vio = new IntersectionObserver((es) => es.forEach((e) => e.isIntersecting ? live.add(e.target) : live.delete(e.target)), { rootMargin: '25% 0px' });
  $$('[data-scrub]').forEach((el) => {
    if (calm) return;
    vio.observe(el);
  });
  const runs = [];
  const nav = $$('.masthead nav a[href^="#"]');
  const sections = nav.map((a) => d.querySelector(a.getAttribute('href'))).filter(Boolean);
  let queued = 0;
  const update = () => {
    queued = 0;
    const h = innerHeight;
    const bar = d.querySelector('.masthead');
    if (bar && !calm) bar.style.setProperty('--pg', clamp(scrollY / Math.max(1, r.scrollHeight - h)).toFixed(4));
    live.forEach((el) => {
      if (el.dataset.manual) return;
      const b = el.getBoundingClientRect();
      const mode = el.dataset.scrub;
      let p;
      if (mode === 'top') p = -b.top / Math.max(1, b.height);
      else if (mode === 'text') p = (h * 0.92 - b.top) / (h * 0.6);
      else p = (h - b.top) / (h + b.height);
      p = clamp(p);
      el.style.setProperty('--p', p.toFixed(4));
    });
    runs.forEach((f) => f());
    let cur = null;
    sections.forEach((s) => { if (s.getBoundingClientRect().top < h * 0.45) cur = s.id; });
    nav.forEach((a) => a.classList.toggle('cur', a.getAttribute('href') === '#' + cur));
    if (bar) bar.classList.toggle('solid', scrollY > 24);
  };
  addEventListener('scroll', () => { queued || (queued = requestAnimationFrame(update)); }, { passive: true });
  addEventListener('resize', update);
  update();


  /* 3a. The route run. Scrolling draws one line down the page: a lime trace grows along a dotted
     guide, a navigation arrow rides its head, and each stop lights up as the arrow reaches its marker.
     The path is rebuilt from the real marker positions, so it follows the layout at any width. */
  $$('[data-run]').forEach((run) => {
    const svg = run.querySelector('.run-line');
    const guide = svg.querySelector('.run-guide'), trace = svg.querySelector('.run-trace');
    const car = svg.querySelector('.run-car');
    const stops = $$('[data-stop]', run);
    const dots = stops.map((s) => s.querySelector('.dot'));
    let ys = [], len = [], total = 0, ready = false;
    const showAll = () => {
      stops.forEach((s) => s.classList.add('on'));
      trace.style.strokeDasharray = 'none';
      trace.style.strokeDashoffset = '0';
    };
    const build = () => {
      const rb = run.getBoundingClientRect();
      const pts = dots.map((dot) => {
        const b = dot.getBoundingClientRect();
        return [b.left + b.width / 2 - rb.left, b.top + b.height / 2 - rb.top];
      });
      if (!rb.width || pts.some((p) => !isFinite(p[0]) || !isFinite(p[1]))) return;
      svg.setAttribute('viewBox', `0 0 ${rb.width} ${rb.height}`);
      // Straight in from the top edge, then a smooth S between neighbouring markers.
      let d = `M${pts[0][0]} 0 L${pts[0][0]} ${pts[0][1]}`;
      for (let i = 1; i < pts.length; i++) {
        const [x0, y0] = pts[i - 1], [x1, y1] = pts[i], k = (y1 - y0) * 0.55;
        d += ` C${x0} ${y0 + k} ${x1} ${y1 - k} ${x1} ${y1}`;
      }
      guide.setAttribute('d', d);
      trace.setAttribute('d', d);
      total = trace.getTotalLength();
      // Sample the path so a scroll position maps to a length, whatever the curve does.
      ys = []; len = [];
      const n = 240;
      for (let i = 0; i <= n; i++) {
        const l = (total * i) / n, p = trace.getPointAtLength(l);
        len.push(l); ys.push(p.y);
      }
      stops.forEach((s, i) => { s.dataset.y = pts[i][1]; });
      ready = true;
      if (calm) showAll(); else {
        trace.style.strokeDasharray = `${total}`;
        step();
      }
    };
    const lengthAt = (y) => {
      if (y <= ys[0]) return 0;
      if (y >= ys[ys.length - 1]) return total;
      let lo = 0, hi = ys.length - 1;
      while (hi - lo > 1) { const m = (lo + hi) >> 1; if (ys[m] <= y) lo = m; else hi = m; }
      const f = (y - ys[lo]) / Math.max(0.0001, ys[hi] - ys[lo]);
      return len[lo] + (len[hi] - len[lo]) * f;
    };
    function step() {
      if (!ready || calm) return;
      const rb = run.getBoundingClientRect();
      const head = innerHeight * 0.62 - rb.top;          // where the arrow sits on the run
      const l = lengthAt(head);
      trace.style.strokeDashoffset = `${total - l}`;
      stops.forEach((s) => s.classList.toggle('on', head >= +s.dataset.y - 30));
      if (car) {
        const p = trace.getPointAtLength(l), q = trace.getPointAtLength(Math.min(total, l + 2));
        const a = Math.atan2(q.y - p.y, q.x - p.x) * 180 / Math.PI + 90;
        car.setAttribute('transform', `translate(${p.x} ${p.y}) rotate(${a})`);
        car.classList.toggle('live', head > 0 && l < total - 1 && rb.top < innerHeight && rb.bottom > 0 && run.dataset.car !== 'off');
      }
    }
    runs.push(step);
    new ResizeObserver(() => build()).observe(run);
    addEventListener('load', build);
    build();
  });

  /* 3b. Day/night compare: drag, tap or use arrow keys. Takes over from scroll once touched. */
  $$('.dn-board').forEach((b) => {
    const t = b.querySelector('.dn-tile');
    if (!t) return;
    t.setAttribute('role', 'slider');
    t.tabIndex = 0;
    t.setAttribute('aria-label', 'Day to night comparison');
    t.setAttribute('aria-valuemin', '0');
    t.setAttribute('aria-valuemax', '100');
    const set = (k) => {
      k = clamp(k);
      b.dataset.manual = 1;
      b.style.setProperty('--k', k.toFixed(3));
      t.setAttribute('aria-valuenow', Math.round(k * 100));
      t.setAttribute('aria-valuetext', k < 0.15 ? 'Day' : k > 0.85 ? 'Night' : Math.round(k * 100) + ' percent toward night');
    };
    const at = (e) => { const q = t.getBoundingClientRect(); set(1 - (e.clientX - q.left) / q.width); };
    // One gentle swing when it first comes into view, so it is obvious the handle moves.
    if (!calm) {
      const seen = new IntersectionObserver((es) => {
        if (!es[0].isIntersecting) return;
        seen.disconnect();
        const t0 = performance.now();
        const swing = (now) => {
          if (b.dataset.manual) return;
          const u = (now - t0) / 2200;
          if (u >= 1) return b.style.setProperty('--k', '.5');
          b.style.setProperty('--k', (0.5 + 0.3 * Math.sin(u * Math.PI * 2) * (1 - u)).toFixed(3));
          requestAnimationFrame(swing);
        };
        setTimeout(() => requestAnimationFrame(swing), 600);
      }, { threshold: 0.6 });
      seen.observe(t);
    }
    // World switcher: swap in the other world's real day and night captures.
    $$('.dn-pick', b).forEach((btn) => btn.addEventListener('click', () => {
      $$('.dn-pick', b).forEach((x) => x.setAttribute('aria-pressed', String(x === btn)));
      ['day', 'night'].forEach((m) => {
        const im = b.querySelector('.dn-map[data-dn="' + m + '"]');
        im.src = 'assets/landing/daynight/' + btn.dataset.world + '-' + m + '.webp';
        im.alt = btn.dataset[m];
      });
      const note = d.getElementById('dn-place');
      if (note) note.textContent = 'Shown: ' + btn.textContent + ' world, ' + btn.dataset.place + '.';
    }));
    t.addEventListener('pointerdown', (e) => { t.setPointerCapture(e.pointerId); t.classList.add('drag'); at(e); });
    t.addEventListener('pointermove', (e) => { if (t.classList.contains('drag')) at(e); });
    ['pointerup', 'pointercancel'].forEach((n) => t.addEventListener(n, () => t.classList.remove('drag')));
    t.addEventListener('keydown', (e) => {
      const cur = parseFloat(getComputedStyle(b).getPropertyValue('--k')) || 0.5;
      const step = e.shiftKey ? 0.25 : 0.05;
      if (e.key === 'ArrowLeft') { set(cur + step); e.preventDefault(); }
      else if (e.key === 'ArrowRight') { set(cur - step); e.preventDefault(); }
      else if (e.key === 'Home') { set(1); e.preventDefault(); }
      else if (e.key === 'End') { set(0); e.preventDefault(); }
    });
  });

  /* 3c. Discord channel demo: tapping a world opens its channel. On first sight, three worlds pick themselves one by one. */
  $$('[data-channels]').forEach((box) => {
    const chips = $$('.cd-chip', box);
    const count = box.querySelector('[data-cd-count]');
    const total = () => { if (count) count.textContent = chips.filter((c) => c.getAttribute('aria-pressed') === 'true').length; };
    let touched = false;
    chips.forEach((c) => c.addEventListener('click', () => {
      touched = true;
      c.setAttribute('aria-pressed', String(c.getAttribute('aria-pressed') !== 'true'));
      total();
    }));
    if (calm) return;
    const finale = chips.filter((c) => c.getAttribute('aria-pressed') === 'true');
    chips.forEach((c) => c.setAttribute('aria-pressed', 'false'));
    total();
    const seen = new IntersectionObserver((es) => {
      if (!es[0].isIntersecting) return;
      seen.disconnect();
      finale.forEach((c, i) => setTimeout(() => {
        if (touched) return;
        c.setAttribute('aria-pressed', 'true');
        total();
      }, 700 + i * 650));
    }, { threshold: 0.5 });
    seen.observe(box);
  });

  /* 3d. The three community points draw their rules and unmask one after another (CSS owns the timing). */
  $$('.community-list').forEach((ul) => {
    [...ul.children].forEach((li, i) => li.style.setProperty('--i', i));
    const seen = new IntersectionObserver((es) => {
      if (!es[0].isIntersecting) return;
      seen.disconnect();
      ul.classList.add('in');
    }, { threshold: 0.3 });
    seen.observe(ul);
  });

  /* 4. Pointer-driven polish (mouse and trackpad only, never under reduced motion). */
  if (calm || !fine) return;

  // Spotlight that follows the cursor across the hero panel.
  $$('[data-spot]').forEach((el) => {
    el.addEventListener('pointermove', (e) => {
      const b = el.getBoundingClientRect();
      el.style.setProperty('--mx', (e.clientX - b.left) + 'px');
      el.style.setProperty('--my', (e.clientY - b.top) + 'px');
      el.style.setProperty('--so', 1);
    });
    el.addEventListener('pointerleave', () => el.style.setProperty('--so', 0));
  });

  // Gentle 3D tilt on the big device and screen frames.
  $$('[data-tilt]').forEach((el) => {
    el.addEventListener('pointermove', (e) => {
      const b = el.getBoundingClientRect();
      const x = (e.clientX - b.left) / b.width - 0.5, y = (e.clientY - b.top) / b.height - 0.5;
      el.style.setProperty('--tx', (x * 7).toFixed(2) + 'deg');
      el.style.setProperty('--ty', (-y * 7).toFixed(2) + 'deg');
    });
    el.addEventListener('pointerleave', () => { el.style.setProperty('--tx', '0deg'); el.style.setProperty('--ty', '0deg'); });
  });

  // Primary buttons lean toward the cursor.
  $$('[data-mag]').forEach((el) => {
    el.addEventListener('pointermove', (e) => {
      const b = el.getBoundingClientRect();
      el.style.setProperty('--mxp', ((e.clientX - b.left - b.width / 2) * 0.16).toFixed(1) + 'px');
      el.style.setProperty('--myp', ((e.clientY - b.top - b.height / 2) * 0.28).toFixed(1) + 'px');
    });
    el.addEventListener('pointerleave', () => { el.style.setProperty('--mxp', '0px'); el.style.setProperty('--myp', '0px'); });
  });
})();
