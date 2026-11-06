#!/usr/bin/env python3
"""Keep Android's version code and the stable GitHub release tag in agreement."""
from pathlib import Path
import os
import re

values = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
name = values['VERSION_NAME']
match = re.fullmatch(r'(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})', name)
assert match, 'Use a stable major.minor.patch version.'
major, minor, patch = map(int, match.group(1, 2, 3))
assert int(values['VERSION_CODE']) == major * 1_000_000 + minor * 1_000 + patch, 'VERSION_CODE does not match VERSION_NAME.'
notes = Path('docs/releases/current.md').read_bytes()
assert notes == Path('app/src/main/assets/release-notes.md').read_bytes(), 'Bundled and published release notes differ.'
assert f'# Waypoint {name}'.encode() in notes, 'Release notes do not match VERSION_NAME.'
assert notes == (Path('docs/releases') / f'{name}.md').read_bytes(), 'Versioned release page differs from current release notes.'
assert notes == (Path('app/src/main/assets') / f'release-notes-{name}.md').read_bytes(), 'Archived in-app release notes differ from current release notes.'
if os.environ.get('GITHUB_REF_TYPE') == 'tag':
    assert os.environ['GITHUB_REF_NAME'] == 'v' + name, 'Release tag does not match version.properties.'
print(f'Version {name} ({values["VERSION_CODE"]}) verified')
