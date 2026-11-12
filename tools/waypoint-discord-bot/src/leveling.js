import { EmbedBuilder } from 'discord.js';
import {
  BASE_ROLE, BUG_FORUM_ID, FEATURE_FORUM_ID, GUILD_ID, LEVEL_ROLES,
  HELP_CHANNEL_ID, QUESTS_CHANNEL_ID, ROLE_FORUM_ID, ROLE_PICK_CHANNEL_ID,
  RULES_CHANNEL_ID, SHOW_MAP_CHANNEL_ID, WELCOME_CHANNEL_ID, WORLD_IDEAS_FORUM_ID,
} from './config.js';
import { loadState, saveState } from './store.js';

export const XP_PER_MESSAGE_MIN = 15;
export const XP_PER_MESSAGE_MAX = 40;
export const XP_COOLDOWN_MS = 60_000;
export const XP_PER_REACTION = 25;
export const REACTION_COOLDOWN_MS = 5 * 60_000;
export const XP_PER_FORUM_POST = 35;
const IST_OFFSET_MS = 330 * 60_000;
// Arcane's configured linear curve needs 75 + 100 * current level for each level.
export const totalXpForLevel = (level) => 50 * level * level + 25 * level;
export const levelForXp = (xp) => Math.floor((Math.sqrt(625 + 200 * Math.max(0, xp)) - 25) / 100);
export const dayKey = (now = Date.now()) => new Date(now + IST_OFFSET_MS).toISOString().slice(0, 10);
const previousDayKey = (now = Date.now()) => dayKey(now - 86_400_000);

export async function rankFor(userId) {
  const state = await loadState();
  const xp = state.xp[userId]?.xp ?? 0;
  const level = levelForXp(xp);
  return { xp, level, next: totalXpForLevel(level + 1), today: state.daily?.[dayKey()]?.[userId] ?? 0 };
}

export async function leaderboard(period = 'daily', date = dayKey()) {
  const state = await loadState();
  if (period === 'daily') {
    return Object.entries(state.daily?.[date] ?? {})
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .map(([userId, xp], index) => ({ position: index + 1, userId, xp, level: levelForXp(state.xp[userId]?.xp ?? 0) }));
  }
  return Object.entries(state.xp)
    .sort((a, b) => b[1].xp - a[1].xp)
    .slice(0, 10)
    .map(([userId, entry], index) => ({ position: index + 1, userId, xp: entry.xp, level: levelForXp(entry.xp) }));
}

export async function syncLevelRoles(member, level, removeHigher = false) {
  const roles = await member.guild.roles.fetch();
  for (const [threshold, name] of LEVEL_ROLES) {
    const role = roles.find((item) => item.name === name);
    if (!role || !role.editable) throw new Error(`Cannot change level role: ${name}`);
    if (level >= threshold && !member.roles.cache.has(role.id)) {
      await member.roles.add(role, `A.T.L.A.S. reached level ${level}`);
    } else if (removeHigher && level < threshold && member.roles.cache.has(role.id)) {
      await member.roles.remove(role, 'A.T.L.A.S. XP correction');
    }
  }
}

async function award(guild, userId, points, client) {
  const state = await loadState();
  const today = dayKey();
  state.daily ??= {};
  state.daily[today] ??= {};
  const entry = state.xp[userId] ?? { xp: 0 };
  const previousLevel = levelForXp(entry.xp);
  entry.xp += points;
  state.xp[userId] = entry;
  state.daily[today][userId] = (state.daily[today][userId] ?? 0) + points;
  await saveState();
  const level = levelForXp(entry.xp);
  if (level === previousLevel) return;
  const member = await guild.members.fetch(userId);
  await syncLevelRoles(member, level);
  if (LEVEL_ROLES.has(level)) {
    const channel = await client.channels.fetch(QUESTS_CHANNEL_ID);
    if (channel?.isTextBased()) {
      await channel.send({
        content: `<@${userId}> reached level ${level} and earned ${LEVEL_ROLES.get(level)}!`,
        allowedMentions: { users: [userId] },
      });
    }
  }
}

