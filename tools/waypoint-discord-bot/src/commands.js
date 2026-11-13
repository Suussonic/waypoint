import {
  ActionRowBuilder, EmbedBuilder, Events, MessageFlags, ModalBuilder,
  SlashCommandBuilder, TextInputBuilder, TextInputStyle,
} from 'discord.js';
import { GUILD_ID, STAFF_ROLE } from './config.js';
import { leaderboard, rankFor, setXpByOwner, welcomePayload } from './leveling.js';
import { applyRoleSelection, roleMenuRows } from './selfRoles.js';
import { adoptLegacyTicket, closeTicket, openTicket, syncTicketIssue } from './tickets.js';
import { helpButtons } from './panels.js';

const privateReply = (content, extras = {}) => ({ content, flags: MessageFlags.Ephemeral, ...extras });

const commands = [
  new SlashCommandBuilder().setName('help').setDescription('Open A.T.L.A.S. help and quick actions'),
  new SlashCommandBuilder().setName('roles').setDescription('Choose your Waypoint lanes and world clubs'),
  new SlashCommandBuilder().setName('rank').setDescription('Check your Waypoint community XP')
    .addUserOption((option) => option.setName('member').setDescription('Member to check')),
  new SlashCommandBuilder().setName('leaderboard').setDescription('See Waypoint community rankings')
    .addStringOption((option) => option.setName('period').setDescription('Daily or all-time').addChoices(
      { name: 'Today (IST)', value: 'daily' }, { name: 'All-time', value: 'all-time' },
    )),
  new SlashCommandBuilder().setName('quests').setDescription('See how community activity earns quest points'),
  new SlashCommandBuilder().setName('ticket').setDescription('Open a private Waypoint support ticket')
    .addStringOption((option) => option.setName('type').setDescription('What do you need help with?').setRequired(true)
      .addChoices(
        { name: 'Bug', value: 'bug' },
        { name: 'Feature', value: 'feature' },
        { name: 'General help', value: 'general' },
      )),
  new SlashCommandBuilder().setName('announcements').setDescription('Share a milestone announcement in this channel'),
  new SlashCommandBuilder().setName('close').setDescription('Close this A.T.L.A.S. support ticket'),
  new SlashCommandBuilder().setName('adopt-ticket').setDescription('Owner only: let A.T.L.A.S. manage an open Ticket Tool channel')
    .addUserOption((option) => option.setName('member').setDescription('Original ticket requester').setRequired(true)),
  new SlashCommandBuilder().setName('sync-issue').setDescription('Staff only: retry GitHub issue creation for this ticket'),
  new SlashCommandBuilder().setName('xp-set').setDescription('Owner only: migrate or correct community XP')
    .addUserOption((option) => option.setName('member').setDescription('Member').setRequired(true))
    .addIntegerOption((option) => option.setName('xp').setDescription('Total XP').setMinValue(0).setRequired(true)),
  new SlashCommandBuilder().setName('welcome-preview').setDescription('Owner only: preview the next A.T.L.A.S. welcome card'),
];

function ticketModal(type) {
  const publicIssue = type !== 'general';
  const modal = new ModalBuilder()
    .setCustomId(`atlas:ticket:${type}`)
    .setTitle(publicIssue ? `${type === 'bug' ? 'Bug' : 'Feature'} ticket → public GitHub issue` : 'Private Waypoint help ticket');
  const inputs = [
    new TextInputBuilder().setCustomId('title').setLabel(publicIssue ? 'Public issue title' : 'Ticket title')
      .setStyle(TextInputStyle.Short).setMinLength(5).setMaxLength(100).setRequired(true),
    new TextInputBuilder().setCustomId('description').setLabel(publicIssue ? 'Public description (no private info)' : 'What do you need help with?')
      .setStyle(TextInputStyle.Paragraph).setMinLength(10).setMaxLength(1800).setRequired(true),
  ];
  if (publicIssue) {
    inputs.push(new TextInputBuilder().setCustomId('detail')
      .setLabel(type === 'bug' ? 'Steps to reproduce' : 'Why would this help?')
      .setStyle(TextInputStyle.Paragraph).setMaxLength(1000).setRequired(false));
  }
  if (type === 'bug') {
    inputs.push(new TextInputBuilder().setCustomId('device').setLabel('App version, device, Android version')
      .setStyle(TextInputStyle.Short).setMaxLength(150).setRequired(false));
  }
  modal.addComponents(inputs.map((input) => new ActionRowBuilder().addComponents(input)));
  return modal;
}

