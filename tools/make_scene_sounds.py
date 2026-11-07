#!/usr/bin/env python3
"""Synthesizes the 1.0 sounds: the soft set every Normal and No Drops scene shares (std_*), the cinematic RNG set (rng_*) and the
star map's sounds. Writes sounds/<name>.ogg, the sounds.json entries and the envelopes the Sounds tab draws.
Needs numpy, scipy and ffmpeg (libvorbis). Run from the repo root: python3 tools/make_scene_sounds.py"""
import os, json, subprocess, tempfile, wave
import numpy as np
from scipy.signal import lfilter, butter

SR = 44100
RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fancydrops')
rng = np.random.default_rng(90)
N = lambda m: 440 * 2 ** ((m - 69) / 12)

# ------------------------------------------------------------------ building blocks
def T(d): return np.arange(int(SR * d)) / SR
def mix(d): return np.zeros(int(SR * d))
def at(buf, s, t0):
    i = int(t0 * SR); n = min(len(s), len(buf) - i)
    if n > 0: buf[i:i + n] += s[:n]
def env(t, a=0.005, r=0.2, p=2.0):
    d = t[-1] + 1 / SR
    return np.minimum(1, t / max(a, 1e-4)) * np.clip((d - t) / max(r, 1e-4), 0, 1) ** p
def dec(t, k): return np.exp(-t * k)
def lp(x, f): b, a = butter(2, min(0.99, f / (SR / 2))); return lfilter(b, a, x)
def hp(x, f): b, a = butter(2, min(0.99, f / (SR / 2)), 'high'); return lfilter(b, a, x)
def bp(x, lo, hi): b, a = butter(2, [lo / (SR / 2), min(0.99, hi / (SR / 2))], 'band'); return lfilter(b, a, x)
def noise(d): return rng.standard_normal(int(SR * d))
def osc(f, d, shape='sin', glide=None, vib=0.0, vibf=5.0):
    t = T(d); fr = np.full_like(t, f, dtype=float) if glide is None else f * (glide / f) ** (t / d)
    if vib: fr = fr * (1 + vib * np.sin(2 * np.pi * vibf * t))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    if shape == 'sin': return np.sin(ph)
    if shape == 'tri': return 2 / np.pi * np.arcsin(np.sin(ph))
    if shape == 'saw': return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if shape == 'sq': return np.sign(np.sin(ph)) * .6
def pluck(f, d, bright=1.0, k=None):
    t = T(d); k = k or 3.2 / d
    s = np.sin(2 * np.pi * f * t) + .45 * bright * np.sin(4 * np.pi * f * t) * dec(t, 12) + .2 * bright * np.sin(6 * np.pi * f * t) * dec(t, 22)
    return s * dec(t, k) * np.minimum(1, t / .002)
def bell(f, d, ratios=(1, 2.76, 5.4, 8.93), amps=(1, .5, .3, .15), k=2.5):
    t = T(d); return sum(a * np.sin(2 * np.pi * f * r * t) * dec(t, k * (1 + i * .8)) for i, (r, a) in enumerate(zip(ratios, amps))) * np.minimum(1, t / .002)
def mallet(f, d):           # marimba-ish: wood with a quick 4x partial
    t = T(d); return (np.sin(2 * np.pi * f * t) * dec(t, 6) + .35 * np.sin(2 * np.pi * f * 4 * t) * dec(t, 30)) * np.minimum(1, t / .001)
def thump(f0, f1, d, k=9):
    return osc(f0, d, glide=f1) * dec(T(d), k) * np.minimum(1, T(d) / .002)
def swish(d, lo, hi, peak=.5):
    t = T(d); x = noise(d); u = t / d
    y = bp(x, lo, hi) * np.clip(np.sin(np.pi * np.clip(u / (peak * 2) if peak < .5 else u, 0, 1)), 0, 1) ** 1.6
    return y
def sweep_noise(d, f0, f1, q=0.25):  # noise through a moving band: whooshes
    out = np.zeros(int(SR * d)); seg = 1024; x = noise(d)
    for i in range(0, len(out), seg):
        u = i / max(1, len(out)); fc = f0 * (f1 / f0) ** u
        out[i:i + seg] = bp(x[max(0, i - 2048):i + seg], fc * (1 - q), fc * (1 + q))[-len(out[i:i + seg]):]
    return out
def pad(notes, d, shape='sin', det=.003, a=.3, r=None):
    t = T(d); s = np.zeros_like(t)
    for m in notes:
        for dd in (-det, 0, det): s += osc(N(m) * (1 + dd), d, shape)
    return s / (3 * len(notes)) * env(t, a, r or d * .6)
def chord(notes, d, kind='pluck', step=0.0, gain=.5, **kw):
    b = mix(d + step * len(notes))
    for i, m in enumerate(notes):
        s = {'pluck': lambda: pluck(N(m), d, **kw), 'bell': lambda: bell(N(m), d, **kw), 'mallet': lambda: mallet(N(m), d)}[kind]()
        at(b, s * gain, i * step)
    return b
