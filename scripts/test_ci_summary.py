"""Regression tests for branch-specific Actions release gates."""

import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / ".github" / "scripts"))

import announce_discord_release
from ci_summary import COMMON_GATES, build_summary, required_gates


class CiSummaryTests(unittest.TestCase):
    @staticmethod
    def successful_needs(extra=()):
        return {job: {"result": "success"} for job in COMMON_GATES | set(extra)}

    def test_dev_requires_the_full_shared_route(self):
        mode, required = required_gates("push", "refs/heads/dev")
        self.assertEqual("Dev route", mode)
        self.assertEqual(COMMON_GATES, required)

    def test_main_requires_signing_and_provenance(self):
        _, required = required_gates("push", "refs/heads/main")
        self.assertTrue({"sign", "attest"}.issubset(required))

    def test_pull_request_requires_dependency_review(self):
        _, required = required_gates("pull_request", "refs/pull/42/merge", "dev")
        self.assertIn("dependency_review", required)
        self.assertNotIn("sign", required)
        self.assertNotIn("attest", required)

    def test_skipped_event_only_gates_do_not_block_dev(self):
        needs = self.successful_needs()
        needs.update({"sign": {"result": "skipped"}, "attest": {"result": "skipped"},
                      "dependency_review": {"result": "skipped"}})
        summary, passed, failed = build_summary(needs, "push", "refs/heads/dev")
        self.assertTrue(passed)
        self.assertEqual([], failed)
        self.assertIn("Dev route · CLEARED", summary)
        self.assertIn("APK attestation** — `skipped` · event-only / skipped", summary)

    def test_missing_or_skipped_required_gate_blocks_route(self):
        needs = self.successful_needs({"dependency_review"})
        needs["assets"] = {"result": "skipped"}
        needs["dependency_review"] = {"result": "skipped"}
        _, passed, failed = build_summary(needs, "pull_request", "refs/pull/42/merge", "main")
        self.assertFalse(passed)
        self.assertIn("assets", failed)
        self.assertIn("dependency_review", failed)

    def test_unknown_event_is_rejected(self):
        with self.assertRaises(ValueError):
            required_gates("workflow_dispatch", "refs/heads/dev")

    def test_release_publisher_announces_only_when_it_creates_a_release(self):
        publisher = (ROOT / ".github/workflows/release.yml").read_text()
        self.assertIn("id: publish_release", publisher)
        self.assertIn('echo "created=true" >> "$GITHUB_OUTPUT"', publisher)
        self.assertIn('echo "created=false" >> "$GITHUB_OUTPUT"', publisher)
        self.assertIn("if: steps.publish_release.outputs.created == 'true'", publisher)
        self.assertIn(".github/scripts/announce_discord_release.py", publisher)

    def test_published_release_announcer_has_event_and_recovery_paths(self):
        announcer = (ROOT / ".github/workflows/discord-release-announcement.yml").read_text()
        self.assertIn("types: [published]", announcer)
        self.assertIn("workflow_dispatch:", announcer)
        self.assertIn("inputs.tag", announcer)
        self.assertIn("github.ref == 'refs/heads/main'", announcer)
        self.assertIn(".github/scripts/announce_discord_release.py", announcer)
        self.assertNotIn("DISCORD_RELEASES_WEBHOOK_URL", announcer)

    def test_release_announcer_only_needs_the_announcements_webhook(self):
        publisher = (ROOT / ".github/workflows/release.yml").read_text()
        self.assertNotIn("DISCORD_RELEASES_WEBHOOK_URL", publisher)
        environment = {
            "WAYPOINT_RELEASE_TAG": "v2.6.5",
            "WAYPOINT_RELEASE_URL": "https://github.com/the-geeky-couldron/waypoint/releases/tag/v2.6.5",
            "WAYPOINT_RELEASE_NOTES": "Release notes",
            "DISCORD_ANNOUNCEMENTS_WEBHOOK_URL": "https://discord.com/api/webhooks/123/abc",
        }
        with (
            patch.dict(os.environ, environment, clear=True),
            patch.object(announce_discord_release, "build_opener") as build_opener,
            patch.object(announce_discord_release, "post") as post,
        ):
            opener = object()
            build_opener.return_value = opener
            self.assertEqual(0, announce_discord_release.main())

        post.assert_called_once()
        self.assertEqual(opener, post.call_args.args[0])
        self.assertEqual("announcements", post.call_args.args[1])
        self.assertEqual(
            "https://discord.com/api/webhooks/123/abc?wait=true",
            post.call_args.args[2],
        )


if __name__ == "__main__":
    unittest.main()
