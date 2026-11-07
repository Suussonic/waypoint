#!/usr/bin/env python3
"""Keep Android, release notes, README and changelog on the same release."""
from pathlib import Path
import os
import re

values = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
name = values['VERSION_NAME']
match = re.fullmatch(r'(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})', name)
assert match, 'Use a stable major.minor.patch version.'
major, minor, patch = map(int, match.group(1, 2, 3))
code = int(values['VERSION_CODE'])
assert code == major * 1_000_000 + minor * 1_000 + patch, 'VERSION_CODE does not match VERSION_NAME.'
notes = Path('docs/releases/current.md').read_bytes()
assert notes == Path('app/src/main/assets/release-notes.md').read_bytes(), 'Bundled and published release notes differ.'
assert notes.startswith(f'# Waypoint {name}\n'.encode()), 'Release notes must start with the current VERSION_NAME.'
assert notes == (Path('docs/releases') / f'{name}.md').read_bytes(), 'Versioned release page differs from current release notes.'
assert notes == (Path('app/src/main/assets') / f'release-notes-{name}.md').read_bytes(), 'Archived in-app release notes differ from current release notes.'

release_versions = [
    (tuple(map(int, path.stem.split('.'))), path)
    for path in Path('docs/releases').glob('*.md')
    if re.fullmatch(r'\d+\.\d+\.\d+', path.stem)
]
previous = max((entry for entry in release_versions if entry[0] < (major, minor, patch)), default=None)
if previous is not None:
    body = notes.split(b'\n', 1)[1].strip()
    previous_body = previous[1].read_bytes().split(b'\n', 1)[1].strip()
    assert body and body != previous_body, 'Write new release notes; the previous release body was reused.'

changelog = Path('CHANGELOG.md').read_text()
latest_change = re.search(r'^## ([0-9]+\.[0-9]+\.[0-9]+)\s*$', changelog, re.MULTILINE)
assert latest_change and latest_change.group(1) == name, 'The newest changelog section must match VERSION_NAME.'
latest_body = changelog[latest_change.end():].split('\n## ', 1)[0]
assert re.search(r'^- \S', latest_body, re.MULTILINE), 'The newest changelog section needs release-specific changes.'

readme = Path('README.md').read_text()
readme_heading = re.findall(r"^## What['’]s new in ([0-9]+\.[0-9]+\.[0-9]+)\s*$", readme, re.MULTILINE)
assert readme_heading == [name], 'README latest-release heading must match VERSION_NAME.'
readme_source = re.findall(r'This source tree is \*\*Waypoint ([0-9]+\.[0-9]+\.[0-9]+)\*\*', readme)
assert readme_source == [name], 'README source-tree version must match VERSION_NAME.'
assert not re.search(r'(?<![\w.])@[A-Za-z0-9][\w-]*|\[u/[A-Za-z0-9_-]+', readme), 'Keep user credits in release notes, not README.'

history = Path('app/src/main/java/com/waypoint/app/updates/ReleaseNotesHistory.kt').read_text()
history_entries = [
    (int(entry_code.replace('_', '')), entry_name)
    for entry_code, entry_name in re.findall(
        r'BundledReleaseNotes\(\s*([0-9_]+)\s*,\s*"release-notes-([0-9]+\.[0-9]+\.[0-9]+)\.md"\s*\)',
        history,
    )
]
assert history_entries and history_entries[-1] == (code, name), 'Add the current release to ReleaseNotesHistory.kt.'
assert [entry[0] for entry in history_entries] == sorted(set(entry[0] for entry in history_entries)), 'Release notes history must stay in unique version-code order.'
if os.environ.get('GITHUB_REF_TYPE') == 'tag':
    assert os.environ['GITHUB_REF_NAME'] == 'v' + name, 'Release tag does not match version.properties.'
print(f'Version {name} ({code}) verified')
