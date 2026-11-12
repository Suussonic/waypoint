# A.T.L.A.S. for Waypoint

A.T.L.A.S. is Waypoint's single Discord community bot. It handles welcomes, a base member role, 38 self-serve world roles, lane roles, activity-based quest points, level roles, a daily leaderboard, private support tickets, GitHub issues for bug and feature tickets, and owner-only role request decisions.

The existing Moderator request was already granted to its author, tagged Approved, and closed manually. Old reactions are deliberately not replayed on bot startup.

## Commands and behavior

| Command | What it does |
| --- | --- |
| `/help` | Interactive buttons for roles, rank, and a private help ticket |
| `/roles` | Select lanes and any of the 38 world club roles |
| `/quests` | Show how Discord activity earns community points |
| `/rank [member]` | Show total points, today's points, level, and next threshold |
| `/leaderboard [period]` | Daily IST board by default, or all-time board |
| `/ticket type` | Structured ticket form; bug and feature forms create public GitHub issues |
| `/close` | Requester or staff closes an A.T.L.A.S. ticket |
| `/sync-issue` | Staff retries GitHub issue creation if it failed |
| `/xp-set member xp` | Server owner migrates or corrects prior Arcane XP |

Messages earn 20 points at most once a minute. Reacting to someone else's message earns 5 points at most once every five minutes and 20 times a day. A new bug, feature, or world idea forum post earns 35 points, capped at three per day. Private ticket messages do not earn points. The daily board resets at midnight Asia/Kolkata; a summary is posted in `⚔️│quests-and-xp`. All-time points continue accumulating. Level roles unlock at 5, 10, 20, and 35. This Discord score is separate from private quest XP in the Android app.

`🙏│role-requests` permits one pending post per member. A duplicate is marked Declined, closed, and linked to the first pending post. Only a ✅ or ❌ from the **server owner** on the first message counts. The bot removes those reactions from other members. Owner ✅ grants the role named by the exact post title to the **post author**, marks Approved, and closes the post; ❌ marks Declined and closes it. The allowlist is in `src/rules.js`. The bot refuses roles with Administrator permission or roles above its own role.

For bug and feature tickets, the form labels the title and description as public. Only those submitted fields, plus optional steps and device information, are copied to [the Waypoint GitHub repository](https://github.com/the-geeky-couldron/waypoint/issues). Private replies are never copied. General help tickets stay entirely in Discord. GitHub issue creation is idempotent by ticket ID and can be retried by staff.

## Installation

The [A.T.L.A.S. Discord application](https://discord.com/developers/applications/1570323716204658781/) already exists. Its avatar and banner are in `assets/`.

1. Enable **Server Members Intent** for join events. Leave Message Content and Presence intents off. Use only the **Guild Install** context with `bot` and `applications.commands` scopes.
2. Grant **Manage Roles**, **Manage Channels**, **Manage Threads**, **Manage Messages**, **View Channels**, **Send Messages**, **Send Messages in Threads**, and **Read Message History**. Add Reactions is optional. Give the bot explicit access to `📋│mod-log` and `🎫│ticket-log` if those staff channels are private.
3. Put its role below `👑 The Dev` but above every role it must grant, including `🛡️ Mod Squad`. This is powerful access. Keep the bot token private and do not grant Administrator.
4. With Node 20 or newer, run `npm ci` in this directory. Put `DISCORD_BOT_TOKEN=...` in an ignored, mode-600 `.env.local` file for local testing. Run `npm run start:local`. On a cloud host, set the token as a secret environment variable and use the included `Dockerfile`.
5. For local GitHub issue creation, sign in to `gh` as a repository account with Issues write access. On a cloud host, set `GITHUB_ISSUES_TOKEN` to a fine-grained token for only `the-geeky-couldron/waypoint` with **Issues: write**. The token stays on the host, never in Discord or the repository.

The bot persists XP, daily totals, and GitHub issue links in `data/state.json`, written atomically and ignored by Git. Back up that directory before moving hosts. Run only one A.T.L.A.S. process for the same server.

For a container deployment, mount a persistent disk at `/data`. The entrypoint fixes volume ownership and then runs Node as the unprivileged `node` user. Set `WAYPOINT_DATA_DIR=/data` (already set in the image). Keep exactly one running replica, and include both `DISCORD_BOT_TOKEN` and `GITHUB_ISSUES_TOKEN` in the host's secret variables. Never upload `.env.local` to the host or repository.

## Cutover checks

1. Verify a new member gets Certified Explorer and one A.T.L.A.S. welcome. Preserve OG Day-Ones for existing launch members; stop assigning it to new members.
2. Test `/roles`, `/rank`, `/quests`, daily `/leaderboard`, and all four level thresholds. Use `/xp-set` to migrate any Arcane totals worth preserving before turning Arcane off.
3. Test bug, feature, and general tickets. Bug and feature forms should create correctly titled GitHub issues; general help should not. Test `/close` and `/sync-issue`.
4. Test a role request with a non-owner ✅, owner ✅, owner ❌, and a duplicate pending post. Confirm that only owner decisions alter roles and statuses.
5. Once the replacement is running continuously and tested, disable or remove Welcomer, Carl-bot, Arcane, Ticket Tool, and any unused Zapier integration. Replace old Ticket Tool panel messages with A.T.L.A.S. `/ticket` instructions or panels first. Keep existing ticket channels and their history.

Run `npm test` for the points, role, and issue mapping rules. Syntax-check with `node --check src/index.js`. Startup checks the forum tags and registers its slash commands for Waypoint. The bot scans active and archived role-request posts when a new post is made, so the one-pending rule survives restarts.
