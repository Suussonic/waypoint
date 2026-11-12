import { ChannelType, PermissionFlagsBits } from 'discord.js';
import { GUILD_ID, STAFF_ROLE, SUPPORT_CATEGORY_ID, TICKET_LOG_ID } from './config.js';
import { createIssueOnce } from './githubIssues.js';
import { loadState, saveState } from './store.js';

const marker = /^atlas-ticket owner=(\d+) type=(bug|feature|general) status=(open|closed)(?: legacy=ticket-tool)?$/;
const ticketLocks = new Map();

function ticketMeta(channel) {
  const match = channel?.topic?.match(marker);
  return match ? { ownerId: match[1], type: match[2], status: match[3] } : null;
}

async function logTicket(guild, content) {
  const log = await guild.channels.fetch(TICKET_LOG_ID).catch(() => null);
  if (log?.isTextBased()) {
    await log.send({ content, allowedMentions: { parse: [] } }).catch((error) => console.error('[A.T.L.A.S. ticket log]', error));
  }
}

export async function openTicket(guild, user, type, fields) {
  const previous = ticketLocks.get(user.id) ?? Promise.resolve();
  const current = previous.catch(() => {}).then(() => createTicket(guild, user, type, fields));
  ticketLocks.set(user.id, current);
  try {
    return await current;
  } finally {
    if (ticketLocks.get(user.id) === current) ticketLocks.delete(user.id);
  }
}

async function createTicket(guild, user, type, fields) {
  if (guild.id !== GUILD_ID || !['bug', 'feature', 'general'].includes(type)) throw new Error('Invalid ticket request.');
  if (!fields?.title?.trim() || !fields?.description?.trim()) throw new Error('A ticket title and description are required.');
  const channels = await guild.channels.fetch();
  const existing = channels.find((channel) => ticketMeta(channel)?.ownerId === user.id && ticketMeta(channel)?.status === 'open');
  if (existing) return { channel: existing, created: false };
  const staff = (await guild.roles.fetch()).find((role) => role.name === STAFF_ROLE);
  if (!staff) throw new Error('Mod Squad role is missing.');
  const bot = await guild.members.fetchMe();
  const channel = await guild.channels.create({
    name: `ticket-${type}-${user.id.slice(-6)}`,
    type: ChannelType.GuildText,
    parent: SUPPORT_CATEGORY_ID,
    topic: `atlas-ticket owner=${user.id} type=${type} status=open`,
    permissionOverwrites: [
      { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      { id: user.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
      { id: staff.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
      { id: bot.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
    ],
    reason: `A.T.L.A.S. ${type} support ticket for ${user.id}`,
  });
  await channel.send({
    content: `**${type.toUpperCase()} ticket: ${fields.title.trim().slice(0, 100)}**\n${fields.description.trim().slice(0, 2000)}\n\n${user}, staff can continue the discussion here. Use /close when it is resolved.`,
    allowedMentions: { users: [user.id] },
  });
  await logTicket(guild, `Opened ${type} ticket ${channel} for ${user.tag}.`);
  if (type === 'general') return { channel, created: true };
  const state = await loadState();
  state.ticketDrafts ??= {};
  state.ticketDrafts[channel.id] = { type, fields };
  await saveState();
  try {
    const issue = await syncTicketIssue(channel, user, true);
    return { channel, created: true, issue };
  } catch (error) {
    await channel.send('The private ticket is open, but its GitHub issue could not be created yet. Staff can retry with /sync-issue.');
    await logTicket(guild, `GitHub issue sync failed for ${channel}: ${error.message}`);
    return { channel, created: true, issueError: error.message };
  }
}

export async function syncTicketIssue(channel, actor, initial = false) {
  const meta = ticketMeta(channel);
  if (!meta || !['bug', 'feature'].includes(meta.type)) throw new Error('This ticket does not need a GitHub issue.');
  const member = await channel.guild.members.fetch(actor.id);
  const staff = member.roles.cache.some((role) => role.name === STAFF_ROLE);
  if (!initial && actor.id !== channel.guild.ownerId && !staff) throw new Error('Only staff can retry issue sync.');
  const state = await loadState();
  const draft = state.ticketDrafts?.[channel.id];
  if (!draft && state.ticketIssues?.[channel.id]) return state.ticketIssues[channel.id];
  if (!draft) throw new Error('The ticket issue form is missing; ask the requester to submit it again.');
  const issue = await createIssueOnce(channel.id, meta.type, draft.fields);
  delete state.ticketDrafts[channel.id];
  await saveState();
  await channel.send({ content: `GitHub issue created: ${issue.url}. Only the submitted form was copied; private replies stay here.`, allowedMentions: { parse: [] } });
  await logTicket(channel.guild, `Linked ${channel} to GitHub issue ${issue.url}.`);
  return issue;
}

export async function closeTicket(channel, actor) {
  const meta = ticketMeta(channel);
  if (!meta || meta.status !== 'open') throw new Error('This is not an open A.T.L.A.S. ticket.');
  const member = await channel.guild.members.fetch(actor.id);
  const staff = member.roles.cache.some((role) => role.name === STAFF_ROLE);
  if (actor.id !== meta.ownerId && actor.id !== channel.guild.ownerId && !staff) {
    throw new Error('Only the requester or staff can close this ticket.');
  }
  await channel.setTopic(`atlas-ticket owner=${meta.ownerId} type=${meta.type} status=closed`, 'A.T.L.A.S. ticket closed');
  await channel.permissionOverwrites.edit(meta.ownerId, { ViewChannel: false }, { reason: 'A.T.L.A.S. ticket closed' });
  await channel.setName(`closed-${meta.type}-${meta.ownerId.slice(-6)}`, 'A.T.L.A.S. ticket closed');
  await logTicket(channel.guild, `Closed ${meta.type} ticket ${channel} for <@${meta.ownerId}> by ${actor.tag}.`);
}

export async function adoptLegacyTicket(channel, actor, ownerId) {
  if (channel.guildId !== GUILD_ID || channel.parentId !== SUPPORT_CATEGORY_ID || !channel.name.startsWith('ticket-')) {
    throw new Error('Run this only in an open legacy support ticket.');
  }
  if (actor.id !== channel.guild.ownerId) throw new Error('Only the server owner can adopt a legacy ticket.');
  if (ticketMeta(channel)) throw new Error('A.T.L.A.S. already manages this ticket.');
  const member = await channel.guild.members.fetch(ownerId);
  if (!channel.permissionsFor(member)?.has(PermissionFlagsBits.ViewChannel)) {
    throw new Error('The selected member cannot view this ticket.');
  }
  await channel.setTopic(`atlas-ticket owner=${ownerId} type=general status=open legacy=ticket-tool`,
    'A.T.L.A.S. adopted an open Ticket Tool channel');
  await logTicket(channel.guild, `Adopted legacy ticket ${channel} for <@${ownerId}>; prior messages stay private.`);
  return channel;
}
