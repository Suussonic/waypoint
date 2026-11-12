import {
  ChannelType,
  Client,
  Events,
  GatewayIntentBits,
  Partials,
  PermissionFlagsBits,
} from 'discord.js';
import { oldestPending, requestedRole, statusOf, withStatus } from './rules.js';
import { GUILD_ID, ROLE_FORUM_ID } from './config.js';
import { registerCommands } from './commands.js';
import { awardForumPostXp, awardMessageXp, awardReactionXp, postDailyLeaderboard, welcomeMember } from './leveling.js';
import { ensurePanels } from './panels.js';

const token = process.env.DISCORD_BOT_TOKEN;

if (!token) throw new Error('Set DISCORD_BOT_TOKEN before starting the bot.');

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.GuildMessageReactions,
  ],
  partials: [Partials.Message, Partials.Channel, Partials.Reaction],
});

const leavingGuilds = new Set();
async function leaveOtherGuild(guild) {
  if (guild.id === GUILD_ID || leavingGuilds.has(guild.id)) return;
  leavingGuilds.add(guild.id);
  try {
    console.warn(`[A.T.L.A.S.] Leaving unauthorized server ${guild.id}`);
    await guild.leave();
  } finally {
    leavingGuilds.delete(guild.id);
  }
}

client.on(Events.GuildCreate, (guild) => {
  leaveOtherGuild(guild).catch((error) => console.error('[A.T.L.A.S. guild restriction]', error));
});

registerCommands(client);

client.on(Events.MessageCreate, (message) => {
  serial(() => awardMessageXp(message));
});

client.on(Events.MessageReactionAdd, (reaction, user) => {
  serial(() => awardReactionXp(reaction, user));
});

client.on(Events.ThreadCreate, (thread, newlyCreated) => {
  if (newlyCreated) serial(() => awardForumPostXp(thread));
});

client.on(Events.GuildMemberAdd, (member) => {
  serial(() => welcomeMember(member));
});

// Serialize decisions and duplicate checks, including two posts created at once.
let queue = Promise.resolve();
function serial(task) {
  queue = queue.then(task).catch((error) => console.error('[Waypoint bot]', error));
}

async function forum() {
  const channel = await client.channels.fetch(ROLE_FORUM_ID);
  if (!channel || channel.type !== ChannelType.GuildForum || channel.guildId !== GUILD_ID) {
    throw new Error('Configured role-requests forum was not found.');
  }
  return channel;
}

function statusTags(channel) {
  const names = { pending: '⏳ Pending', approved: '✅ Approved', declined: '❌ Declined' };
  const tags = Object.fromEntries(
    Object.entries(names).map(([key, name]) => [
      key,
      channel.availableTags.find((tag) => tag.name === name)?.id,
    ]),
  );
  if (Object.values(tags).some((id) => !id)) {
    throw new Error('Role forum needs Pending, Approved and Declined tags.');
  }
  return tags;
}

async function allForumThreads(channel) {
  const active = await channel.threads.fetchActive();
  const threads = [...active.threads.values()];
  let before;
  for (;;) {
    const page = await channel.threads.fetchArchived({ type: 'public', limit: 100, before });
    const items = [...page.threads.values()];
    threads.push(...items);
    if (!page.hasMore || items.length === 0) break;
    const oldest = Math.min(...items.map((item) => item.archiveTimestamp));
    if (!Number.isFinite(oldest) || before?.getTime() === oldest - 1) {
      throw new Error('Could not page through archived role requests safely.');
    }
    before = new Date(oldest - 1);
  }
  return threads;
}

async function rejectDuplicate(thread, first, tags) {
  await thread.send({
    content: `You already have a pending role request: ${first.url}. Please continue there. You can open another after it is approved or declined.`,
    allowedMentions: { parse: [] },
  });
  await thread.setAppliedTags(withStatus(thread, tags, 'declined'), 'Duplicate pending request');
  await thread.setLocked(true, 'Duplicate pending request');
  await thread.setArchived(true, 'Duplicate pending request');
}

client.on(Events.ThreadCreate, (thread, newlyCreated) => {
  if (!newlyCreated || thread.parentId !== ROLE_FORUM_ID || thread.guildId !== GUILD_ID) return;
  serial(async () => {
    const channel = await forum();
    const tags = statusTags(channel);
    const pending = oldestPending(await allForumThreads(channel), tags);
    const first = pending.get(thread.ownerId);
    if (first && first.id !== thread.id) await rejectDuplicate(thread, first, tags);
    else if (statusOf(thread, tags) === 'pending' && !thread.appliedTags.includes(tags.pending)) {
      await thread.setAppliedTags(withStatus(thread, tags, 'pending'), 'New role request');
    }
  });
});

