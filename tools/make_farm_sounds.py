#!/usr/bin/env python3
"""Synthesizes the Farming and Experiments sound bank (sounds/farm_*, pest_*, exp_*.ogg), the matching sounds.json entries and the
envelopes the Sounds tab draws. Needs numpy and ffmpeg (libvorbis). Run from the repo root: python3 tools/make_farm_sounds.py"""
import os, json, subprocess, tempfile, wave
import numpy as np
SR = 44100
RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fancydrops')
rng = np.random.default_rng(26)

def T(d): return np.arange(int(SR * d)) / SR
def env(t, a=0.005, r=0.2, p=2.0):
    d = t[-1] + 1 / SR
    return np.minimum(1, t / a) * np.clip((d - t) / r, 0, 1) ** p
def tone(f, d, a=0.004, r=None, harm=(1, .35, .12), glide=None):
    t = T(d); r = r or d * .8
    fr = f if glide is None else f + (glide - f) * (t / d)
    ph = 2 * np.pi * np.cumsum(fr) / SR
    s = sum(h * np.sin(ph * (i + 1)) for i, h in enumerate(harm))
    return s * env(t, a, r)
def pluck(f, d):
    t = T(d); s = np.sin(2 * np.pi * f * t) + .4 * np.sin(4 * np.pi * f * t) * np.exp(-t * 14) + .15 * np.sin(6 * np.pi * f * t) * np.exp(-t * 20)
    return s * np.exp(-t * (3.2 / d)) * np.minimum(1, t / .002)
def lowpass(x, k):
    y = np.zeros_like(x); a = 0
    for i, v in enumerate(x): a += k * (v - a); y[i] = a
    return y
def noise(d, k=.3): return lowpass(rng.standard_normal(int(SR * d)), k)
def at(buf, s, t0): i = int(t0 * SR); n = min(len(s), len(buf) - i); buf[i:i + n] += s[:n]
def mix(d): return np.zeros(int(SR * d))
def norm(x, peak=.8): return x / max(1e-6, np.abs(x).max()) * peak

N = lambda m: 440 * 2 ** ((m - 69) / 12)
OUT = {}

def farm_cut():                                      # a scythe swish and a clean snip
    b = mix(.5); t = T(.3)
    sw = noise(.3, .5) * np.sin(np.pi * np.clip(t / .3, 0, 1)) ** 1.5; at(b, sw * .7, 0)
    at(b, tone(N(88), .12, r=.1, harm=(1, .5, .2)) * .5, .17)
    at(b, noise(.08, .15) * env(T(.08), .001, .07) * .8, .17)
    return b
def farm_heave():                                    # soil gives way: a low pull, rustle, and a soft pop
    b = mix(.7); t = T(.5)
    at(b, tone(110, .5, a=.15, r=.3, glide=190, harm=(1, .3)) * .6, 0)
    at(b, noise(.5, .12) * env(t, .1, .35) * .9, 0)
    at(b, tone(N(76), .14, r=.12, glide=N(84)) * .55, .52)
    return b
def bloom(big):                                      # the plant opens: a rising pentatonic pluck run on a warm bed
    d = 2.3 if big else 1.2; b = mix(d)
    notes = [67, 71, 74, 79, 83, 86, 91] if big else [72, 76, 79, 84]
    for i, m in enumerate(notes): at(b, pluck(N(m), 1.0 if big else .7) * (.55 + .06 * i), i * (.1 if big else .09))
    t = T(d)
    pad = sum(np.sin(2 * np.pi * N(m) * t) for m in ([55, 62, 67] if big else [60, 64, 67])) * .12
    at(b, pad * env(t, .25, d * .6), 0)
    if big:
        at(b, tone(N(96), 1.2, a=.01, r=1.0, harm=(1, .2)) * .22, .7)
        at(b, noise(.9, .6) * env(T(.9), .3, .6) * .06, .5)
    return b
def farm_end():
    b = mix(.9)
    for i, m in enumerate([84, 79, 72]): at(b, pluck(N(m), .6) * .55, i * .13)
    return b
