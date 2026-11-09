/* The HTML contains a dated snapshot. Refresh both figures from GitHub when available. */
(() => {
  const downloadsEl = document.querySelector('[data-github-downloads]');
  const starsEl = document.querySelector('[data-github-stars]');
  const statusEl = document.querySelector('[data-github-metrics-status]');
  if (!downloadsEl || !starsEl || !statusEl || !window.fetch || !window.AbortController) return;

  const api = 'https://api.github.com/repos/the-geeky-couldron/waypoint';
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);
  const validCount = (value) => Number.isSafeInteger(value) && value >= 0;
  const request = async (path) => {
    const response = await fetch(api + path, {
      headers: { Accept: 'application/vnd.github+json' },
      signal: controller.signal
    });
    if (!response.ok) throw new Error('GitHub metrics unavailable');
    return response.json();
  };

  const allReleaseDownloads = async () => {
    let total = 0;
    for (let page = 1; page <= 20; page++) {
      const releases = await request(`/releases?per_page=100&page=${page}`);
      if (!Array.isArray(releases) || releases.length > 100) throw new Error('Invalid releases');
      for (const release of releases) {
        if (!Array.isArray(release.assets)) throw new Error('Invalid release assets');
        for (const asset of release.assets) {
          if (!validCount(asset.download_count)) throw new Error('Invalid download count');
          total += asset.download_count;
          if (!validCount(total)) throw new Error('Download total overflow');
        }
      }
      if (releases.length < 100) return total;
    }
    throw new Error('Incomplete release history');
  };

  Promise.all([request(''), allReleaseDownloads()])
    .then(([repository, downloads]) => {
      if (!validCount(repository.stargazers_count)) throw new Error('Invalid star count');
      const format = new Intl.NumberFormat('en-US');
      downloadsEl.textContent = format.format(downloads);
      starsEl.textContent = format.format(repository.stargazers_count);
      statusEl.textContent = 'GitHub · refreshed on this visit';
      statusEl.classList.add('is-live');
    })
    .catch(() => { /* Keep the visibly dated snapshot when GitHub is unavailable. */ })
    .finally(() => clearTimeout(timeout));
})();
