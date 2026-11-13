"""Tests for semantic release-page and offline-safe navigation contracts."""

from pathlib import Path
import tempfile
import unittest

from check_release_page import validate_document, validate_release_site


REPO = Path(__file__).resolve().parents[1]


class ReleasePageTests(unittest.TestCase):
    def test_current_release_site_is_complete(self):
        errors = validate_release_site(REPO / "docs")
        self.assertEqual([], errors)

    def test_document_rejects_missing_local_media_and_bad_heading_order(self):
        with tempfile.TemporaryDirectory() as temporary:
            docs = Path(temporary) / "docs"
            page_dir = docs / "releases"
            page_dir.mkdir(parents=True)
            page = page_dir / "test.html"
            page.write_text(
                "<!doctype html><html lang='en'><head><title>Test</title>"
                "<meta name='viewport' content='width=device-width'></head><body>"
                "<a href='#main-content'>Skip</a><main id='main-content'>"
                "<h1>Release</h1><h3>Skipped level</h3><img src='../missing.webp'>"
                "</main></body></html>",
                encoding="utf-8",
            )
            errors = validate_document(page, docs)
            self.assertTrue(any("skips a heading level" in error for error in errors))
            self.assertTrue(any("missing local target" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