def pest_vacuum():                                   # the pest gets sucked out: a falling-then-rising whoosh
    d = .8; t = T(d); b = mix(d)
    fr = 900 - 700 * np.sin(np.pi * t / d) ** .7
    ph = 2 * np.pi * np.cumsum(fr) / SR
    b += (np.sin(ph) * .25 + noise(d, .35) * .8) * np.sin(np.pi * t / d) ** 1.4
    return b
def pest_gulp():                                     # a wet two-step blip
    b = mix(.4)
    at(b, tone(260, .1, a=.003, r=.09, glide=520, harm=(1, .3)) * .7, 0)
    at(b, tone(190, .14, a=.003, r=.12, glide=420, harm=(1, .3)) * .7, .1)
    at(b, noise(.2, .08) * env(T(.2), .002, .18) * .3, 0)
    return b
def pest_pop():
    b = mix(.55)
    at(b, tone(N(79), .09, a=.002, r=.08, glide=N(91), harm=(1, .4)) * .7, 0)
    for i, m in enumerate([84, 88, 91]): at(b, pluck(N(m), .4) * .5, .08 + i * .07)
    return b
def exp_flip():                                      # pages riffling: a burst of short papery ticks
    b = mix(.6)
    for i in range(9):
        t0 = i * .045 + rng.uniform(0, .01); n = noise(.035, .55) * env(T(.035), .001, .03, 1.5)
        at(b, n * (.8 - i * .04), t0)
    return b
def exp_open():                                      # the cover swings: a thump, a rune hum
    b = mix(1.0); t = T(1.0)
    at(b, tone(70, .25, r=.22, glide=48, harm=(1, .4)) * .8, 0)
    at(b, noise(.1, .3) * env(T(.1), .001, .09) * .6, 0)
    at(b, (np.sin(2 * np.pi * N(64) * t) + np.sin(2 * np.pi * N(71) * t) * .7) * env(t, .15, .6) * .25, .12)
    return b
def exp_reveal(big):                                 # arcane chord with a shimmer
    d = 2.6 if big else 1.4; b = mix(d); t = T(d)
    notes = [60, 67, 71, 76, 79, 83] if big else [64, 71, 76]
    for i, m in enumerate(notes): at(b, pluck(N(m), 1.3 if big else .9) * .5, i * .08)
    sh = sum(np.sin(2 * np.pi * N(m + 12) * t + i) for i, m in enumerate(notes)) * (1 + .5 * np.sin(2 * np.pi * 6 * t))
    at(b, sh * env(t, .3, d * .6) * (.06 if big else .04), 0)
    if big: at(b, tone(N(48), 1.4, a=.05, r=1.2, harm=(1, .3)) * .35, 0)
    return b
def exp_end():
    b = mix(1.0)
    for i, m in enumerate([88, 83, 76]): at(b, pluck(N(m), .7) * .45, i * .15)
    return b

SOUNDS = {'farm_cut': farm_cut(), 'farm_heave': farm_heave(), 'farm_bloom': bloom(False), 'farm_bloom_big': bloom(True), 'farm_end': farm_end(),
          'pest_vacuum': pest_vacuum(), 'pest_gulp': pest_gulp(), 'pest_pop': pest_pop(),
          'exp_flip': exp_flip(), 'exp_open': exp_open(), 'exp_reveal': exp_reveal(False), 'exp_reveal_epic': exp_reveal(True), 'exp_end': exp_end()}

sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
for name, x in SOUNDS.items():
    x = norm(x)
    x[-200:] *= np.linspace(1, 0, 200)
    with tempfile.TemporaryDirectory() as td:
        wp = os.path.join(td, 'a.wav')
        with wave.open(wp, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wp, '-c:a', 'libvorbis', '-q:a', '4', os.path.join(RES, 'sounds', name + '.ogg')], check=True)
    d = round(len(x) / SR, 2); n = int(d * 40) + 1; hop = SR // 40
    e = [float(np.sqrt(np.mean(x[i * hop:(i + 1) * hop] ** 2))) if i * hop < len(x) else 0 for i in range(n)]
    m = max(e) or 1
    ej[name] = {'d': d, 'e': [int(round(v / m * 99)) for v in e]}
    sj[name] = {'sounds': [{'name': 'fancydrops:' + name, 'stream': False}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', len(SOUNDS), 'sounds')
