import {
  ActionRowBuilder, Events, MessageFlags, ModalBuilder,
  SlashCommandBuilder, TextInputBuilder, TextInputStyle,
} from 'discord.js';
import { GUILD_ID } from './config.js';
import { leaderboard, rankFor, setXpByOwner } from './leveling.js';
import { applyRoleSelection, roleMenuRows } from './selfRoles.js';
import { closeTicket, openTicket, syncTicketIssue } from './tickets.js';
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
  new SlashCommandBuilder().setName('close').setDescription('Close this A.T.L.A.S. support ticket'),
  new SlashCommandBuilder().setName('sync-issue').setDescription('Staff only: retry GitHub issue creation for this ticket'),
  new SlashCommandBuilder().setName('xp-set').setDescription('Owner only: migrate or correct community XP')
    .addUserOption((option) => option.setName('member').setDescription('Member').setRequired(true))
    .addIntegerOption((option) => option.setName('xp').setDescription('Total XP').setMinValue(0).setRequired(true)),
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

async function rankText(userId) {
  const rank = await rankFor(userId);
  return `<@${userId}> is level **${rank.level}** with **${rank.xp} total quest points** and **${rank.today} today**. Next level: **${rank.next} points**. Discord community points are separate from the app's on-device quest XP.`;
}

async function handle(interaction) {
  if (!interaction.inGuild() || interaction.guildId !== GUILD_ID) return;
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
      await interaction.reply(privateReply(`**Community quests**\n• Chat with members: 20 points per eligible message, at most once a minute.\n• React to someone else's message: 5 points, at most once every 5 minutes and 20 times a day.\n• Start a helpful bug, feature, or world-idea post: 35 points, up to 3 a day.\nYour points today: **${rank.today}**. The daily leaderboard resets at midnight IST; your all-time level keeps growing.`));
      break;
    }
    case 'ticket': {
      const type = interaction.options.getString('type', true);
      await interaction.showModal(ticketModal(type));
      break;
    }
    case 'close':
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await closeTicket(interaction.channel, interaction.user);
      await interaction.editReply('Ticket closed. Staff can still read its history.');
      break;
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