def crackle(d, rate=60, amp=.6):
    b = mix(d)
    for _ in range(int(rate * d)):
        t0 = rng.uniform(0, d - .01); at(b, hp(noise(.006), 2000) * dec(T(.006), 500) * rng.uniform(.2, 1) * amp, t0)
    return b
def reverb(x, d=.35, mixv=.25):
    out = x.copy()
    for k, (dl, g) in enumerate([(.031, .5), (.047, .42), (.071, .35), (.103, .28), (.149, .2), (.211, .14)]):
        i = int(dl * SR * (1 + d)); y = np.zeros_like(x); y[i:] = x[:-i] * g; out += lp(y, 5000 - k * 500) * mixv
    return out
def pad_to(x, d): b = mix(max(d, len(x) / SR)); at(b, x, 0); return b

OUT = {}
def S(name, x, rv=.25): OUT[name] = reverb(x, mixv=rv) if rv else x


# ================================================================== 1.0 sound package: new instruments
def fm(f, d, ratio=3.5, index=2.0, k=3.0, a=0.002):
    """An FM tone: a carrier modulated by ratio*f, the brightness dying away faster than the body."""
    t = T(d); mi = index * np.exp(-t * k * 1.6)
    return np.sin(2 * np.pi * f * t + mi * np.sin(2 * np.pi * f * ratio * t)) * env(t, a, d, 1) * np.exp(-t * k * .5)
def supersaw(notes, d, det=.012, cut=2200, a=.08, r=None):
    t = T(d); s = np.zeros(len(t))
    for m in notes:
        for v in (-1, -.4, .3, 1): s += osc(N(m) * (1 + det * v), d, 'saw') * .25
    s = lp(s, cut) * env(t, a, r if r else d * .6)
    return s / max(1, len(notes))
def impact(f0=110, f1=38, d=1.2, bright=1.0, sub=1.0):
    """A layered hit: a click, a pitched body that drops, a noise tail and a sub."""
    b = mix(d); t = T(d)
    at(b, hp(noise(.02), 3000) * dec(T(.02), 160) * .7 * bright, 0)
    at(b, osc(f0, d, glide=f1) * dec(t, 6) * .9, 0)
    at(b, lp(noise(d), 1200 * bright + 300) * dec(t, 5) * .6, 0)
    at(b, osc(f1 * .5 + 20, d) * env(t, .005, d * .8) * .7 * sub, 0)
    return b
def riser(d=.6, f0=300, f1=4200): t = T(d); return sweep_noise(d, f0, f1, .3) * (t / d) ** 2 * .6
def sparkle(d=1.2, n=14, lo=2800, hi=6000, gain=.18):
    b = mix(d)
    for i in range(n): at(b, fm(rng.uniform(lo, hi), .25, ratio=2.0, index=1.2, k=14) * gain, rng.uniform(0, d * .75))
    return b
def arp(notes, inst, step, d=1.0, gain=.5, **kw):
    b = mix(d + step * len(notes))
    for i, m in enumerate(notes): at(b, inst(N(m), d, **kw) * gain * (1 - .03 * i), i * step)
    return b
def add(*xs):
    b = mix(max(len(x) for x in xs) / SR + .01)
    for x in xs: at(b, x, 0)
    return b
def pump(x, rate=2.0):
    t = np.arange(len(x)) / SR; return x * (0.55 + 0.45 * ((t * rate) % 1.0) ** .6)
def kalimba(f, d, **kw): return fm(f, d, ratio=5.04, index=1.4, k=4.5)
def celesta(f, d, **kw): return add(fm(f, d, ratio=4.0, index=1.0, k=3.0), fm(2 * f, d * .6, ratio=3.0, index=.6, k=6) * .3)
def harp(f, d, **kw): return add(pluck(f, d, 1.2) * .8, fm(f, d, ratio=2.0, index=.8, k=4) * .4)
def mbox(f, d, **kw): return fm(f, d, ratio=3.5, index=1.8, k=3.5)
def glock(f, d, **kw): return fm(f, d, ratio=2.76, index=1.5, k=2.2)
def pizz(f, d, **kw): return pluck(f, d * .5, .6) * 1.1
def choir(notes, d, a=.4):
    t = T(d); s = np.zeros(len(t))
    for m in notes: s += osc(N(m), d, 'saw', vib=.006, vibf=5.2) * .3
    s = bp(s, 400, 1600) * 1.4 + bp(s, 2200, 3200) * .5
    return s * env(t, a, d * .6)

