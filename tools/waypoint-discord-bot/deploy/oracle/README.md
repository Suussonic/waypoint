# Oracle Always Free deployment

Use one **Always Free** Ubuntu VM in the tenancy's **home region**. Prefer `VM.Standard.A1.Flex` with 1 OCPU and 6 GB memory. Its default 50 GB boot volume is within the Always Free storage allowance. If A1 capacity is unavailable, an Always Free `VM.Standard.E2.1.Micro` can run the bot, but its 1 GB memory makes building the image on the VM less comfortable. Check the **Always Free eligible** label on the shape, image, and boot volume before creation. Do not create a load balancer or paid networking service. A.T.L.A.S. needs outbound HTTPS and Discord Gateway access, but no inbound web port.

Oracle may reclaim idle Always Free VMs. Keep a backup of `/var/lib/atlas/state.json` and verify the bot after any host interruption. The service restarts after a normal VM reboot or process failure.

## Install on the VM

1. Create the VM in the Oracle console with Ubuntu, a public IP, and SSH access. Keep only SSH open in its ingress rules; the bot exposes no HTTP service.
2. SSH to the VM and install Git: `sudo apt-get update && sudo apt-get install -y git`.
3. Clone the bot branch: `sudo git clone --depth 1 --branch codex/atlas-discord-bot https://github.com/the-geeky-couldron/waypoint.git /opt/atlas-source`.
4. Install the container and service: `sudo /opt/atlas-source/tools/waypoint-discord-bot/deploy/oracle/install.sh`.
5. Copy the local `tools/waypoint-discord-bot/data/state.json` to `/var/lib/atlas/state.json` on the VM **before first start**. This carries the existing XP, ticket links, and panel IDs, so the VM does not create duplicate panels. Make the file readable only by root until the container entrypoint assigns ownership to its `node` user.
6. Put exactly two environment variables in `/etc/atlas/atlas.env`: `DISCORD_BOT_TOKEN` and `GITHUB_ISSUES_TOKEN`. The latter should be a fine-grained GitHub token limited to `the-geeky-couldron/waypoint` with **Issues: write**. Create the file as root with mode `0600`. Do not put secrets in VM metadata, shell history, the Git checkout, or a public issue.
7. Start and check: `sudo systemctl enable --now atlas.service`, `sudo systemctl status atlas.service`, and `sudo journalctl -u atlas.service -n 50 --no-pager`. The logs should say that A.T.L.A.S. logged in and registered nine guild commands. Never paste token values into logs or support messages.

The same `install.sh` can be rerun after `sudo git -C /opt/atlas-source pull --ff-only` to build the latest image and restart the service. Keep the ignored local `.env.local` and the cloud `/etc/atlas/atlas.env` separate. Run only one live A.T.L.A.S. process: stop the local test process after the VM succeeds.

Back up state before any host change: `sudo cp /var/lib/atlas/state.json /var/lib/atlas/state.json.backup` and then copy that backup to a private location outside the VM. The state contains XP totals, daily leaderboard history, and ticket-to-issue links. GitHub issue creation also searches its ticket marker to avoid duplicates if local state is restored from an older backup.