export async function awardMessageXp(message) {
  if (!message.guild || message.guildId !== GUILD_ID || message.author.bot || message.webhookId) return;
  if (message.channel.topic?.startsWith('atlas-ticket')) return;
  const state = await loadState();
  const entry = state.xp[message.author.id] ?? { xp: 0 };
  const now = Date.now();
  if (now - (entry.lastMessageAt ?? 0) < XP_COOLDOWN_MS) return;
  entry.lastMessageAt = now;
  state.xp[message.author.id] = entry;
  await award(message.guild, message.author.id,
    XP_PER_MESSAGE_MIN + Math.floor(Math.random() * (XP_PER_MESSAGE_MAX - XP_PER_MESSAGE_MIN + 1)), message.client);
}

async function awardReactionParticipant(message, userId, key) {
  const state = await loadState();
  const entry = state.xp[userId] ?? { xp: 0 };
  const now = Date.now();
  if (now - (entry.lastReactionAt ?? 0) < REACTION_COOLDOWN_MS) return;
  state.reactionAwards ??= {};
  state.reactionAwards[dayKey()] ??= {};
  const awarded = state.reactionAwards[dayKey()][userId] ?? [];
  if (awarded.includes(key) || awarded.length >= 20) return;
  awarded.push(key);
  state.reactionAwards[dayKey()][userId] = awarded;
  entry.lastReactionAt = now;
  state.xp[userId] = entry;
  await award(message.guild, userId, XP_PER_REACTION, message.client);
}

export async function awardReactionXp(reaction, user) {
  if (user.bot) return;
  if (reaction.partial) reaction = await reaction.fetch();
  const message = await reaction.message.fetch();
  if (!message.guild || message.guildId !== GUILD_ID || message.author.bot || message.author.id === user.id) return;
  if (message.channelId === ROLE_PICK_CHANNEL_ID || message.channel.parentId === ROLE_FORUM_ID || message.channel.topic?.startsWith('atlas-ticket')) return;
  await awardReactionParticipant(message, user.id, `sent:${message.id}`);
  await awardReactionParticipant(message, message.author.id, `received:${message.id}:${user.id}`);
}

export async function awardForumPostXp(thread) {
  if (thread.guildId !== GUILD_ID || !thread.ownerId) return;
  if (![BUG_FORUM_ID, FEATURE_FORUM_ID, WORLD_IDEAS_FORUM_ID].includes(thread.parentId)) return;
  const user = await thread.client.users.fetch(thread.ownerId);
  if (user.bot) return;
  const state = await loadState();
  state.forumAwards ??= {};
  state.forumAwards[dayKey()] ??= {};
  const count = state.forumAwards[dayKey()][user.id] ?? 0;
  if (count >= 3) return;
  state.forumAwards[dayKey()][user.id] = count + 1;
  await award(thread.guild, user.id, XP_PER_FORUM_POST, thread.client);
}

export async function postDailyLeaderboard(client) {
  const state = await loadState();
  const previous = previousDayKey();
  if (state.lastDailyPostKey === previous) return;
  const entries = await leaderboard('daily', previous);
  if (entries.length === 0) {
    state.lastDailyPostKey = previous;
    await saveState();
    return;
  }
  const channel = await client.channels.fetch(QUESTS_CHANNEL_ID);
  if (!channel?.isTextBased()) throw new Error('Quest and XP channel is unavailable.');
  const lines = entries.map((entry) => `${entry.position}. <@${entry.userId}> — ${entry.xp} quest points`);
  await channel.send({
    content: `**Daily Waypoint leaderboard · ${previous} (IST)**\n${lines.join('\n')}\nToday starts fresh. Use /leaderboard for the live board and /rank for all-time progress.`,
    allowedMentions: { parse: [] },
  });
  state.lastDailyPostKey = previous;
  for (const date of Object.keys(state.reactionAwards ?? {})) {
    if (date < previous) delete state.reactionAwards[date];
  }
  for (const date of Object.keys(state.forumAwards ?? {})) {
    if (date < previous) delete state.forumAwards[date];
  }
  await saveState();
}

