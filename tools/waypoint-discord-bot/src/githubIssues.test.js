import assert from 'node:assert/strict';
import test from 'node:test';
import { issuePayload } from './githubIssues.js';

test('bug ticket produces a clear public issue without private chat content', () => {
  const issue = issuePayload('1570323716204658781', 'bug', {
    title: 'Route vanishes on Android Auto',
    description: 'The route disappears after a turn near home.',
    detail: '1. Start guidance\n2. Take a turn',
    device: 'Waypoint 2.6.5, Pixel 8, Android 15',
  });
  assert.equal(issue.title, '[Discord bug] Route vanishes on Android Auto');
  assert.match(issue.body, /## What happened/);
  assert.match(issue.body, /## Steps to reproduce/);
  assert.match(issue.body, /Waypoint 2\.6\.5/);
  assert.match(issue.body, /Private ticket replies are not copied/);
  assert.deepEqual(issue.labels, ['discord-ticket', 'bug']);
});

test('general help cannot publish a GitHub issue', () => {
  assert.throws(() => issuePayload('1570323716204658781', 'general', { title: 'Help me', description: 'Need help with setup' }));
});
