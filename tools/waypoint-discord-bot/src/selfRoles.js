import { ActionRowBuilder, StringSelectMenuBuilder } from 'discord.js';
import { LANE_ROLES, WORLD_ROLES } from './config.js';

export function roleMenuRows(member) {
  const groups = [LANE_ROLES, WORLD_ROLES.slice(0, 19), WORLD_ROLES.slice(19)];
  return groups.map((group, index) => {
    const menu = new StringSelectMenuBuilder()
      .setCustomId(`atlas:roles:${index}`)
      .setPlaceholder(index === 0 ? 'Your lanes' : `World clubs ${index} of 2`)
      .setMinValues(0)
      .setMaxValues(group.length)
      .addOptions(group.map((name) => ({
        label: name.replace(/^\S+\s/, ''),
        value: name,
        default: member.roles.cache.some((role) => role.name === name),
      })));
    return new ActionRowBuilder().addComponents(menu);
  });
}

export async function applyRoleSelection(interaction) {
  const groupIndex = Number(interaction.customId.split(':')[2]);
  const groups = [LANE_ROLES, WORLD_ROLES.slice(0, 19), WORLD_ROLES.slice(19)];
  const group = groups[groupIndex];
  if (!group) throw new Error('Unknown role menu.');
  const member = await interaction.guild.members.fetch(interaction.user.id);
  const roles = await interaction.guild.roles.fetch();
  for (const name of group) {
    const role = roles.find((item) => item.name === name);
    if (!role || !role.editable) throw new Error(`Cannot change self-serve role: ${name}`);
    if (interaction.values.includes(name) && !member.roles.cache.has(role.id)) {
      await member.roles.add(role, 'A.T.L.A.S. self-serve role selection');
    } else if (!interaction.values.includes(name) && member.roles.cache.has(role.id)) {
      await member.roles.remove(role, 'A.T.L.A.S. self-serve role selection');
    }
  }
  const refreshed = await interaction.guild.members.fetch({ user: member.id, force: true });
  return roleMenuRows(refreshed);
}