export function welcomePayload(member, roleName = BASE_ROLE) {
  const themes = [
      ['🧭 New waypoint unlocked', 'A fresh route begins for {name}. Pick a world, find your crew, and make the map your own.', 0x55c7ba],
      ['🗺️ The map just grew', '{name} has joined the expedition. There are 38 worlds to explore and plenty of room for a new story.', 0x6aa9ff],
      ['✨ Explorer signal found', 'Welcome aboard, {name}. The first checkpoint is choosing the worlds you want to visit.', 0xb29aff],
      ['🚀 Adventure loading', '{name} is here. Choose a lane, meet the community, and start collecting quest points.', 0xffb86b],
      ['🌍 A new world awaits', 'The doors are open for {name}. Your favorite game worlds are only one role away.', 0x7edc9b],
      ['🎮 Player joined Waypoint', 'Good to see you, {name}. The best routes start with a hello and a world to call home.', 0xff8fb1],
      ['📍 Checkpoint reached', '{name} has arrived. Set your course through the community and share what you discover.', 0xf0ce73],
      ['🌌 Your route starts here', 'Welcome, {name}. Pick a world, bring your ideas, and see where the journey goes.', 0x88b8ff],
  ];
  const hash = [...member.id].reduce((value, character) => (value * 31 + character.charCodeAt(0)) >>> 0, 0);
  const [title, template, color] = themes[hash % themes.length];
  const name = member.displayName.replace(/[@*_`~|<>]/g, '').slice(0, 40) || 'explorer';
  const embed = new EmbedBuilder()
      .setColor(color)
      .setTitle(title)
      .setDescription(template.replace('{name}', name))
      .setThumbnail(member.user.displayAvatarURL({ size: 128 }))
      .addFields(
        { name: 'Your first checkpoints', value: `1. Read <#${RULES_CHANNEL_ID}>.\n2. Pick your worlds in <#${ROLE_PICK_CHANNEL_ID}> or use **/roles**.\n3. Say hi and share a discovery in <#${SHOW_MAP_CHANNEL_ID}>.`, inline: false },
        { name: 'Need a guide?', value: `Use **/help** for tickets and **/rank** for your quest progress. Private support starts in <#${HELP_CHANNEL_ID}>.`, inline: false },
      )
      .setFooter({ text: `Explorer #${member.guild.memberCount} • 38 worlds • A.T.L.A.S.` });
  return {
    content: `Welcome to Waypoint, ${member}! Your ${roleName} role is ready.`,
    embeds: [embed],
    allowedMentions: { users: [member.id] },
  };
}

export async function welcomeMember(member) {
  if (member.guild.id !== GUILD_ID || member.user.bot) return;
  const role = member.guild.roles.cache.find((item) => item.name === BASE_ROLE)
    ?? (await member.guild.roles.fetch()).find((item) => item.name === BASE_ROLE);
  if (!role || !role.editable) throw new Error(`Cannot grant base role: ${BASE_ROLE}`);
  if (!member.roles.cache.has(role.id)) await member.roles.add(role, 'A.T.L.A.S. welcome role');
  const channel = await member.client.channels.fetch(WELCOME_CHANNEL_ID);
  if (channel?.isTextBased()) await channel.send(welcomePayload(member, role.name));
}

export async function setXpByOwner(guild, userId, xp) {
  if (!Number.isSafeInteger(xp) || xp < 0 || xp > 100_000_000) throw new Error('Invalid XP value.');
  const state = await loadState();
  state.xp[userId] = { ...(state.xp[userId] ?? {}), xp };
  await saveState();
  await syncLevelRoles(await guild.members.fetch(userId), levelForXp(xp), true);
}