async function resolveRoleRequest(thread, emoji) {
  const channel = await forum();
  const guild = channel.guild;
  const tags = statusTags(channel);
  if (statusOf(thread, tags) !== 'pending') return;
  if (thread.archived) await thread.setArchived(false, 'Resolve owner reaction on role request');
  if (emoji === '✅') {
    const roleName = requestedRole(thread.name);
    if (!roleName) {
      await thread.send('Approval paused: rename this post to the exact requestable role name, then add ✅ again.');
      const starter = await thread.fetchStarterMessage();
      await starter.reactions.resolve('✅')?.users.remove(guild.ownerId);
      return;
    }
    const roles = await guild.roles.fetch();
    const role = roles.find((item) => item.name === roleName);
    if (!role || !role.editable || role.permissions.has(PermissionFlagsBits.Administrator)) {
      await thread.send('Approval paused: the requested role is missing, above the bot, or has Administrator permission. The server owner must check the role hierarchy.');
      const starter = await thread.fetchStarterMessage();
      await starter.reactions.resolve('✅')?.users.remove(guild.ownerId);
      return;
    }
    const member = await guild.members.fetch(thread.ownerId);
    if (!member.roles.cache.has(role.id)) await member.roles.add(role, `Owner approved role request ${thread.id}`);
    await thread.setAppliedTags(withStatus(thread, tags, 'approved'), 'Owner approved role request');
    await thread.send({ content: `Approved: ${role.name} was granted to the post author.`, allowedMentions: { parse: [] } });
  } else {
    await thread.setAppliedTags(withStatus(thread, tags, 'declined'), 'Owner declined role request');
    await thread.send('Declined by the server owner. You may submit another request.');
  }
  await thread.setLocked(true, 'Role request resolved');
  await thread.setArchived(true, 'Role request resolved');
}

client.on(Events.MessageReactionAdd, (reaction, user) => {
  if (user.bot || !['✅', '❌'].includes(reaction.emoji.name)) return;
  serial(async () => {
    if (reaction.partial) reaction = await reaction.fetch();
    const thread = await client.channels.fetch(reaction.message.channelId);
    if (!thread?.isThread() || thread.parentId !== ROLE_FORUM_ID || thread.guildId !== GUILD_ID) return;
    const starter = await thread.fetchStarterMessage();
    if (!starter || reaction.message.id !== starter.id) return;
    if (user.id !== thread.guild.ownerId) {
      // Discord cannot natively make an existing emoji reaction owner-only.
      await reaction.users.remove(user.id);
      return;
    }
    await resolveRoleRequest(thread, reaction.emoji.name);
  });
});

async function reconcileOwnerReactions(channel) {
  const tags = statusTags(channel);
  for (const thread of await allForumThreads(channel)) {
    if (statusOf(thread, tags) !== 'pending') continue;
    try {
      const starter = await thread.fetchStarterMessage();
      if (!starter) continue;
      for (const emoji of ['✅', '❌']) {
        const reaction = starter.reactions.resolve(emoji);
        if (!reaction) continue;
        const users = await reaction.users.fetch({ limit: 100 });
        if (users.has(channel.guild.ownerId)) {
          await resolveRoleRequest(thread, emoji);
          break;
        }
      }
    } catch (error) {
      console.error(`[A.T.L.A.S. role request ${thread.id}]`, error);
    }
  }
}

client.once(Events.ClientReady, async () => {
  try {
    const departures = await Promise.allSettled([...client.guilds.cache.values()].map(leaveOtherGuild));
    for (const departure of departures) {
      if (departure.status === 'rejected') console.error('[A.T.L.A.S. guild restriction]', departure.reason);
    }
    const channel = await forum();
    statusTags(channel);
    console.log(`A.T.L.A.S. ready in Waypoint as ${client.user.tag}; owner ${channel.guild.ownerId}`);
    serial(() => reconcileOwnerReactions(channel));
    serial(() => postDailyLeaderboard(client));
    serial(() => ensurePanels(client));
    setInterval(() => serial(() => postDailyLeaderboard(client)), 60_000);
  } catch (error) {
    console.error(error);
    process.exitCode = 1;
    client.destroy();
  }
});

await client.login(token);
