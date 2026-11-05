#!/usr/bin/env python3
"""Original, short three-note completion cues. No game audio or sampled recordings."""
import math
import re
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
worlds = re.findall(r'R.string.world_[a-z_]+_description,"([a-z_]+)"', (ROOT / 'app/src/main/java/com/waypoint/app/World.kt').read_text())
assert len(worlds) == len(set(worlds)) and worlds, "World IDs must be unique"
folder = ROOT / 'app/src/main/assets/quest-sounds'
folder.mkdir(exist_ok=True)
for index, world in enumerate(worlds):
    rate = 22050
    base = 220 * 2 ** ((index % 12) / 12)
    intervals = (0, 4, 7) if index % 2 else (0, 7, 12)
    samples = []
    for n in range(int(rate * .65)):
        t = n / rate
        value = 0
        for note, interval in enumerate(intervals):
            age = t - note * .13
            if 0 <= age < .34:
                envelope = min(1, age / .012) * (1 - age / .34) ** 2
                phase = 2 * math.pi * base * 2 ** (interval / 12) * age
                value += envelope * (math.sin(phase) + .12 * math.sin(2 * phase + index / 36))
        samples.append(struct.pack('<h', int(max(-1, min(1, value * .26)) * 32767)))
    with wave.open(str(folder / f'{world}.wav'), 'wb') as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(rate); out.writeframes(b''.join(samples))
print(f'Generated {len(worlds)} original completion cues.')
