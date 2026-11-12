#!/usr/bin/env python3
"""Draw the Android workflow's route map and enforce event-specific gates."""

import json
import os
from pathlib import Path


STAGES = (
    ("Trailhead", (("repository", "Repository integrity"),)),
    ("North gates", (("workflow_safety", "Workflow safety"), ("dependency_review", "Dependency review"), ("route_model", "CI gate tests"))),
    ("Cartography", (("release_page", "Release page"), ("styles", "Map styles"), ("assets", "Map assets"), ("motion", "Motion source"))),
    ("Street grid", (("android", "Debug APK"), ("android_unit", "JVM tests"), ("android_lint", "Android lint"))),
    ("Release loop", (("release_candidate", "Release preflight"), ("device_smoke", "Navigation emulator"))),
    ("Signing vault", (("sign", "Signed APK"), ("attest", "APK attestation"))),
)

ICONS = {"success": "●", "failure": "✕", "cancelled": "■", "skipped": "↷"}
COMMON_GATES = {
    "repository", "workflow_safety", "route_model", "release_page", "styles", "assets", "motion",
    "android", "android_unit", "android_lint", "release_candidate", "device_smoke",
}


def required_gates(event: str, ref: str, base_ref: str = "") -> tuple[str, set[str]]:
    if event == "push" and ref == "refs/heads/main":
        return "Main release route", COMMON_GATES | {"sign", "attest"}
    if event == "push" and ref == "refs/heads/dev":
        return "Dev route", COMMON_GATES
    if event == "pull_request" and base_ref in {"main", "dev"}:
        return f"Pull request → {base_ref}", COMMON_GATES | {"dependency_review"}
    raise ValueError(f"Unsupported CI event/ref: {event} / {ref} (base: {base_ref or 'none'})")


def build_summary(needs: dict, event: str, ref: str, base_ref: str = "") -> tuple[str, bool, list[str]]:
    mode, required = required_gates(event, ref, base_ref)
    results = {
        job: needs.get(job, {}).get("result", "missing")
        for _, entries in STAGES
        for job, _ in entries
    }
    failed = sorted(job for job in required if results.get(job) != "success")
    state = "CLEARED" if not failed else "HOLD"

    lines = [
        "# Waypoint · Release route map",
        "",
        f"**{mode} · {state}**",
        "",
        "```text",
        "                              ┌─ workflow safety ─ CI gate tests ───┐",
        "                              │                                     │",
        "TRAILHEAD ─┬─ release page ──┼─ debug APK ────────┬─ release loop ─┼─ signing vault ─ provenance",
        "           ├─ map styles ────┤                     ├─ field station┘                    │",
        "           ├─ map assets ────┤                     └─ JVM tests ─ lint ──────────────────┘",
        "           └─ motion source ─┘",
        "```",
        "",
        "## Gate ledger",
        "",
    ]
    for stage, entries in STAGES:
        lines.append(f"### {stage}")
        for job, title in entries:
            result = results[job]
            icon = ICONS.get(result, "?")
            required_mark = "required" if job in required else "event-only / skipped"
            lines.append(f"- {icon} **{title}** — `{result}` · {required_mark}")
        lines.append("")
    if failed:
        lines.extend(["**Route blocked.** Required gates missing or unsuccessful: " + ", ".join(f"`{job}`" for job in failed) + ".", ""])
    else:
        lines.extend(["All required gates completed successfully. The main release route also carries APK build provenance.", ""])
    return "\n".join(lines), not failed, failed


def main() -> None:
    needs = json.loads(os.environ["CI_NEEDS"])
    summary, passed, failed = build_summary(
        needs,
        os.environ["GITHUB_EVENT_NAME"],
        os.environ["GITHUB_REF"],
        os.environ.get("GITHUB_BASE_REF", ""),
    )
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as output:
        output.write(summary)
    print(summary)
    if not passed:
        raise SystemExit("Required CI gates failed or did not run: " + ", ".join(failed))


if __name__ == "__main__":
    main()
