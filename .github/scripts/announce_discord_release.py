#!/usr/bin/env python3
"""Post a published Waypoint release to the configured Discord announcements channel."""

from __future__ import annotations

import json
import os
import re
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit, urlunsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener


REPOSITORY = "the-geeky-couldron/waypoint"
TAG_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}\Z")
WEBHOOK_PATH_PATTERN = re.compile(r"/api/webhooks/[0-9]+/[A-Za-z0-9._-]+\Z")
RELEASE_PATH_PREFIX = f"/{REPOSITORY}/releases/tag/"


class NoRedirectHandler(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Required release announcement setting is missing: {name}")
    return value


def validate_release(tag: str, release_url: str) -> None:
    if not TAG_PATTERN.fullmatch(tag):
        raise ValueError("The release tag contains unsupported characters.")
    parsed = urlsplit(release_url)
    expected_path = f"{RELEASE_PATH_PREFIX}{tag}"
    if (
        parsed.scheme != "https"
        or parsed.netloc != "github.com"
        or parsed.path != expected_path
        or parsed.query
        or parsed.fragment
    ):
        raise ValueError("The release link is outside the Waypoint GitHub release page.")


def validate_webhook(value: str, setting: str) -> str:
    parsed = urlsplit(value)
    if (
        parsed.scheme != "https"
        or parsed.netloc != "discord.com"
        or not WEBHOOK_PATH_PATTERN.fullmatch(parsed.path)
        or parsed.query
        or parsed.fragment
    ):
        raise ValueError(f"{setting} must be an HTTPS Discord channel webhook URL.")
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "wait=true", ""))


def release_payload(tag: str, release_url: str, notes: str) -> bytes:
    # Keep the ping explicit and allow only @everyone. Release-note text stays in
    # the embed and has other mention syntax neutralized.
    safe_notes = notes.strip().replace("@", "@\u200b")
    if not safe_notes:
        safe_notes = "The latest Waypoint release is ready. Open GitHub for the full notes and APK."
    if len(safe_notes) > 1500:
        safe_notes = safe_notes[:1499].rstrip() + "…"

    payload = {
        "content": "@everyone",
        "allowed_mentions": {"parse": ["everyone"]},
        "embeds": [
            {
                "title": f"Waypoint {tag} is live",
                "url": release_url,
                "description": safe_notes,
                "color": 0x5B9E8B,
                "fields": [
                    {
                        "name": "Release notes and APK",
                        "value": f"[Open Waypoint {tag} on GitHub]({release_url})",
                        "inline": False,
                    }
                ],
                "footer": {"text": "Waypoint · Map your world"},
            }
        ],
    }
    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def post(opener, label: str, webhook_url: str, payload: bytes) -> None:
    request = Request(
        webhook_url,
        data=payload,
        headers={"Content-Type": "application/json", "User-Agent": "Waypoint-Release-Announcer"},
        method="POST",
    )
    try:
        with opener.open(request, timeout=20) as response:
            if response.status not in (200, 204):
                raise RuntimeError(f"Discord returned HTTP {response.status} for {label}.")
            response.read(64 * 1024)
    except HTTPError as error:
        raise RuntimeError(f"Discord returned HTTP {error.code} for {label}.") from None
    except (URLError, TimeoutError, OSError):
        raise RuntimeError(f"Discord could not be reached for {label}.") from None


def main() -> int:
    try:
        tag = required("WAYPOINT_RELEASE_TAG")
        release_url = required("WAYPOINT_RELEASE_URL")
        validate_release(tag, release_url)
        payload = release_payload(tag, release_url, os.environ.get("WAYPOINT_RELEASE_NOTES", ""))
        announcements_webhook = validate_webhook(
            required("DISCORD_ANNOUNCEMENTS_WEBHOOK_URL"), "DISCORD_ANNOUNCEMENTS_WEBHOOK_URL"
        )
        opener = build_opener(NoRedirectHandler())
        try:
            post(opener, "announcements", announcements_webhook, payload)
            print(f"Posted the {tag} announcement to Discord #announcements.")
            return 0
        except RuntimeError as error:
            print(str(error), file=sys.stderr)
            return 1
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
