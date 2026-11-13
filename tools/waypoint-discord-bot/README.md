# A.T.L.A.S. for Waypoint

A.T.L.A.S. is Waypoint's single Discord community bot. It handles welcomes, a base member role, 38 self-serve world roles, lane roles, activity-based quest points, level roles, a daily leaderboard, milestone announcements, private support tickets, GitHub issues for bug and feature tickets, and owner-only role request decisions.

The existing Moderator request was already granted to its author and tagged Approved. For each new pending request, A.T.L.A.S. posts an approval card naming the exact role and applicant. On startup, it checks owner reactions on those cards so decisions made while it was offline are not lost.

The Discord application is private: **Public Bot** is off, its install link is **None**, and User Install is off. Only the application owner can invite it. Commands and events are limited to the Waypoint guild, and the bot leaves any other guild if it is added there. Discord app verification requires an install link and would prevent turning Public Bot off later, so this bot remains unverified while it is private.

## Commands and behavior

| Command | What it does |
| --- | --- |
| `/help` | Interactive buttons for roles, rank, and a private help ticket |
| `/roles` | Select lanes and any of the 38 world club roles |
| `/quests` | Show how Discord activity earns community points |
| `/rank [member]` | Show total points, today's points, level, and next threshold |
| `/leaderboard [period]` | Daily IST board by default, or all-time board |
| `/ticket type` | Structured ticket form; bug and feature forms create public GitHub issues |
| `/announcements` | Owner or Mod Squad opens a milestone form and posts a formatted `@everyone` announcement in the current channel |
| `/close` | Requester or staff closes an A.T.L.A.S. ticket |
| `/adopt-ticket member` | Owner moves an existing Ticket Tool channel under A.T.L.A.S. without publishing its history |
| `/sync-issue` | Staff retries GitHub issue creation if it failed |
| `/xp-set member xp` | Server owner migrates or corrects prior Arcane XP |
| `/welcome-preview` | Owner previews A.T.L.A.S.'s personalized welcome card privately |

Messages earn 15–40 points at most once a minute. Reacting to someone else's message earns 25 points for both people, at most once every five minutes and 20 awards a day per person. A new bug, feature, or world idea forum post earns 35 points, capped at three per day. Private ticket messages and role-picker reactions do not earn points. The daily board resets at midnight Asia/Kolkata; a summary is posted in `⚔️│quests-and-xp`. All-time points continue accumulating. A.T.L.A.S. uses Arcane's former linear level curve so migrated ranks stay consistent. Level roles unlock at 5, 10, 20, and 35. This Discord score is separate from private quest XP in the Android app.

The pinned A.T.L.A.S. button in `🎯│pick-your-roles` opens menus for all 38 world clubs and both activity lanes. Existing role assignments stay in place. New members get a themed, personalized welcome card with their avatar and first steps in `👋│welcome`; existing welcome messages stay in place.

`🙏│role-requests` permits one pending post per member. A duplicate is marked Declined, closed, and linked to the first pending post. The post title must exactly name one requestable role. A.T.L.A.S. then posts an approval card that fixes the role and applicant; later title edits cannot change that decision. Only a ✅ or ❌ from the **server owner** on the **A.T.L.A.S. approval card** counts. The bot removes those reactions from other members. Owner ✅ grants the card's role to the post author, marks Approved, and closes the post; ❌ marks Declined and closes it. The allowlist is in `src/rules.js`. The bot refuses roles with Administrator permission or roles above its own role.

