export const REQUESTABLE_ROLES = new Map([
  ['beta gremlins', '🧪 Beta Gremlins'],
  ['bug squashers', '🐛 Bug Squashers'],
  ['idea cooks', '💡 Idea Cooks'],
  ['world builders', '🎨 World Builders'],
  ['map flexers', '📸 Map Flexers'],
  ['map makers', '🗺️ Map Makers'],
  ['mod squad', '🛡️ Mod Squad'],
  ['moderator', '🛡️ Mod Squad'],
]);

const clean = (value) => value.trim().toLowerCase().replace(/\s+/g, ' ');

export function requestedRole(title) {
  return REQUESTABLE_ROLES.get(clean(title)) ?? null;
}

export function statusOf(thread, tags) {
  if (thread.appliedTags.includes(tags.approved)) return 'approved';
  if (thread.appliedTags.includes(tags.declined)) return 'declined';
  return 'pending';
}

export function withStatus(thread, tags, status) {
  const statusIds = new Set(Object.values(tags));
  return [
    ...thread.appliedTags.filter((tag) => !statusIds.has(tag)),
    tags[status],
  ];
}

export function oldestPending(threads, tags) {
  const byOwner = new Map();
  for (const thread of threads) {
    if (!thread.ownerId || statusOf(thread, tags) !== 'pending') continue;
    const existing = byOwner.get(thread.ownerId);
    if (!existing || thread.createdTimestamp < existing.createdTimestamp) {
      byOwner.set(thread.ownerId, thread);
    }
  }
  return byOwner;
}
