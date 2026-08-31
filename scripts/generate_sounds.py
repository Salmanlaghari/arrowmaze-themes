"""Generate small placeholder WAV files for sound effects (sine tones)."""
import math
import os
import struct
import wave

SAMPLE_RATE = 22050
OUT_DIR = os.path.join("app", "src", "main", "res", "raw")
os.makedirs(OUT_DIR, exist_ok=True)


def write_wav(path: str, samples: list[float]) -> None:
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        for s in samples:
            s = max(-1.0, min(1.0, s))
            w.writeframes(struct.pack("<h", int(s * 32767)))


def envelope(i: int, total: int, attack: float = 0.02, release: float = 0.1) -> float:
    t = i / SAMPLE_RATE
    dur = total / SAMPLE_RATE
    if t < attack:
        return t / attack
    if t > dur - release:
        return max(0.0, (dur - t) / release)
    return 1.0


def tone(freq: float, duration: float, amp: float = 0.6, attack: float = 0.02, release: float = 0.1) -> list[float]:
    total = int(duration * SAMPLE_RATE)
    return [amp * envelope(i, total, attack, release) * math.sin(2 * math.pi * freq * i / SAMPLE_RATE) for i in range(total)]


def mix(*parts: list[float]) -> list[float]:
    n = max(len(p) for p in parts)
    out = [0.0] * n
    for p in parts:
        for i, v in enumerate(p):
            out[i] += v
    return [max(-1.0, min(1.0, s / max(1, len(parts)))) for s in out]


correct = mix(
    tone(660, 0.08, amp=0.6),
    [0.0] * int(0.05 * SAMPLE_RATE) + tone(880, 0.12, amp=0.6),
)
wrong = tone(160, 0.22, amp=0.7, attack=0.005, release=0.05)
level_complete: list[float] = []
for f in (523.25, 659.25, 783.99, 1046.50):
    n = int(0.12 * SAMPLE_RATE)
    seg = [0.5 * envelope(i, n, 0.005, 0.05) * math.sin(2 * math.pi * f * i / SAMPLE_RATE) for i in range(n)]
    level_complete.extend(seg)
    level_complete.extend([0.0] * int(0.02 * SAMPLE_RATE))
button_tap = tone(1200, 0.04, amp=0.4, attack=0.001, release=0.02)

for name, data in {
    "correct_move.wav": correct,
    "wrong_move.wav": wrong,
    "level_complete.wav": level_complete,
    "button_tap.wav": button_tap,
}.items():
    path = os.path.join(OUT_DIR, name)
    write_wav(path, data)
    print(f"wrote {path} ({len(data)} samples, {len(data)/SAMPLE_RATE:.2f}s)")

print("done")