For bug and feature tickets, the form labels the title and description as public. Only those submitted fields, plus optional steps and device information, are copied to [the Waypoint GitHub repository](https://github.com/the-geeky-couldron/waypoint/issues). Private replies are never copied. General help tickets stay entirely in Discord. GitHub issue creation is idempotent by ticket ID and can be retried by staff.

The public bug and feature forums require a starting status tag on New Posts: **🆕 New** for bugs and **💡 Proposed** for features. Staff can then apply the moderated Investigating/Fixed/Duplicate/Won’t fix or Planned/In progress/Shipped/Declined tags. A.T.L.A.S. tags its own start-here posts, and the existing “Google maps API” post is marked Proposed.

## Installation

The [A.T.L.A.S. Discord application](https://discord.com/developers/applications/1570323716204658781/) already exists. Its avatar and banner are in `assets/`.

1. Enable **Server Members Intent** for join events. Leave Message Content and Presence intents off. Use only the **Guild Install** context with `bot` and `applications.commands` scopes.
2. Grant **Manage Roles**, **Manage Channels**, **Manage Threads**, **Manage Messages**, **View Channels**, **Send Messages**, **Send Messages in Threads**, and **Read Message History**. Add Reactions is optional. Give the bot explicit access to `📋│mod-log` and `🎫│ticket-log` if those staff channels are private.
3. Put its role below `👑 The Dev` but above every role it must grant, including `🛡️ Mod Squad`. This is powerful access. Keep the bot token private and do not grant Administrator.
4. With Node 20 or newer, run `npm ci` in this directory. Put `DISCORD_BOT_TOKEN=...` in an ignored, mode-600 `.env.local` file for local testing. Run `npm run start:local`. On a cloud host, set the token as a secret environment variable and use the included `Dockerfile`.
5. For local GitHub issue creation, sign in to `gh` as a repository account with Issues write access. On a cloud host, set `GITHUB_ISSUES_TOKEN` to a fine-grained token for only `the-geeky-couldron/waypoint` with **Issues: write**. The token stays on the host, never in Discord or the repository.

The bot persists XP, daily totals, and GitHub issue links in `data/state.json`, written atomically and ignored by Git. Back up that directory before moving hosts. Run only one A.T.L.A.S. process for the same server.

For a container deployment, mount a persistent disk at `/data`. The Oracle systemd service runs Node directly as UID 1000 with no Linux capabilities, a read-only root filesystem, and resource limits; `/data` must already be writable by UID 1000. Other deployments may use the image entrypoint, which fixes volume ownership before switching to the unprivileged `node` user. Set `WAYPOINT_DATA_DIR=/data` (already set in the image). Keep exactly one running replica, and include both `DISCORD_BOT_TOKEN` and `GITHUB_ISSUES_TOKEN` in the host's secret variables. Never upload `.env.local` to the host or repository.

For a zero-subscription VM option, follow the [Oracle Always Free deployment guide](deploy/oracle/README.md). It installs the container as a restartable system service and stores the state on the VM's persistent boot volume. An Oracle Cloud account and an Always Free VM in the account's home region are required.

## Waypoint cutover

A.T.L.A.S. runs on the Oracle Always Free VM as a system service. Welcomer, Carl-bot, Arcane, Ticket Tool, and Zapier were removed from the Discord server after A.T.L.A.S. was tested; the old Zap was turned off. A.T.L.A.S. is the only installed bot or app. A separate `Waypoint GitHub Announcements` webhook remains for repository announcements.

The 17 Arcane leaderboard totals were migrated to persistent VM state. The 37 existing human members were given the permissionless `🌍 Certified Explorer` base role; future members receive it on join. The pinned A.T.L.A.S. button replaced the old emoji posts, and adding and removing a lane role was verified live. `/rank` and the daily `/leaderboard` were verified after migration. The owner-only `/welcome-preview` displayed a personalized card; a new-member join has not yet been observed since cutover.

An A.T.L.A.S. bug ticket created a correctly titled public GitHub issue with the submitted description, and general help stays private. The open `ticket-0005` was adopted by A.T.L.A.S. as private general help without publishing its old report. Existing welcome messages, member conversations, and ticket channels were preserved. The already approved Moderator request retained its original title, Approved tag, and Mod Squad assignment.

For future checks, verify a real new join, a new role request decision by the owner, and the next midnight IST leaderboard post. The bot's unit tests cover the issue mapping, role request rules, level thresholds, and welcome card content.

Run `npm test` for the points, role, and issue mapping rules. Syntax-check with `node --check src/index.js`. Startup checks the forum tags and registers its slash commands for Waypoint. The bot scans active and archived role-request posts when a new post is made, so the one-pending rule survives restarts.
