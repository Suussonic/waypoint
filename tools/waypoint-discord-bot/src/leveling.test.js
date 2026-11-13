import assert from 'node:assert/strict';
import test from 'node:test';
import { Collection } from 'discord.js';
import { dayKey, levelForXp, totalXpForLevel, welcomePayload } from './leveling.js';
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

test('Arcane legacy ranks retain their level on import', () => {
  assert.equal(totalXpForLevel(4), 900);
  assert.equal(levelForXp(1356), 4);
  assert.equal(levelForXp(340), 2);
  assert.equal(levelForXp(82), 1);
});

test('welcome cards vary by member and keep channel directions', () => {
  const member = (id) => ({ id, displayName: 'Map*Maker', user: { displayAvatarURL: () => 'https://example.com/avatar.png' }, guild: { memberCount: 43 }, toString: () => `<@${id}>` });
  const first = welcomePayload(member('100')).embeds[0].toJSON();
  const second = welcomePayload(member('101')).embeds[0].toJSON();
  assert.notEqual(first.title, second.title);
  assert.match(first.description, /MapMaker/);
  assert.match(first.fields[0].value, /1569240786409758740/);
});

test('all 38 worlds fit in two Discord select menus', () => {
  const rows = roleMenuRows({ roles: { cache: new Collection() } });
  assert.deepEqual(rows.map((row) => row.toJSON().components[0].options.length), [2, 19, 19]);
});