function announcementModal() {
  const modal = new ModalBuilder()
    .setCustomId('atlas:announcements')
    .setTitle('Share a milestone');
  const inputs = [
    new TextInputBuilder().setCustomId('title').setLabel('Milestone headline')
      .setStyle(TextInputStyle.Short).setPlaceholder('e.g. Our 1,000th community quest completed')
      .setMinLength(3).setMaxLength(100).setRequired(true),
    new TextInputBuilder().setCustomId('description').setLabel('What did the community achieve?')
      .setStyle(TextInputStyle.Paragraph).setPlaceholder('Share what happened. Use **bold** for key figures or *italics* for context.')
      .setMinLength(10).setMaxLength(1800).setRequired(true),
    new TextInputBuilder().setCustomId('impact').setLabel('Why it matters or what is next (optional)')
      .setStyle(TextInputStyle.Paragraph).setPlaceholder('What does this unlock, or what comes next?')
      .setMaxLength(1000).setRequired(false),
    new TextInputBuilder().setCustomId('link').setLabel('Related page URL (optional)')
      .setStyle(TextInputStyle.Short).setPlaceholder('https://…').setMaxLength(300).setRequired(false),
  ];
  modal.addComponents(inputs.map((input) => new ActionRowBuilder().addComponents(input)));
  return modal;
}

async function canAnnounce(interaction) {
  if (interaction.user.id === interaction.guild.ownerId) return true;
  const member = await interaction.guild.members.fetch(interaction.user.id);
  return member.roles.cache.some((role) => role.name === STAFF_ROLE);
}

async function rankText(userId) {
  const rank = await rankFor(userId);
  return `<@${userId}> is level **${rank.level}** with **${rank.xp} total quest points** and **${rank.today} today**. Next level: **${rank.next} points**. Discord community points are separate from the app's on-device quest XP.`;
}

