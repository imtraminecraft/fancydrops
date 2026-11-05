#!/usr/bin/env python3
"""Synthesizes the 0.9 sound banks: one bank per animation group (beat, burst, reveal, reveal_big, glow, glow_big, end) and the
Collection book's sounds. Writes sounds/<name>.ogg, the sounds.json entries and the envelopes the Sounds tab draws.
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

def bank2(p, beat, burst, inst, notes, big, bed, *, key_end, glow_bed=None, sub=36, step=.075, sp=1.0):
    """One animation's sounds. beat = the three beats, burst = the drop appearing, reveal / reveal_big = the drop shown (Normal / RNG),
    glow / glow_big = the bed under the finish, end = the close."""
    S(p + '_beat', beat, .22); S(p + '_burst', burst, .3)
    r = mix(1.6); at(r, arp(notes, inst, step, .9), 0); at(r, (glow_bed[0](bed, 1.4) if glow_bed else pad(bed, 1.4, 'tri', a=.15)) * .35, 0); at(r, sparkle(1.0, 6) * sp, .15); S(p + '_reveal', r, .3)
    R = mix(3.0); at(R, impact(N(sub) * 2, N(sub), 1.4, .8, 1.2) * .8, 0); at(R, arp(big, inst, step * .9, 1.3), .1)
    at(R, supersaw(bed, 2.6, cut=1800) * .45, 0); at(R, sparkle(2.2, 22) * sp, .3); at(R, riser(.5, 2000, 9000) * .2, 0); S(p + '_reveal_big', R, .38)
    gb = glow_bed[0](bed + [bed[0] + 12], 2.6) if glow_bed else pad(bed + [bed[0] + 12], 2.6, 'sin', a=.6, r=1.8)
    S(p + '_glow', pump(gb, 1.6) * .6, .25); S(p + '_glow_big', pump(add(gb, sparkle(2.6, 16, gain=.12)), 2.2) * .7, .3)
    S(p + '_end', add(arp(key_end, inst, .12, 1.2, .45), pad([key_end[-1] - 12], 1.4, 'sin', a=.02, r=1.2) * .25), .38)

# ---- Mythological: arcane arcs, thunder, a harp over a choir in D lydian
def arc():
    b = mix(.7); t = T(.5); z = osc(1800, .5, 'saw', glide=150) * .3 + bp(noise(.5), 1500, 8000) * .6
    at(b, z * dec(t, 8) * (0.5 + .5 * (rng.random(len(t)) > .35)), 0); at(b, fm(N(74), .5, 1.414, 3, 6) * .35, .02); return b
def thunder():
    b = mix(2.2); at(b, impact(95, 30, 2.0, 1.2, 1.4), 0); at(b, lp(noise(1.8), 500) * env(T(1.8), .05, 1.6) * .9, .05); at(b, choir([62, 69, 74], 1.6, .02) * .3, 0); return b
bank2('rit', arc(), thunder(), harp, [62, 69, 74, 78, 81], [50, 57, 62, 66, 69, 74, 78, 81, 86], [50, 57, 64, 66], key_end=[86, 81, 74, 69], glow_bed=(choir,), sub=38)

# ---- Treasure: a spade in gravel, a chest bursting with coins, a music box in F
def gravel():
    b = mix(.6); at(b, impact(150, 70, .3, .6, .2) * .7, 0)
    for i in range(10): at(b, bp(noise(.02), 1500, 6000) * dec(T(.02), 200) * rng.uniform(.2, .5), .02 + i * .025 + rng.uniform(0, .01))
    return b
def coinrain(n=18, d=1.2):
    b = mix(d)
    for i in range(n): at(b, fm(rng.uniform(2400, 4200), .35, 1.5, 1.4, 10) * rng.uniform(.15, .35), rng.uniform(0, d * .7) * (i / n) ** .5)
    return b
def chestburst():
    b = mix(1.8); at(b, osc(160, .35, 'saw', glide=260, vib=.05, vibf=25) * env(T(.35), .04, .2) * .12, 0); at(b, impact(120, 50, .8, .9, .8), .32); at(b, coinrain(22, 1.3), .36); return b
bank2('tre', gravel(), chestburst(), mbox, [65, 69, 72, 77, 81], [53, 60, 65, 69, 72, 77, 81, 84, 89], [53, 60, 65, 69], key_end=[89, 84, 77, 72], sub=29)

# ---- Slayer: a whooshing strike with a ringing edge, a colossal hit, dark brass and taiko in C minor
def strike():
    b = mix(.8); at(b, sweep_noise(.22, 500, 6000, .3) * .7, 0); at(b, impact(140, 60, .5, 1.1, .5) * .8, .18); at(b, fm(1760, .5, 1.47, 2.5, 6) * .25, .18); return b
def colossal():
    b = mix(2.4); at(b, riser(.35, 800, 6000) * .4, 0); at(b, impact(100, 26, 2.2, 1.3, 1.6) * 1.1, .3)
    at(b, np.tanh(lp(noise(1.2), 900) * 3) * env(T(1.2), .01, 1.0) * .35, .3); at(b, fm(110, 1.8, 2.4, 3, 2.5) * .35, .3); return b
def brass2(notes, d): return supersaw(notes, d, det=.006, cut=1400, a=.05)
bank2('sly', strike(), colossal(), lambda f, d, **kw: fm(f, d, 1.0, 2.2, 3.5) * .8, [60, 63, 67, 72, 75], [48, 55, 60, 63, 67, 72, 75, 79, 84], [36, 43, 48, 51], key_end=[72, 67, 63, 60], glow_bed=(brass2,), sub=24, sp=.6)

# ---- Rare Crops: rustle and a wood knock, an earthy bloom, a kalimba in G
def rustle():
    b = mix(.5); at(b, bp(noise(.3), 2000, 7000) * env(T(.3), .03, .25) * .45, 0); at(b, mallet(N(55), .4) * .6, .05); return b
def bloom():
    b = mix(1.4); at(b, impact(200, 80, .6, .5, .5) * .6, 0); at(b, sweep_noise(.8, 300, 3000, .3) * .4, 0)
    for i, m in enumerate([79, 83, 86, 91]): at(b, kalimba(N(m), .6) * .3, .1 + i * .05)
    return b
bank2('crp', rustle(), bloom(), kalimba, [67, 71, 74, 79, 83], [55, 62, 67, 71, 74, 79, 83, 86, 91], [55, 62, 67, 71], key_end=[91, 86, 79, 74], sub=31)

# ---- Harvest Feast: a pumpkin knock and bells, a festival drum hit, a squeezebox tune in A minor
def pknock():
    b = mix(.5); at(b, osc(180, .3, glide=120) * dec(T(.3), 14) * .8, 0); at(b, fm(N(81), .3, 2.0, 1, 10) * .2, .02); at(b, fm(N(88), .3, 2.0, 1, 10) * .15, .06); return b
def drumhit():
    b = mix(1.4); at(b, impact(130, 55, 1.0, 1.0, .8), 0)
    for i in range(5): at(b, hp(noise(.15), 5000) * dec(T(.15), 18) * .25, .06 + i * .07)
    return b
def squeeze(f, d, **kw): t = T(d); return lp(osc(f, d, 'sq', vib=.008, vibf=6)[:len(t)] * .6 + osc(f * 1.005, d, 'sq')[:len(t)] * .4, 2600) * env(t, .03, d * .5) * .5
bank2('fst', pknock(), drumhit(), squeeze, [69, 72, 76, 81, 84], [57, 64, 69, 72, 76, 81, 84, 88, 93], [57, 64, 69, 72], key_end=[84, 81, 76, 69], sub=33)

# ---- Harvest Bounty: greenhouse glass chimes, a bright bloom, a celesta in E
def glasschime():
    b = mix(.8)
    for i in range(4): at(b, fm(rng.uniform(1800, 3200), .6, 2.76, 2, 5) * .25, i * .05)
    return b
def brightbloom(): b = mix(1.5); at(b, sweep_noise(1.0, 600, 9000, .35) * .45, 0); at(b, sparkle(1.2, 18, 3000, 8000, .2), .1); at(b, impact(260, 120, .5, .4, .3) * .4, 0); return b
bank2('bty', glasschime(), brightbloom(), celesta, [76, 80, 83, 88, 92], [64, 71, 76, 80, 83, 88, 92, 95, 100], [64, 71, 76, 80], key_end=[100, 95, 88, 83], sub=40)

# ---- Pests: a buzzing chirp, a vacuum suck and pop, pizzicato in D
def buzz():
    b = mix(.4); t = T(.25); at(b, fm(N(86), .25, 0.5, 6, 6) * (0.6 + .4 * np.sin(2 * np.pi * 60 * t)) * .5, 0); at(b, fm(N(93), .12, 0.5, 4, 12) * .3, .12); return b
def suck():
    b = mix(1.0); at(b, sweep_noise(.5, 4000, 300, .3) * .6, 0); at(b, osc(300, .5, glide=1200) * env(T(.5), .3, .1) * .3, 0); at(b, impact(400, 150, .3, .6, .2) * .6, .5); return b
bank2('pst', buzz(), suck(), pizz, [74, 78, 81, 86, 90], [62, 69, 74, 78, 81, 86, 90, 93, 98], [62, 69, 74], key_end=[98, 93, 86, 81], sub=38)

# ---- Experimentation Table: a page riffle and a glyph chime, a book slam with a rune hum, celesta and choir in B
def riffle2():
    b = mix(.6)
    for i in range(8): at(b, bp(noise(.03), 2000, 8000) * dec(T(.03), 90) * .35, i * .03)
    at(b, fm(N(83), .4, 3.0, 1.5, 6) * .25, .2); return b
def slam():
    b = mix(1.6); at(b, impact(150, 60, .9, 1.0, .8), 0); t = T(1.4); at(b, fm(N(47), 1.4, 1.0, .8, 1.5) * (1 + .3 * np.sin(2 * np.pi * 7 * t)) * .3, .05); return b
bank2('tab', riffle2(), slam(), celesta, [71, 75, 78, 83, 87], [59, 66, 71, 75, 78, 83, 87, 90, 95], [59, 66, 71, 75], key_end=[95, 90, 83, 78], glow_bed=(choir,), sub=35)

# ---- Water: a bobber plop, a splash column, liquid FM bells in C
def plop2():
    b = mix(.5); at(b, osc(900, .12, glide=250) * env(T(.12), .002, .1) * .7, 0); at(b, bp(noise(.2), 800, 4000) * dec(T(.2), 20) * .3, .04); return b
def splash2():
    b = mix(1.5); at(b, lp(noise(1.0), 3500) * env(T(1.0), .01, .8) * .8, 0); at(b, impact(180, 70, .5, .6, .6) * .6, 0)
    for i in range(10): at(b, osc(rng.uniform(800, 1800), .08, glide=rng.uniform(1800, 3200)) * env(T(.08), .002, .07) * .2, rng.uniform(.1, .8))
    return b
def liquid(f, d, **kw): t = T(d); return fm(f, d, 1.0, 1.4, 3) * (1 + .15 * np.sin(2 * np.pi * 5 * t))
bank2('wat', plop2(), splash2(), liquid, [72, 76, 79, 84, 88], [60, 67, 72, 76, 79, 84, 88, 91, 96], [60, 67, 72, 76], key_end=[96, 91, 84, 79], sub=36)

# ---- Lava: a magma blorp, an eruption, low brass fifths in E minor
def blorp2(): b = mix(.6); at(b, osc(140, .4, glide=60) * env(T(.4), .01, .3) * .8, 0); at(b, lp(noise(.3), 600) * dec(T(.3), 10) * .4, .05); at(b, crackle(.4, 50, .3), .1); return b
def erupt(): b = mix(2.2); at(b, impact(90, 28, 2.0, .9, 1.5), 0); at(b, lp(noise(1.8), 1000) * env(T(1.8), .02, 1.5) * .7, 0); at(b, crackle(1.6, 90, .45), .05); return b
bank2('lav', blorp2(), erupt(), lambda f, d, **kw: fm(f, d, 1.0, 2.6, 3) * .8, [64, 67, 71, 76, 79], [52, 59, 64, 67, 71, 76, 79, 83, 88], [40, 47, 52, 55], key_end=[76, 71, 67, 64], glow_bed=(brass2,), sub=28, sp=.5)

# ---- Spooky: a tolling bell, an organ blast, a wavering theremin in D minor
def toll2(): return fm(N(50), 1.4, 1.4, 3.0, 1.8) * .8
def organ2(notes, d):
    t = T(d); s = sum(osc(N(m), d, 'sin') + osc(N(m) * 2, d, 'sin') * .5 + osc(N(m) * 4, d, 'sin') * .25 for m in notes)
    return s * env(t, .03, d * .6) * .25
def ghostblast(): b = mix(1.8); at(b, organ2([50, 53, 57, 62], 1.6), 0); at(b, impact(110, 40, 1.0, .7, 1.0) * .6, 0); return b
def theremin2(f, d, **kw): t = T(d); return osc(f, d, 'sin', vib=.02, vibf=6) * env(t, .08, d * .6) * .7
bank2('spk', toll2(), ghostblast(), theremin2, [62, 65, 69, 74, 77], [50, 57, 62, 65, 69, 74, 77, 81, 86], [50, 53, 57, 62], key_end=[86, 81, 77, 74], glow_bed=(organ2,), sub=26, sp=.5)

# ---- Winter: an ice crack, crystals shattering, a glockenspiel in A
def crack2(): b = mix(.5); at(b, hp(noise(.05), 2000) * dec(T(.05), 80) * .9, 0); at(b, fm(2400, .4, 1.73, 3, 8) * .3, .01); at(b, osc(800, .2, glide=300) * dec(T(.2), 20) * .3, .02); return b
def shatter2():
    b = mix(1.6); at(b, impact(300, 120, .6, 1.4, .3) * .6, 0)
    for i in range(24): at(b, fm(rng.uniform(2500, 7000), .3, 1.3, 2, 10) * .14, rng.uniform(0, .9))
    return b
bank2('ice', crack2(), shatter2(), glock, [81, 85, 88, 93, 97], [69, 76, 81, 85, 88, 93, 97, 100, 105], [57, 64, 69, 73], key_end=[105, 100, 93, 88], sub=33)

# ---- Crimson Isle: a meteor strike, a fortress boom, a war horn in F minor
def meteor2(): b = mix(.9); at(b, sweep_noise(.35, 5000, 500, .35) * .7, 0); at(b, impact(110, 40, .55, 1.0, .7), .32); at(b, crackle(.4, 70, .35), .34); return b
def fortress(): b = mix(2.2); at(b, impact(80, 24, 2.0, 1.0, 1.6) * 1.1, 0); at(b, crackle(1.5, 80, .4), .05); at(b, supersaw([41, 48, 53], 1.6, cut=900) * .4, .05); return b
def horn2(f, d, **kw): t = T(d); return lp(osc(f, d, 'saw', vib=.006, vibf=4.5), 1500) * env(t, .06, d * .55) * .55
bank2('crm', meteor2(), fortress(), horn2, [65, 68, 72, 77, 80], [53, 60, 65, 68, 72, 77, 80, 84, 89], [41, 48, 53, 56], key_end=[77, 72, 68, 65], glow_bed=(brass2,), sub=29, sp=.5)

# ---- Rare mobs: a rattling cage, iron bars snapping, a gritty arpeggio in G minor
def rattle2():
    b = mix(.6)
    for i in range(5): at(b, fm(rng.uniform(900, 1500), .25, 1.7, 3, 14) * .3, i * .04)
    return b
def bars():
    b = mix(1.5); at(b, impact(140, 50, .8, 1.0, .9) * .8, 0); at(b, fm(700, 1.0, 1.9, 3, 4) * .45, 0)
    for i in range(7): at(b, fm(rng.uniform(1200, 2600), .35, 2.2, 2, 10) * .18, .08 + rng.uniform(0, .6))
    return b
def gritty(f, d, **kw): return lp(osc(f, d * .6, 'sq'), 2600) * dec(T(d * .6), 6) * .5
bank2('mob', rattle2(), bars(), gritty, [67, 70, 74, 79, 82], [55, 62, 67, 70, 74, 79, 82, 86, 91], [43, 50, 55, 58], key_end=[82, 79, 74, 67], glow_bed=(brass2,), sub=31, sp=.6)

# ---- Dyes: a rising drip, a paint splash, a sparkling celesta in Bb
def drip2(): b = mix(.4); at(b, osc(700, .09, glide=2400) * env(T(.09), .002, .08) * .6, 0); at(b, osc(1200, .06, glide=2800) * env(T(.06), .002, .05) * .3, .09); return b
def paint2():
    b = mix(1.3); at(b, lp(noise(.6), 2800) * env(T(.6), .005, .45) * .7, 0); at(b, impact(240, 100, .3, .6, .3) * .5, 0)
    for i in range(10): at(b, osc(rng.uniform(900, 2200), .07, glide=rng.uniform(2200, 3800)) * env(T(.07), .002, .06) * .22, rng.uniform(.05, .7))
    return b
bank2('dye', drip2(), paint2(), celesta, [70, 74, 77, 82, 86], [58, 65, 70, 74, 77, 82, 86, 89, 94], [58, 65, 70, 74], key_end=[94, 89, 82, 77], sub=34)

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
