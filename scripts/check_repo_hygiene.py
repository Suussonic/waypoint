#!/usr/bin/env python3
"""Keep private QA material and generated duplicates out of the committed tree."""

from pathlib import Path
import re
import subprocess


ROOT = Path(__file__).resolve().parents[1]
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
untracked = subprocess.check_output(
    ["git", "ls-files", "--others", "--exclude-standard", "-z"], cwd=ROOT
).decode().split("\0")
candidates = sorted(set(filter(None, tracked + untracked)))
blocked_roots = (
    ".impeccable/", ".local/", ".tooling/", "dist/", "output/", "tmp/",
    "tools/waypoint-motion/public/", "tools/waypoint-motion/source-blips/",
    "tools/waypoint-motion/source-poi-sheets/", "docs/images/captures/",
)
blocked_suffixes = {".apk", ".aab", ".log", ".pyc", ".mov", ".webm", ".avi", ".pdf"}
blocked_suffixes |= {".keystore", ".jks", ".p12", ".pem"}
readme_media = {
    "docs/images/waypoint-icon.png",
    "docs/images/promo/download.jpg",
    "docs/images/promo/hero.jpg",
    "docs/images/promo/hero-mobile.jpg",
    "docs/images/promo/android-auto.jpg",
    "docs/images/promo/android-auto-mobile.jpg",
    "docs/images/promo/identity.jpg",
    *(f"docs/images/promo/worlds-{index}{suffix}.jpg"
      for index in range(1, 4) for suffix in ("", "-mobile")),
}
errors = []
for name in candidates:
    # A locally deleted file will disappear from the next commit; CI sees the committed tree.
    if not (ROOT / name).exists():
        continue
    suffix = Path(name).suffix.lower()
    if name.startswith(blocked_roots) or suffix in blocked_suffixes or name == "local.properties" or Path(name).name.startswith(".env"):
        errors.append(f"Unexpected tracked artifact: {name}")
    elif suffix == ".mp4" and not name.startswith("app/src/main/res/raw/"):
        errors.append(f"Video outside Android runtime resources: {name}")
    elif name.startswith("docs/images/") and suffix in {".jpg", ".jpeg", ".png", ".webp", ".gif"} and name not in readme_media:
        errors.append(f"Unused or source screenshot in public docs: {name}")

readme = (ROOT / "README.md").read_text()
for image in re.findall(r"(?:src|srcset)=[\"'](docs/images/[^\"']+)[\"']", readme):
    if not (ROOT / image).is_file():
        errors.append(f"README image is missing: {image}")

private_patterns = (
    re.compile(r"/" + r"Users/[^/\s\"'<>]{2,}/"),
    re.compile(r"[A-Za-z0-9._%+-]+@(?:gmail|outlook|yahoo)\.com", re.I),
    re.compile(r"(?:sk-[A-Za-z0-9_-]{16,}|AIza[0-9A-Za-z_-]{20,}|AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----)"),
)
text_suffixes = {".md", ".kt", ".kts", ".py", ".ts", ".tsx", ".js", ".cjs",
                 ".json", ".jsonl", ".yml", ".yaml", ".xml", ".properties", ".txt", ".sh"}
for name in candidates:
    path = ROOT / name
    if not path.is_file() or path.suffix.lower() not in text_suffixes:
        continue
    content = path.read_text(errors="replace")
    if any(pattern.search(content) for pattern in private_patterns):
        errors.append(f"Review possible private information in: {name}")
    if name.startswith("app/src/androidTest/") and re.search(r"Coordinate\(\s*-?\d+\.\d{5,}", content):
        errors.append(f"Review precise test coordinate for device-derived location: {name}")

for image in readme_media:
    path = ROOT / image
    if path.is_file() and (b"Exif\0\0" in path.read_bytes() or b"eXIf" in path.read_bytes()):
        errors.append(f"Remove EXIF metadata from public README image: {image}")

raw_videos = (ROOT / "app/src/main/res/raw").glob("*.mp4")
app_sources = "\n".join(path.read_text() for path in (ROOT / "app/src/main/java").rglob("*.kt"))
for video in raw_videos:
    if f"R.raw.{video.stem}" not in app_sources:
        errors.append(f"Android video has no runtime reference: {video.name}")

resource_sources = app_sources + "\n" + "\n".join(
    path.read_text() for path in (ROOT / "app/src/main/res").rglob("*.xml")
)
for drawable in (ROOT / "app/src/main/res/drawable-nodpi").iterdir():
    if drawable.is_file() and f"R.drawable.{drawable.stem}" not in resource_sources and f"@drawable/{drawable.stem}" not in resource_sources:
        errors.append(f"Android image has no resource reference: {drawable.name}")

if errors:
    raise SystemExit("\n".join(errors))
print("Repository hygiene OK: no tracked QA artifacts or unused video files")