async function handle(interaction) {
  if (!interaction.inGuild() || interaction.guildId !== GUILD_ID) return;
  if (interaction.isModalSubmit() && interaction.customId === 'atlas:announcements') {
    if (!await canAnnounce(interaction)) {
      await interaction.reply(privateReply('Only the server owner or Mod Squad can post milestone announcements.'));
      return;
    }
    if (!interaction.channel?.isTextBased() || typeof interaction.channel.send !== 'function') {
      await interaction.reply(privateReply('Run /announcements in a text channel where A.T.L.A.S. can post.'));
      return;
    }
    const title = interaction.fields.getTextInputValue('title').trim();
    const description = interaction.fields.getTextInputValue('description').trim();
    const impact = interaction.fields.getTextInputValue('impact').trim();
    const link = interaction.fields.getTextInputValue('link').trim();
    let linkUrl = '';
    if (link) {
      try {
        const parsedUrl = new URL(link);
        if (!['http:', 'https:'].includes(parsedUrl.protocol)) throw new Error('Unsupported URL protocol.');
        linkUrl = parsedUrl.href;
      } catch {
        await interaction.reply(privateReply('Add a valid http or https URL for the related page, or leave the field blank.'));
        return;
      }
    }
    const displayName = interaction.member?.displayName ?? interaction.user.username;
    const embed = new EmbedBuilder()
      .setColor(0xd5f16c)
      .setTitle(title)
      .setDescription(description)
      .setFooter({ text: `Shared by ${displayName} · Waypoint Community` })
      .setTimestamp();
    if (impact) embed.addFields({ name: '✨ Why it matters / what’s next', value: impact });
    if (linkUrl) embed.addFields({ name: '🔗 Read the related update', value: `[Open milestone details](<${linkUrl}>)` });
    await interaction.deferReply({ flags: MessageFlags.Ephemeral });
    await interaction.channel.send({ embeds: [embed], allowedMentions: { parse: [] } });
    await interaction.editReply('Milestone announcement posted in this channel.');
    return;
  }
  if (interaction.isModalSubmit() && interaction.customId.startsWith('atlas:ticket:')) {
    const type = interaction.customId.split(':')[2];
    const fields = {
      title: interaction.fields.getTextInputValue('title'),
      description: interaction.fields.getTextInputValue('description'),
      detail: type === 'general' ? '' : interaction.fields.getTextInputValue('detail'),
      device: type === 'bug' ? interaction.fields.getTextInputValue('device') : '',
    };
    await interaction.deferReply({ flags: MessageFlags.Ephemeral });
    const result = await openTicket(interaction.guild, interaction.user, type, fields);
    const issueLine = result.issue ? ` GitHub issue: ${result.issue.url}` : result.issueError ? ' GitHub sync needs staff attention.' : '';
    await interaction.editReply(`${result.created ? 'Created' : 'You already have'} a private ticket: ${result.channel}.${issueLine}`);
    return;
  }
  if (interaction.isStringSelectMenu() && interaction.customId.startsWith('atlas:roles:')) {
    await interaction.deferUpdate();
    const rows = await applyRoleSelection(interaction);
    await interaction.editReply({ content: 'Your roles are updated. Select every role you want on each page.', components: rows });
    return;
  }
  if (interaction.isButton()) {
    if (interaction.customId.startsWith('atlas:ticket:open:')) {
      await interaction.showModal(ticketModal(interaction.customId.split(':')[3]));
    } else if (interaction.customId === 'atlas:help:roles') {
      const member = await interaction.guild.members.fetch(interaction.user.id);
      await interaction.reply(privateReply('Choose your lanes and world clubs. Select every role you want on each page.', { components: roleMenuRows(member) }));
    } else if (interaction.customId === 'atlas:help:rank') {
      await interaction.reply(privateReply(await rankText(interaction.user.id)));
    } else if (interaction.customId === 'atlas:help:ticket') {
      await interaction.showModal(ticketModal('general'));
    }
    return;
  }
  if (!interaction.isChatInputCommand()) return;
  switch (interaction.commandName) {
    case 'help':
      await interaction.reply(privateReply('A.T.L.A.S. can help with world roles, quests, community rank, and private support. Use the buttons below or /roles, /quests, /rank, /leaderboard, and /ticket.', { components: helpButtons() }));
      break;
    case 'roles': {
      const member = await interaction.guild.members.fetch(interaction.user.id);
      await interaction.reply(privateReply('Choose your lanes and world clubs. Select every role you want on each page.', { components: roleMenuRows(member) }));
      break;
    }
    case 'rank': {
      const user = interaction.options.getUser('member') ?? interaction.user;
      await interaction.reply(privateReply(await rankText(user.id)));
      break;
    }
    case 'leaderboard': {
      const period = interaction.options.getString('period') ?? 'daily';
      const entries = await leaderboard(period);
      const lines = entries.map((entry) => `${entry.position}. <@${entry.userId}> — level ${entry.level}, ${entry.xp} ${period === 'daily' ? 'today' : 'total'} points`);
      await interaction.reply(privateReply(lines.length ? `**Waypoint ${period === 'daily' ? 'daily (IST)' : 'all-time'} leaderboard**\n${lines.join('\n')}` : 'No quest points have been recorded for this period yet.'));
      break;
    }
    case 'quests': {
      const rank = await rankFor(interaction.user.id);
      await interaction.reply(privateReply(`**Community quests**\n• Chat with members: 15–40 points per eligible message, at most once a minute.\n• React to someone else's message: 25 points for you and its author, at most once every 5 minutes and 20 awards a day.\n• Start a helpful bug, feature, or world-idea post: 35 points, up to 3 a day.\nYour points today: **${rank.today}**. The daily leaderboard resets at midnight IST; your all-time level keeps growing.`));
      break;
    }
    case 'ticket': {
      const type = interaction.options.getString('type', true);
      await interaction.showModal(ticketModal(type));
      break;
    }
    case 'announcements':
      if (!await canAnnounce(interaction)) {
        await interaction.reply(privateReply('Only the server owner or Mod Squad can post milestone announcements.'));
        break;
      }
      await interaction.showModal(announcementModal());
      break;
    case 'close': {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const closedNow = await closeTicket(interaction.channel, interaction.user);
      await interaction.editReply(closedNow
        ? 'Ticket closed. Staff can still read its history.'
        : 'This ticket is already closed. Staff can still read its history.');
      break;
    }
    case 'adopt-ticket': {
      const user = interaction.options.getUser('member', true);
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await adoptLegacyTicket(interaction.channel, interaction.user, user.id);
      await interaction.editReply('A.T.L.A.S. now manages this existing private ticket. Its past messages remain here; no GitHub issue was created.');
      break;
    }
    case 'sync-issue': {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const issue = await syncTicketIssue(interaction.channel, interaction.user);
      await interaction.editReply(`GitHub issue linked: ${issue.url}`);
      break;
    }
    case 'xp-set': {
      if (interaction.user.id !== interaction.guild.ownerId) throw new Error('Only the server owner can change XP.');
      const user = interaction.options.getUser('member', true);
      const xp = interaction.options.getInteger('xp', true);
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await setXpByOwner(interaction.guild, user.id, xp);
      await interaction.editReply(`Set ${user}'s community XP to ${xp}.`);
      break;
    }
    case 'welcome-preview': {
      if (interaction.user.id !== interaction.guild.ownerId) throw new Error('Only the server owner can preview welcomes.');
      const member = await interaction.guild.members.fetch(interaction.user.id);
      await interaction.reply(privateReply('This is how A.T.L.A.S. welcomes a new explorer.', {
        embeds: welcomePayload(member).embeds,
      }));
      break;
    }
    default:
      break;
  }
}

export function registerCommands(client) {
  client.once(Events.ClientReady, async () => {
    try {
      const guild = await client.guilds.fetch(GUILD_ID);
      await guild.commands.set(commands.map((command) => command.toJSON()));
      console.log(`A.T.L.A.S. registered ${commands.length} guild commands.`);
    } catch (error) {
      console.error('[A.T.L.A.S. command registration]', error);
    }
  });
  client.on(Events.InteractionCreate, async (interaction) => {
    try {
      await handle(interaction);
    } catch (error) {
      console.error('[A.T.L.A.S. interaction]', error);
      if (!interaction.isRepliable()) return;
      const response = privateReply(`A.T.L.A.S. could not complete that: ${error.message}`);
      if (interaction.deferred && !interaction.replied) await interaction.editReply(response.content).catch(console.error);
      else if (interaction.replied) await interaction.followUp(response).catch(console.error);
      else await interaction.reply(response).catch(console.error);
    }
  });
}
