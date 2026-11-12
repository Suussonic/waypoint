"""Regression tests for branch-specific Actions release gates."""

import unittest
from pathlib import Path

from ci_summary import COMMON_GATES, build_summary, required_gates

ROOT = Path(__file__).resolve().parents[1]


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

    def test_published_release_has_one_discord_announcement_path(self):
        publisher = (ROOT / ".github/workflows/release.yml").read_text()
        announcer = (ROOT / ".github/workflows/discord-release-announcement.yml").read_text()
        self.assertNotIn("Announce the release in Discord", publisher)
        self.assertIn("types: [published]", announcer)
        self.assertIn(".github/scripts/announce_discord_release.py", announcer)


if __name__ == "__main__":
    unittest.main()
