import assert from 'node:assert/strict';
import test from 'node:test';
import { oldestPending, requestedRole, statusOf, withStatus } from './rules.js';

const tags = { pending: 'p', approved: 'a', declined: 'd' };

test('requires one exact requestable role', () => {
  assert.equal(requestedRole('Mod Squad'), '🛡️ Mod Squad');
  assert.equal(requestedRole('Bug Squashers'), '🐛 Bug Squashers');
  assert.equal(requestedRole('Administrator'), null);
  assert.equal(requestedRole('Map Makers / Mod Squad'), null);
});

test('status change replaces old status and retains unrelated tags', () => {
  const thread = { appliedTags: ['p', 'other'] };
  assert.deepEqual(withStatus(thread, tags, 'approved'), ['other', 'a']);
  assert.equal(statusOf({ appliedTags: ['a'] }, tags), 'approved');
});

test('only the oldest unresolved post counts for a member', () => {
  const threads = [
    { id: 'new', ownerId: 'u', createdTimestamp: 2, appliedTags: ['p'] },
    { id: 'old', ownerId: 'u', createdTimestamp: 1, appliedTags: [] },
    { id: 'done', ownerId: 'v', createdTimestamp: 1, appliedTags: ['a'] },
  ];
  assert.equal(oldestPending(threads, tags).get('u').id, 'old');
  assert.equal(oldestPending(threads, tags).has('v'), false);
});