# ---- 1.0: two sets only. Every Normal and No Drops scene shares the soft set (short, quiet, in E); RNG has the cinematic one.
def std_set():
    t = T(.22); S('std_beat', add(mallet(N(83), .22) * .55, fm(N(95), .18, 2.0, .8, 18) * .15), .18)
    b = mix(.5); at(b, thump(320, 140, .22, 18) * .5, 0); at(b, sparkle(.35, 5, 3500, 6500, .14), .03); S('std_burst', b, .22)
    S('std_reveal', add(arp([76, 80, 83, 88], celesta, .06, .55, .45), pad([64, 71], .9, 'sin', a=.05, r=.7) * .18), .28)
    S('std_glow', pad([64, 71, 76], 1.0, 'sin', a=.15, r=.75) * .35, .22)
    S('std_end', arp([88, 83], glock, .09, .5, .35), .3)
    b = mix(.6); at(b, osc(700, .5, glide=180) * env(T(.5), .01, .45) * .3, 0); at(b, bp(noise(.4), 500, 3000) * env(T(.4), .02, .35) * .2, 0); S('std_fizzle', b, .2)
def rng_set():
    b = mix(.9); at(b, impact(120, 46, .7, .7, 1.2) * .8, 0); at(b, osc(N(40), .6) * env(T(.6), .005, .5) * .4, 0); at(b, riser(.35, 1500, 7000) * .25, .2); S('rng_beat', b, .25)
    b = mix(1.6); at(b, riser(.25, 600, 8000) * .5, 0); at(b, impact(95, 28, 1.4, 1.3, 1.6), .22); at(b, sweep_noise(.9, 6000, 400, .35) * .35, .22); at(b, sparkle(1.0, 14, gain=.16), .3); S('rng_burst', b, .32)
    b = mix(2.0); at(b, supersaw([52, 59, 64, 68, 71], 1.8, cut=2600, a=.03) * .55, 0); at(b, arp([64, 68, 71, 76, 80, 83, 88, 92, 95], harp, .045, .9, .4), .02)
    at(b, choir([64, 71, 76], 1.6, .08) * .25, 0); at(b, sparkle(1.6, 24, gain=.15), .15); S('rng_reveal', b, .38)
    S('rng_glow', pump(add(pad([52, 59, 64, 71, 76], 1.6, 'tri', a=.2, r=1.0) * .5, sparkle(1.5, 18, gain=.1)), 2.4) * .7, .3)
    S('rng_end', add(arp([95, 92, 88, 83, 76], glock, .07, .8, .35), pad([52], .9, 'sin', a=.02, r=.8) * .25), .38)
std_set(); rng_set()

# ---- the achievements star map
def sky_open():
    b = mix(2.0); at(b, riser(.8, 400, 5000) * .5, 0)
    for i, m in enumerate([72, 79, 84, 88, 91, 96]): at(b, glock(N(m), 1.2) * (.32 - .03 * i), .2 + i * .08)
    at(b, choir([60, 67, 72], 1.8, .3) * .35, 0); return b
def sky_close():
    b = mix(1.0)
    for i, m in enumerate([91, 84, 79]): at(b, glock(N(m), .6) * .25, i * .06)
    at(b, sweep_noise(.4, 3000, 500, .3) * .3, 0); return b
def sky_zoom(): b = mix(1.1); at(b, sweep_noise(.6, 500, 4000, .35) * .5, 0); at(b, sparkle(.9, 10, gain=.12), .1); return b
def sky_star(): return glock(N(88), .9) * .5
S('sky_open', sky_open(), .35); S('sky_close', sky_close(), .3); S('sky_zoom', sky_zoom(), .3); S('sky_star', sky_star(), .35)

# ------------------------------------------------------------------ write
def norm(x, peak=.8): return x / max(1e-6, np.abs(x).max()) * peak
sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
sj = {k: v for k, v in sj.items() if k in OUT}; ej = {k: v for k, v in ej.items() if k in OUT}       # only the sounds this script makes
for name, x in OUT.items():
    x = norm(np.nan_to_num(x)); x[-300:] *= np.linspace(1, 0, 300)
    nz = np.where(np.abs(x) > 0.002)[0]; x = x[:nz[-1] + 400] if len(nz) else x          # trim the silent tail
    with tempfile.TemporaryDirectory() as td:
        wp = os.path.join(td, 'a.wav')
        with wave.open(wp, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wp, '-c:a', 'libvorbis', '-q:a', '3', os.path.join(RES, 'sounds', name + '.ogg')], check=True)
    d = round(len(x) / SR, 2); n = int(d * 40) + 1; hop = SR // 40
    e = [float(np.sqrt(np.mean(x[i * hop:(i + 1) * hop] ** 2))) if i * hop < len(x) else 0 for i in range(n)]
    m = max(e) or 1
    ej[name] = {'d': d, 'e': [int(round(v / m * 99)) for v in e]}
    sj[name] = {'sounds': [{'name': 'fancydrops:' + name, 'stream': False}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', len(OUT), 'sounds:', ' '.join(OUT))
