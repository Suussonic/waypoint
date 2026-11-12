import { promisify } from 'node:util';
import { execFile } from 'node:child_process';
import { loadState, saveState } from './store.js';

const execFileAsync = promisify(execFile);
const repository = 'the-geeky-couldron/waypoint';
const api = 'https://api.github.com';

function safeText(value, limit) {
  return String(value ?? '').trim().replaceAll('@', '@\u200b').slice(0, limit);
}

export function issuePayload(ticketId, type, fields) {
  if (!['bug', 'feature'].includes(type)) throw new Error('Only bug and feature tickets create GitHub issues.');
  if (!/^\d{17,20}$/.test(ticketId)) throw new Error('Invalid Discord ticket ID.');
  const subject = safeText(fields.title, 100).replace(/\s+/g, ' ');
  const description = safeText(fields.description, 4000);
  if (subject.length < 5 || description.length < 10) throw new Error('Give the issue a clear title and description.');
  const detailLabel = type === 'bug' ? 'Steps to reproduce' : 'Why this would help';
  const detail = safeText(fields.detail, 2000) || '_Not provided._';
  const device = type === 'bug' ? `\n\n## App version and device\n\n${safeText(fields.device, 200) || '_Not provided._'}` : '';
  return {
    title: `[Discord ${type}] ${subject}`,
    body: `## ${type === 'bug' ? 'What happened' : 'Feature request'}\n\n${description}\n\n## ${detailLabel}\n\n${detail}${device}\n\n---\nSubmitted through a Waypoint Discord A.T.L.A.S. ticket. Private ticket replies are not copied here.\n<!-- atlas-ticket:${ticketId} -->`,
    labels: ['discord-ticket', type === 'bug' ? 'bug' : 'enhancement'],
  };
}

async function token() {
  if (process.env.GITHUB_ISSUES_TOKEN) return process.env.GITHUB_ISSUES_TOKEN;
  try {
    const result = await execFileAsync('gh', ['auth', 'token'], { timeout: 10_000 });
    return result.stdout.trim();
  } catch {
    throw new Error('GitHub Issues authentication is missing. Set GITHUB_ISSUES_TOKEN or sign in with gh.');
  }
}

async function github(path, options = {}) {
  const response = await fetch(`${api}${path}`, {
    ...options,
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${await token()}`,
      'X-GitHub-Api-Version': '2026-03-10',
      'Content-Type': 'application/json',
      ...options.headers,
    },
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) {
    const detail = (await response.text()).slice(0, 250);
    throw new Error(`GitHub Issues returned ${response.status}: ${detail}`);
  }
  return response.json();
}

export async function createIssueOnce(ticketId, type, fields) {
  const state = await loadState();
  state.ticketIssues ??= {};
  if (state.ticketIssues[ticketId]) return state.ticketIssues[ticketId];
  const marker = `atlas-ticket:${ticketId}`;
  const search = new URLSearchParams({ q: `repo:${repository} "${marker}" in:body`, per_page: '10' });
  const found = await github(`/search/issues?${search}`);
  const existing = found.items?.find((item) => item.body?.includes(`<!-- ${marker} -->`));
  if (existing) {
    state.ticketIssues[ticketId] = { url: existing.html_url, number: existing.number };
    await saveState();
    return state.ticketIssues[ticketId];
  }
  const payload = issuePayload(ticketId, type, fields);
  const created = await github(`/repos/${repository}/issues`, { method: 'POST', body: JSON.stringify(payload) });
  state.ticketIssues[ticketId] = { url: created.html_url, number: created.number };
  await saveState();
  return state.ticketIssues[ticketId];
}
