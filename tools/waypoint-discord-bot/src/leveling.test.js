import assert from 'node:assert/strict';
import test from 'node:test';
import { Collection } from 'discord.js';
import { dayKey, levelForXp, totalXpForLevel } from './leveling.js';
import { roleMenuRows } from './selfRoles.js';

test('daily quest points roll over at midnight India time', () => {
  assert.equal(dayKey(Date.parse('2026-11-12T18:29:59Z')), '2026-11-12');
  assert.equal(dayKey(Date.parse('2026-11-12T18:30:00Z')), '2026-11-13');
});

test('level thresholds stay monotonic through all four role milestones', () => {
  for (const level of [5, 10, 20, 35]) {
    assert.equal(levelForXp(totalXpForLevel(level) - 1), level - 1);
    assert.equal(levelForXp(totalXpForLevel(level)), level);
  }
});

test('all 38 worlds fit in two Discord select menus', () => {
  const rows = roleMenuRows({ roles: { cache: new Collection() } });
  assert.deepEqual(rows.map((row) => row.toJSON().components[0].options.length), [2, 19, 19]);
});
