import { ActionRowBuilder, ButtonBuilder, ButtonStyle, ChannelType } from 'discord.js';
import { BUG_FORUM_ID, FEATURE_FORUM_ID, HELP_CHANNEL_ID, ROLE_PICK_CHANNEL_ID } from './config.js';
import { loadState, saveState } from './store.js';

export function helpButtons() {
  return [
    new ActionRowBuilder().addComponents(
      new ButtonBuilder().setCustomId('atlas:help:roles').setLabel('Pick roles').setStyle(ButtonStyle.Primary),
      new ButtonBuilder().setCustomId('atlas:help:rank').setLabel('Check rank').setStyle(ButtonStyle.Secondary),
      new ButtonBuilder().setCustomId('atlas:help:ticket').setLabel('Private help').setStyle(ButtonStyle.Success),
    ),
    new ActionRowBuilder().addComponents(
      new ButtonBuilder().setCustomId('atlas:ticket:open:bug').setLabel('Report a bug').setStyle(ButtonStyle.Secondary),
      new ButtonBuilder().setCustomId('atlas:ticket:open:feature').setLabel('Suggest a feature').setStyle(ButtonStyle.Secondary),
    ),
  ];
}

async function ensureHelpPanel(client, state) {
  const channel = await client.channels.fetch(HELP_CHANNEL_ID);
  if (!channel?.isTextBased()) throw new Error('Help channel is unavailable.');
  const content = '**A.T.L.A.S. help desk**\nPick world roles, check your community rank, or open a private ticket. Bug and feature ticket forms create public GitHub issues from the title and description you submit. General help stays private.';
  const existing = state.helpPanelId ? await channel.messages.fetch(state.helpPanelId).catch(() => null) : null;
  if (existing?.author.id === client.user.id) {
    await existing.edit({ content, components: helpButtons() });
    return;
  }
  const sent = await channel.send({ content, components: helpButtons() });
  state.helpPanelId = sent.id;
  await saveState();
}

async function ensureRolePanel(client, state) {
  const channel = await client.channels.fetch(ROLE_PICK_CHANNEL_ID);
  if (!channel?.isTextBased()) throw new Error('Role selection channel is unavailable.');
  const content = '**Choose your Waypoint roles with A.T.L.A.S.**\nPick the world clubs you want to visit and your activity lanes. You can change them any time. Level rewards and staff roles are assigned separately; request a staff role in the role-requests forum.';
  const components = [new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('atlas:help:roles').setLabel('Pick or change roles').setStyle(ButtonStyle.Primary),
  )];
  const existing = state.rolePanelId ? await channel.messages.fetch(state.rolePanelId).catch(() => null) : null;
  if (existing?.author.id === client.user.id) {
    await existing.edit({ content, components });
    if (!existing.pinned) await existing.pin('Keep A.T.L.A.S. role selection visible').catch((error) => {
      console.warn('[A.T.L.A.S. role panel pin]', error.message);
    });
    return;
  }
  const recent = await channel.messages.fetch({ limit: 30 });
  const prior = recent.find((message) => message.author.id === client.user.id && message.content.startsWith('**Choose your Waypoint roles with A.T.L.A.S.**'));
  if (prior) {
    await prior.edit({ content, components });
    state.rolePanelId = prior.id;
    await saveState();
    if (!prior.pinned) await prior.pin('Keep A.T.L.A.S. role selection visible').catch((error) => {
      console.warn('[A.T.L.A.S. role panel pin]', error.message);
    });
    return;
  }
  const sent = await channel.send({ content, components });
  state.rolePanelId = sent.id;
  await saveState();
  await sent.pin('Keep A.T.L.A.S. role selection visible').catch((error) => {
    console.warn('[A.T.L.A.S. role panel pin]', error.message);
  });
}

async function ensureForumPanel(client, state, type, forumId) {
  state.forumPanels ??= {};
  const forum = await client.channels.fetch(forumId);
  if (forum?.type !== ChannelType.GuildForum) throw new Error(`${type} forum is unavailable.`);
  const initialTagName = type === 'bug' ? '🆕 New' : '💡 Proposed';
  const initialTag = forum.availableTags.find((tag) => tag.name === initialTagName);
  if (!initialTag) throw new Error(`${type} forum needs the ${initialTagName} tag.`);
  const oldId = state.forumPanels[type];
  const old = oldId ? await forum.threads.fetch(oldId).catch(() => null) : null;
  if (old?.ownerId === client.user.id) {
    if (!old.appliedTags.includes(initialTag.id)) await old.setAppliedTags([initialTag.id], 'Mark A.T.L.A.S. entry post');
    if (old.archived) await old.setArchived(false, 'Keep A.T.L.A.S. ticket entry visible');
    return;
  }
  const title = type === 'bug' ? 'Report a bug with A.T.L.A.S. (start here)' : 'Suggest a feature with A.T.L.A.S. (start here)';
  const content = type === 'bug'
    ? 'For a public discussion, click New Post in this forum. For a private bug ticket, use the button below. The title and description you submit in the form will become a public GitHub issue; your private ticket replies stay in Discord.'
    : 'For a public discussion, click New Post in this forum. For a private feature ticket, use the button below. The title and description you submit in the form will become a public GitHub issue; your private ticket replies stay in Discord.';
  const thread = await forum.threads.create({
    name: title,
    appliedTags: [initialTag.id],
    message: {
      content,
      components: [new ActionRowBuilder().addComponents(
        new ButtonBuilder().setCustomId(`atlas:ticket:open:${type}`)
          .setLabel(type === 'bug' ? 'Open bug ticket' : 'Open feature ticket')
          .setStyle(ButtonStyle.Primary),
      )],
    },
  });
  state.forumPanels[type] = thread.id;
  await saveState();
}

export async function ensurePanels(client) {
  const state = await loadState();
  for (const task of [
    () => ensureHelpPanel(client, state),
    () => ensureRolePanel(client, state),
    () => ensureForumPanel(client, state, 'bug', BUG_FORUM_ID),
    () => ensureForumPanel(client, state, 'feature', FEATURE_FORUM_ID),
  ]) {
    try {
      await task();
    } catch (error) {
      console.error('[A.T.L.A.S. panel]', error);
    }
  }
}
