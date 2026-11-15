/* Refresh only the two APK download endpoints from GitHub's public release API. */
(() => {
  const page = document.querySelector('.milestones-page');
  const timeline = document.querySelector('[data-release-timeline]') || document.querySelector('.release-list');
  const status = document.querySelector('[data-metrics-status]');
  if (!page || !timeline || !status || !window.fetch || !window.AbortController) return;

  const api = 'https://api.github.com/repos/the-geeky-couldron/waypoint/releases';
  const timeout = new AbortController();
  const timer = setTimeout(() => timeout.abort(), 8000);
  const validCount = (value) => Number.isSafeInteger(value) && value >= 0;
  const stableTag = /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;
  const formatDate = (date) => new Intl.DateTimeFormat('en-GB', {
    day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC'
  }).format(date);

  const fetchReleases = async () => {
    const response = await fetch(`${api}?per_page=100&page=1`, {
      headers: { Accept: 'application/vnd.github+json' },
      signal: timeout.signal
    });
    if (!response.ok) throw new Error('Release data unavailable');
    const result = await response.json();
    if (!Array.isArray(result) || result.length >= 100) throw new Error('Release history is incomplete');
    return result;
  };

  const summarize = (body) => {
    const lines = typeof body === 'string' ? body.split(/\r?\n/) : [];
    const first = lines.find((line) => {
      const text = line.trim();
      return text && !/^#{1,6}\s/.test(text) && !/^[-*_]{3,}$/.test(text);
    }) || lines.find((line) => /^\s*[-*+]\s+/.test(line));
    const plain = (first || 'See the published release notes for details.')
      .trim().replace(/^[-*+]\s+/, '')
      .replace(/!?\[([^\]]+)\]\([^)]*\)/g, '$1')
      .replace(/[`*_>#]/g, '')
      .replace(/\s+/g, ' ')
      .trim();
    return plain.length > 240 ? `${plain.slice(0, 237).trimEnd()}…` : plain;
  };

  const appendRelease = (tag, publishedAt, body) => {
    const entry = document.createElement('li');
    entry.className = 'ms release-entry release-entry-live';
    entry.dataset.releaseTag = tag;
    entry.dataset.published = publishedAt;
    entry.setAttribute('data-stop', '');

    const dot = document.createElement('span');
    dot.className = 'dot ms-dot release-dot';
    dot.setAttribute('aria-hidden', 'true');
    const dotLabel = document.createElement('span');
    dotLabel.textContent = tag.slice(1);
    dot.append(dotLabel);

    const content = document.createElement('div');
    content.className = 'ms-body';
    const dateLine = document.createElement('p');
    dateLine.className = 'release-date';
    dateLine.append('Released ');
    const time = document.createElement('time');
    const publishedDate = new Date(publishedAt);
    time.dateTime = publishedDate.toISOString().slice(0, 10);
    time.textContent = formatDate(publishedDate);
    dateLine.append(time);

    const title = document.createElement('h3');
    title.textContent = `${tag} · ${summarize(body)}`;
    const link = document.createElement('a');
    link.className = 'text-link';
    link.href = `https://github.com/the-geeky-couldron/waypoint/releases/tag/${encodeURIComponent(tag)}`;
    link.textContent = 'Release notes ↗';
    content.append(dateLine, title, link);
    entry.append(dot, content);

    const published = Date.parse(publishedAt);
    const next = [...timeline.querySelectorAll('.release-entry[data-published]')]
      .find((row) => Date.parse(row.dataset.published) > published);
    timeline.insertBefore(entry, next || null);
  };

  fetchReleases().then((rawReleases) => {
    const releases = rawReleases.filter((release) =>
      release && release.draft !== true && release.prerelease !== true &&
      typeof release.tag_name === 'string' && stableTag.test(release.tag_name) &&
      typeof release.published_at === 'string' && Number.isFinite(Date.parse(release.published_at)) &&
      Array.isArray(release.assets)
    ).sort((a, b) => Date.parse(a.published_at) - Date.parse(b.published_at));

    let total = 0;
    const withCounts = releases.map((release) => {
      const apk = release.assets.find((asset) => asset && asset.name === 'Waypoint.apk');
      const count = apk ? apk.download_count : 0;
      if (!validCount(count)) throw new Error('Invalid APK count');
      total += count;
      if (!validCount(total)) throw new Error('APK total overflow');
      return { release, count };
    });
    const first = withCounts.find(({ release }) => release.tag_name === 'v0.2.1');
    const latest = withCounts.at(-1);
    if (!first || !latest || !validCount(first.count)) throw new Error('Release endpoints unavailable');

    document.querySelector('[data-first-downloads]').textContent = new Intl.NumberFormat('en-US').format(first.count);
    document.querySelector('[data-total-downloads]').textContent = new Intl.NumberFormat('en-US').format(total);
    document.querySelectorAll('[data-current-release], [data-current-release-inline]').forEach((el) => {
      el.textContent = latest.release.tag_name;
    });
    document.querySelectorAll('[data-current-date]').forEach((el) => {
      const publishedDate = new Date(latest.release.published_at);
      el.dateTime = publishedDate.toISOString().slice(0, 10);
      el.textContent = formatDate(publishedDate);
    });

    for (const { release } of withCounts) {
      const entry = [...timeline.querySelectorAll('.release-entry')]
        .find((row) => row.dataset.releaseTag === release.tag_name);
      if (entry) {
        entry.dataset.published = release.published_at;
        const time = entry.querySelector('time');
        if (time) {
          const publishedDate = new Date(release.published_at);
          time.dateTime = publishedDate.toISOString().slice(0, 10);
          time.textContent = formatDate(publishedDate);
        }
      } else {
        appendRelease(release.tag_name, release.published_at, release.body);
      }
    }
    const count = timeline.querySelectorAll('.release-entry').length;
    document.querySelector('[data-release-count]').textContent = new Intl.NumberFormat('en-US').format(count);
    status.textContent = `Live GitHub snapshot · refreshed ${formatDate(new Date())}`;
    status.classList.add('is-live');
  }).catch(() => {
    /* Keep the dated, readable release and download snapshot when GitHub is unavailable. */
  }).finally(() => clearTimeout(timer));
})();
