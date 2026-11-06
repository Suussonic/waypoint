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
  $$('[data-scrub], [data-scene]').forEach((el) => {
    if (calm && !el.hasAttribute('data-scene')) return;
    vio.observe(el);
  });
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
      if (el.hasAttribute('data-scene')) p = -b.top / Math.max(1, b.height - h);
      else if (mode === 'top') p = -b.top / Math.max(1, b.height);
      else if (mode === 'text') p = (h * 0.92 - b.top) / (h * 0.6);
      else p = (h - b.top) / (h + b.height);
      p = clamp(p);
      el.style.setProperty('--p', p.toFixed(4));
      if (el.hasAttribute('data-scene')) {
        const n = +el.dataset.steps, s = Math.min(n - 1, Math.floor(p * n));
        if (el.dataset.step !== String(s)) {
          el.dataset.step = s;
          $$('[data-i]', el).forEach((c) => c.classList.toggle('on', +c.dataset.i === s));
        }
      }
    });
    let cur = null;
    sections.forEach((s) => { if (s.getBoundingClientRect().top < h * 0.45) cur = s.id; });
    nav.forEach((a) => a.classList.toggle('cur', a.getAttribute('href') === '#' + cur));
    if (bar) bar.classList.toggle('solid', scrollY > 24);
  };
  addEventListener('scroll', () => { queued || (queued = requestAnimationFrame(update)); }, { passive: true });
  addEventListener('resize', update);
  update();

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

  // The swipeable captures are a scroll region only on small screens.
  const stage = d.querySelector('.scene-stage');
  if (stage) {
    const mq = matchMedia('(max-width: 55rem)');
    const sync = () => { mq.matches ? stage.setAttribute('tabindex', '0') : stage.removeAttribute('tabindex'); };
    sync(); mq.addEventListener('change', sync);
  }

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
