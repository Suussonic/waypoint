# Contributing to Waypoint

Keep real roads, location confidence and guidance readable when changing a game world. Include a focused description, relevant tests and screenshots for visible changes.

## Keep public examples neutral

- Never commit personal GPS traces, search histories, home/work addresses, device identifiers, private account details or unredacted screenshots.
- Use fictional names and synthetic coordinates for regression fixtures. Use fixed public demonstration landmarks when a live map or service is essential.
- Remove status bars, notification content and image metadata from published app captures. Check the visible map and labels too; stripping metadata alone is insufficient.
- Release notes describe the behavior changed, without reproducing a user's place queries, routes or troubleshooting details.
- Keep private QA inputs and captures under ignored local directories. Do not attach them to public issues, workflow artifacts or releases.
- Keep signing keys and tokens out of the repository. Use encrypted repository secrets for release signing.

## Verification

Run the relevant Android unit tests and lint. Validate cartography after map-style edits with `npm ci --prefix tools/style-validation` and `node scripts/validate_styles.cjs`. For changes to editable onboarding motion, run `npm ci --prefix tools/waypoint-motion` and `npm run lint --prefix tools/waypoint-motion`. CI also runs focused Android emulator tests for package security and quest storage. See [the release guide](docs/releasing.md) for the full check tree and signed builds, and [validation notes](docs/validation.md) for the distinction between controlled tests and real-road evidence.

README artwork can be reproduced using [the promotional art generator](scripts/create_readme_promo.py) and local sanitised demonstration captures. Keep source captures, render outputs, logs, APKs and reference imagery out of Git. Run `python3 scripts/check_repo_hygiene.py` before committing; CI runs the same file, media and basic PII/secret-pattern checks. Review examples manually as well: automated checks cannot recognize every personal place or identifier.
