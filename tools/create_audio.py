"""Render the original offline soundtrack and game cues into Android resources."""
from pathlib import Path
import subprocess
import wave

import numpy as np

RATE = 24_000
OUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def note(pitch, duration, color=0):
    t = np.arange(int(duration * RATE)) / RATE
    f = 440 * 2 ** ((pitch - 69) / 12)
    envelope = np.minimum(1, t / 0.012) * np.minimum(1, (duration - t) / 0.06)
    fundamental = np.sin(2 * np.pi * f * t)
    overtone = 0.24 * np.sin(4 * np.pi * f * t + color * np.sin(17 * t))
    return (fundamental + overtone) * envelope * np.exp(-t * (5 - color))


def write(name, signal, compressed=False):
    path = OUT / (name + ".wav")
    with wave.open(str(path), "wb") as file:
        file.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        file.writeframes(np.int16(np.clip(signal, -0.95, 0.95) * 32767).tobytes())
    if compressed:
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(path),
             "-c:a", "libvorbis", "-q:a", "4", str(path.with_suffix(".ogg"))],
            check=True,
        )
        path.unlink()


def add(output, sound, start, gain):
    indices = (np.arange(len(sound)) + round(start * RATE)) % len(output)
    np.add.at(output, indices, sound * gain)


def music(name, bpm, shift, themes):
    beat = 60 / bpm
    bars = [(48, 52, 55), (45, 48, 52), (41, 45, 48), (43, 47, 50),
            (48, 52, 55), (40, 43, 47), (41, 45, 48), (43, 47, 50)]
    output = np.zeros(round(len(bars) * 4 * beat * RATE))
    for bar, chord in enumerate(bars):
        start = bar * 4 * beat
        for i in range(8):
            position = start + i * beat / 2
            pitch = chord[(0, 2, 1, 2, 0, 1, 2, 1)[i]] + 12 + shift
            add(output, note(pitch, beat * 0.6, 1), position, 0.11)
        for i in range(4):
            add(output, note(chord[i % 3] + shift, beat * 0.74, 0), start + i * beat, 0.09)
        for index, pitch in enumerate(themes[bar % len(themes)]):
            if pitch:
                add(output, note(pitch + shift, beat * 0.67, 2), start + index * beat, 0.14)
    write(name, output, True)


def cue(name, sequence, spacing=0.085, duration=0.19):
    output = np.zeros(int(((len(sequence) - 1) * spacing + duration + 0.03) * RATE))
    for index, pitch in enumerate(sequence):
        add(output, note(pitch, duration, 1), index * spacing, 0.28)
    write(name, output)


OUT.mkdir(parents=True, exist_ok=True)
music("music_menu", 105, 0, [(72, 76, 79, 0), (76, 72, 69, 0), (77, 76, 72, 69), (74, 71, 67, 0)])
music("music_game", 126, 2, [(79, 76, 72, 76), (76, 72, 0, 69), (72, 77, 76, 72), (74, 71, 74, 0)])
cue("catch_fruit", [76, 83], 0.041, 0.12)
cue("catch_gold", [72, 79, 84, 91], 0.058)
cue("button_tap", [71], duration=0.075)
cue("heart_loss", [59, 51], 0.065)
cue("record_score", [72, 76, 79, 84], 0.11, 0.31)
cue("game_over", [69, 65, 62, 57], 0.14, 0.31)
cue("start_game", [64, 69, 73, 81], 0.081)
rng = np.random.default_rng(311)
t = np.arange(int(RATE * 0.43)) / RATE
noise = np.convolve(rng.normal(0, 0.6, len(t)), np.ones(8) / 8, mode="same")
blast = (noise + 0.65 * np.sin(2 * np.pi * (96 * t - 57 * t * t))) * np.exp(-t * 12)
write("bomb_hit", blast * np.minimum(1, t * 290) * 0.75)
