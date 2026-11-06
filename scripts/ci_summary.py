#!/usr/bin/env python3
"""Render required GitHub Actions jobs as one readable, blocking check tree."""

import json
import os
from pathlib import Path


JOBS = (
    ("Source", (("repository", "Repository integrity"), ("workflow_safety", "Workflow safety"), ("dependency_review", "Dependency changes"))),
    ("Assets", (("styles", "Map styles"), ("motion", "Motion source"))),
    ("Android", (("android", "Build, unit tests and lint"), ("device_smoke", "Emulator smoke tests"))),
    ("Release", (("sign", "Sign release APK"),)),
)
ICONS = {"success": "✅", "failure": "❌", "cancelled": "🛑", "skipped": "↷"}


def main():
    needs = json.loads(os.environ["CI_NEEDS"])
    ref = os.environ["GITHUB_REF"]
    event = os.environ["GITHUB_EVENT_NAME"]
    if event == "push" and ref == "refs/heads/main":
        required = {"workflow_safety", "repository", "styles", "motion", "android", "device_smoke", "sign"}
        mode = "Main branch"
    elif event == "pull_request":
        required = {"workflow_safety", "dependency_review", "repository", "styles", "motion", "android", "device_smoke"}
        mode = "Pull request"
    else:
        raise SystemExit(f"Unsupported CI event/ref: {event} / {ref}")

    results = {job: needs.get(job, {}).get("result", "missing") for _, entries in JOBS for job, _ in entries}
    passed = all(results[job] == "success" for job in required)
    lines = ["# Waypoint CI", "", f"**{mode} · {'Passed' if passed else 'Failed'}**", "", "```text", "Waypoint"]
    for group_index, (group, entries) in enumerate(JOBS):
        group_last = group_index == len(JOBS) - 1
        lines.append(("└── " if group_last else "├── ") + group)
        indent = "    " if group_last else "│   "
        for entry_index, (job, title) in enumerate(entries):
            status = results[job]
            icon = ICONS.get(status, "❓")
            suffix = "" if job in required else " (not required for this event)"
            branch = "└── " if entry_index == len(entries) - 1 else "├── "
            lines.append(f"{indent}{branch}{icon} {title}: {status}{suffix}")
    lines.extend(["```", "", "Required checks must pass. A skipped required check also fails this gate; event-only checks are marked above."])
    summary = "\n".join(lines) + "\n"
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as output:
        output.write(summary)
    print(summary)
    if not passed:
        raise SystemExit("One or more required checks failed or did not run")


if __name__ == "__main__":
    main()
