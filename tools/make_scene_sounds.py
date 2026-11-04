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

# ------------------------------------------------------------------ a reveal: a run of notes on an instrument over a bed; big adds a sub hit, a shimmer and more notes
def reveal(notes, big_notes, inst, bed, bed_shape='sin', sub=36, shimmer=1.0, step=.085, bigstep=.075, dur=1.3, bigdur=2.6, **kw):
    small = mix(dur + .2)
    for i, m in enumerate(notes): at(small, {'pluck': pluck, 'bell': bell, 'mallet': mallet}[inst](N(m), .9 if inst != 'bell' else 1.4, **kw) * (.5 + .05 * i), i * step)
    at(small, pad(bed, dur, bed_shape, a=.2) * .5, 0)
    big = mix(bigdur + .3)
    for i, m in enumerate(big_notes): at(big, {'pluck': pluck, 'bell': bell, 'mallet': mallet}[inst](N(m), 1.2 if inst != 'bell' else 1.8, **kw) * (.45 + .04 * i), .12 + i * bigstep)
    at(big, pad(bed, bigdur, bed_shape, a=.15) * .65, 0)
    at(big, thump(N(sub) * 2, N(sub), .9, k=4) * .9, 0)
    t = T(bigdur)
    sh = sum(np.sin(2 * np.pi * N(m + 24) * t + i) for i, m in enumerate(big_notes[-3:])) * (1 + .6 * np.sin(2 * np.pi * 7 * t))
    at(big, sh * env(t, .5, bigdur * .7) * .035 * shimmer, .3)
    at(big, hp(noise(bigdur), 6000) * env(t, .4, bigdur * .6) * .03 * shimmer, .2)
    return small, big
def glow(notes, d=2.4, shape='sin', lfo=.3, big=False, extra=None):
    t = T(d); s = pad(notes if not big else notes + [notes[0] + 12, notes[-1] + 7], d, shape, a=.5, r=d * .7)
    s *= 1 + lfo * np.sin(2 * np.pi * (.8 if not big else 1.6) * t)
    if extra is not None: at(s, extra, 0)
    return s * (.7 if not big else .9)
def ending(notes, inst='bell', step=.14, d=1.1):
    b = mix(d + step * len(notes))
    for i, m in enumerate(notes): at(b, ({'pluck': pluck, 'bell': bell, 'mallet': mallet}[inst])(N(m), d) * (.5 - .06 * i), i * step)
    return b

def bank(p, beat, burst, rv_notes, rv_big, inst, bed, bed_shape='sin', *, gl, end_notes, end_inst='bell', sub=36, gl_shape='sin', gl_extra=None, **kw):
    S(p + '_beat', beat, .2); S(p + '_burst', burst, .3)
    a, b = reveal(rv_notes, rv_big, inst, bed, bed_shape, sub, **kw); S(p + '_reveal', a, .3); S(p + '_reveal_big', b, .35)
    S(p + '_glow', glow(gl, shape=gl_shape, extra=gl_extra), .2); S(p + '_glow_big', glow(gl, shape=gl_shape, big=True, extra=gl_extra), .25)
    S(p + '_end', ending(end_notes, end_inst), .35)

# ------------------------------------------------------------------ Mythological: crackling arcane lightning, a thunder clap, a harp in D Lydian
def zap(d=.45, f0=2400, f1=180):
    t = T(d); s = osc(f0, d, 'saw', glide=f1) * .3 + bp(noise(d), 1500, 7000) * .7
    return s * dec(t, 7) * (0.6 + 0.4 * (rng.random(len(t)) > .3))
def clap(d=1.6):
    t = T(d); s = lp(noise(d), 900) * dec(t, 3.5) * 2.2 + hp(noise(d), 2500) * dec(t, 20) * .8 + thump(90, 35, d, 3) * 1.2
    return s
bank('rit', zap(), clap(), [62, 69, 74, 78, 81], [50, 57, 62, 66, 69, 74, 78, 81, 86], 'pluck', [50, 57, 64, 66], 'tri', sub=38,
     gl=[62, 69, 76], end_notes=[86, 81, 74], gl_shape='tri')

# ------------------------------------------------------------------ Treasure: a spade biting dirt, a chest lid and coins, a music box in F
def dig():
    b = mix(.5); at(b, thump(140, 60, .25, 14) * .9, 0); at(b, lp(noise(.3), 2500) * env(T(.3), .002, .25) * .5, .01)
    for i in range(6): at(b, hp(noise(.01), 1500) * dec(T(.01), 300) * .4, .04 + i * .03 + rng.uniform(0, .01))
    return b
def coins(n=12, d=1.0):
    b = mix(d)
    for i in range(n): at(b, bell(rng.uniform(2600, 4200), .3, ratios=(1, 1.5, 2.3), amps=(1, .4, .2), k=12) * rng.uniform(.2, .45), rng.uniform(0, d * .6) * (i / n) ** .5)
    return b
def chest():
    b = mix(1.4); t = T(.4); at(b, osc(180, .4, 'saw', glide=240, vib=.04, vibf=23) * env(t, .05, .2) * .12, 0)
    at(b, thump(110, 55, .3, 10) * .8, .38); at(b, coins(16, 1.0), .42); return b
bank('tre', dig(), chest(), [65, 69, 72, 77, 81], [53, 60, 65, 69, 72, 77, 81, 84, 89], 'bell', [53, 60, 65, 69], sub=29,
     gl=[65, 72, 77], end_notes=[89, 84, 77], gl_extra=coins(8, 2.0) * .4, k=3.5)

# ------------------------------------------------------------------ Slayer: a ringing blade, a heavy cleave, a dark brass chord in C minor
def blade():
    b = mix(.6); at(b, sweep_noise(.28, 600, 5000, .3) * .8, 0)
    at(b, bell(1850, .5, ratios=(1, 1.47, 2.09, 2.83), amps=(1, .6, .4, .2), k=6) * .35, .2); return b
def cleave():
    b = mix(1.3); at(b, sweep_noise(.18, 3000, 400, .35) * .7, 0); at(b, thump(120, 40, 1.0, 4) * 1.2, .16)
    at(b, bell(220, 1.0, ratios=(1, 2.4, 3.9, 5.2), amps=(1, .7, .5, .3), k=4) * .4, .16); at(b, lp(noise(.5), 600) * dec(T(.5), 8) * .8, .16); return b
def brass(notes, d):
    b = mix(d); t = T(d)
    for m in notes: at(b, lp(osc(N(m), d, 'saw', vib=.004), 1800) * env(t, .06, d * .5) * .3, 0)
    return b
_sa, _sb = brass([48, 55, 60, 63], 1.3), brass([36, 43, 48, 55, 60, 63, 67], 2.6)
S('sly_beat', blade(), .2); S('sly_burst', cleave(), .3)
s1 = mix(1.5); at(s1, _sa, 0); at(s1, chord([72, 75, 79], .9, 'pluck', .07, .35), .05); S('sly_reveal', s1, .35)
s2 = mix(2.9); at(s2, _sb, 0); at(s2, thump(N(36) * 2, N(24), 1.2, 3) * 1.0, 0); at(s2, chord([72, 75, 79, 84, 87], 1.2, 'pluck', .08, .35), .1); S('sly_reveal_big', s2, .4)
S('sly_glow', glow([36, 43, 48], shape='saw') * .5, .2); S('sly_glow_big', glow([36, 43, 48], shape='saw', big=True) * .5, .25)
S('sly_end', bell(N(36) * 2, 2.2, ratios=(1, 2.1, 3.3, 4.6), amps=(1, .5, .35, .2), k=1.5) * .7, .35)

# ------------------------------------------------------------------ Rare Crops: a rustling scythe, an earthy pop, a marimba in G
def scythe():
    b = mix(.5); at(b, swish(.32, 1200, 6000) * .7, 0)
    for i in range(10): at(b, bp(noise(.02), 2000, 6000) * dec(T(.02), 150) * .25, .05 + i * .025)
    return b
def earth_pop():
    b = mix(.8); at(b, lp(noise(.5), 900) * env(T(.5), .15, .3) * .5, 0); at(b, thump(90, 200, .2, 12) * .9, .45); at(b, mallet(N(79), .4) * .5, .5); return b
bank('crp', scythe(), earth_pop(), [67, 71, 74, 79, 83], [55, 62, 67, 71, 74, 79, 83, 86, 91], 'mallet', [55, 62, 67, 71], sub=31,
     gl=[67, 74, 79], end_notes=[91, 86, 79], end_inst='mallet')

# ------------------------------------------------------------------ Harvest Feast: a hollow pumpkin knock, a festival drum, a warm accordion-ish chord in A minor
def knock():
    b = mix(.5); at(b, thump(260, 180, .18, 18) * .8, 0); at(b, thump(200, 140, .2, 16) * .6, .11); return b
def festival():
    b = mix(1.0); at(b, thump(150, 60, .5, 8) * 1.0, 0); at(b, hp(noise(.25), 3000) * dec(T(.25), 14) * .5, 0)
    for i, m in enumerate([69, 72, 76]): at(b, bell(N(m + 12), .6, k=5) * .25, .1 + i * .06)
    return b
bank('fst', knock(), festival(), [69, 72, 76, 81, 84], [57, 64, 69, 72, 76, 81, 84, 88, 93], 'pluck', [57, 64, 69, 72], 'saw', sub=33,
     gl=[57, 64, 69], end_notes=[88, 84, 76], gl_shape='tri', bright=1.4)

# ------------------------------------------------------------------ Harvest Bounty: greenhouse glass chimes, a soft whoosh, a music box in E
def glass_chime():
    b = mix(.6)
    for i, f in enumerate([3520, 4186, 4699]): at(b, bell(f, .5, ratios=(1, 2.2, 3.6), amps=(1, .3, .1), k=8) * .3, i * .05)
    return b
def green_whoosh():
    b = mix(1.0); at(b, sweep_noise(.7, 400, 3000, .3) * .6, 0); at(b, chord([76, 83, 88], .6, 'bell', .05, .3, k=6), .6); return b
bank('bty', glass_chime(), green_whoosh(), [76, 80, 83, 88, 92], [64, 71, 76, 80, 83, 88, 92, 95, 100], 'bell', [64, 71, 76, 80], sub=40,
     gl=[76, 83, 88], end_notes=[100, 95, 88], k=4)

# ------------------------------------------------------------------ Pests: a bug chirp, a vacuum slurp, bouncy pizzicato in D
def chirp():
    b = mix(.4)
    for i in range(3): at(b, osc(3200, .05, 'sin', glide=4800) * env(T(.05), .002, .04) * .5, i * .07)
    return b
def slurp():
    d = .7; t = T(d); b = mix(.9); ph_f = 300 + 900 * (t / d) ** 2
    at(b, (np.sin(2 * np.pi * np.cumsum(ph_f) / SR) * .3 + bp(noise(d), 500, 2500) * .7) * np.sin(np.pi * t / d) ** 1.3, 0)
    at(b, thump(400, 900, .08, 30) * .6, .7); return b
bank('pst', chirp(), slurp(), [74, 78, 81, 86, 90], [62, 69, 74, 78, 81, 86, 90, 93, 98], 'pluck', [62, 69, 74], sub=38,
     gl=[62, 69, 74], end_notes=[86, 81, 74], end_inst='pluck', k=9)

# ------------------------------------------------------------------ Experimentation Table: a page riffle, a book slam with runes, a celesta in B
def riffle():
    b = mix(.6)
    for i in range(10): at(b, bp(noise(.03), 1500, 8000) * env(T(.03), .001, .025, 1.5) * (.7 - i * .04), i * .04 + rng.uniform(0, .01))
    return b
def rune_slam():
    b = mix(1.2); at(b, thump(80, 45, .4, 9) * 1.0, 0); at(b, lp(noise(.12), 3000) * dec(T(.12), 30) * .6, 0)
    t = T(1.0); at(b, (np.sin(2 * np.pi * N(71) * t) + .7 * np.sin(2 * np.pi * N(78) * t)) * env(t, .2, .6) * .3 * (1 + .4 * np.sin(2 * np.pi * 9 * t)), .1); return b
bank('tab', riffle(), rune_slam(), [71, 75, 78, 83, 87], [59, 66, 71, 75, 78, 83, 87, 90, 95], 'bell', [59, 66, 71, 75], sub=35,
     gl=[71, 78, 83], end_notes=[95, 90, 83], k=3)

# ------------------------------------------------------------------ Water: a bobber plop, a splash, liquid bells in C with chorus
def plop():
    b = mix(.5); at(b, osc(500, .12, glide=1400) * env(T(.12), .002, .1) * .6, 0); at(b, lp(noise(.15), 2500) * dec(T(.15), 25) * .25, 0)
    for i in range(3): at(b, osc(rng.uniform(900, 1600), .06, glide=rng.uniform(1800, 2600)) * env(T(.06), .002, .05) * .25, .1 + i * .05)
    return b
def splash():
    b = mix(1.3); at(b, lp(noise(1.0), 4000) * env(T(1.0), .01, .8) * .7, 0); at(b, thump(110, 50, .4, 8) * .7, 0)
    for i in range(14): at(b, osc(rng.uniform(700, 2400), .05, glide=rng.uniform(1500, 4000)) * env(T(.05), .002, .04) * .25, rng.uniform(.1, .9))
    return b
bank('wat', plop(), splash(), [72, 76, 79, 84, 88], [60, 67, 72, 76, 79, 84, 88, 91, 96], 'bell', [60, 67, 72, 76], sub=36,
     gl=[60, 67, 76], end_notes=[96, 91, 84], gl_extra=plop() * .15, k=2.2)

# ------------------------------------------------------------------ Lava: a magma blorp, an eruption, low brassy fifths in E minor
def blorp():
    b = mix(.6); at(b, osc(90, .25, glide=180) * env(T(.25), .02, .2) * .8, 0); at(b, thump(300, 120, .08, 40) * .6, .22); at(b, crackle(.4, 50, .4), .2); return b
def eruption():
    b = mix(1.8); t = T(1.6); at(b, lp(noise(1.6), 400) * env(t, .05, 1.3) * 2.0, 0); at(b, thump(70, 30, 1.2, 3) * 1.2, 0); at(b, crackle(1.4, 90, .5), .1); return b
S('lav_beat', blorp(), .2); S('lav_burst', eruption(), .3)
l1 = mix(1.5); at(l1, brass([40, 47, 52], 1.3) * 1.2, 0); at(l1, chord([64, 71, 76], .9, 'pluck', .08, .35), .05); S('lav_reveal', l1, .35)
l2 = mix(2.9); at(l2, brass([28, 35, 40, 47, 52, 59], 2.6) * 1.2, 0); at(l2, thump(N(28) * 2, N(16), 1.2, 3), 0); at(l2, chord([64, 71, 76, 79, 83, 88], 1.2, 'pluck', .08, .32), .1); at(l2, crackle(2.0, 40, .3), .2); S('lav_reveal_big', l2, .4)
S('lav_glow', glow([40, 47, 52], shape='tri', extra=crackle(2.4, 30, .25)), .2); S('lav_glow_big', glow([40, 47, 52], shape='tri', big=True, extra=crackle(2.4, 60, .3)), .25)
S('lav_end', ending([76, 71, 64], 'pluck'), .3)

# ------------------------------------------------------------------ Spooky: a tolling bell, an organ stab, a wavering theremin line in D minor
def toll(): return bell(N(50), 1.3, ratios=(1, 2.0, 2.4, 3.0, 4.2), amps=(1, .6, .5, .3, .2), k=2) * .8
def organ(notes, d):
    b = mix(d); t = T(d)
    for m in notes:
        for h, a in ((1, 1), (2, .5), (4, .25), (8, .12)): at(b, np.sin(2 * np.pi * N(m) * h * t) * a * env(t, .02, d * .5) * .12, 0)
    return b
def theremin(notes, d):
    b = mix(d + .4); seg = d / len(notes)
    for i, m in enumerate(notes): at(b, osc(N(m), seg * 1.4, vib=.012, vibf=5.5) * env(T(seg * 1.4), .05, seg * .8) * .35, i * seg)
    return b
S('spk_beat', toll(), .4); S('spk_burst', organ([50, 57, 62, 65], 1.2), .45)
k1 = mix(1.6); at(k1, theremin([74, 77, 81, 86], 1.1), 0); at(k1, organ([50, 57, 62], 1.4) * .6, 0); S('spk_reveal', k1, .4)
k2 = mix(3.0); at(k2, theremin([62, 69, 74, 77, 81, 86, 89], 2.2), .1); at(k2, organ([38, 50, 57, 62, 65], 2.6) * .8, 0); at(k2, toll() * .5, 0); S('spk_reveal_big', k2, .45)
S('spk_glow', glow([50, 53, 57], shape='tri', lfo=.5), .3); S('spk_glow_big', glow([50, 53, 57], shape='tri', lfo=.6, big=True), .35)
S('spk_end', ending([86, 81, 77, 74], 'bell'), .45)

# ------------------------------------------------------------------ Winter: an ice crack, a shatter of crystals, a glockenspiel in A
def ice_crack():
    b = mix(.5); at(b, hp(noise(.02), 2500) * dec(T(.02), 200) * 1.0, 0)
    for i in range(5): at(b, hp(noise(.01), 3000) * dec(T(.01), 300) * .5, .02 + i * .018 + rng.uniform(0, .01))
    at(b, bell(2900, .4, ratios=(1, 2.3, 3.7), amps=(1, .4, .2), k=9) * .3, .01); return b
def shatter():
    b = mix(1.4); at(b, hp(noise(.08), 2000) * dec(T(.08), 40) * 1.0, 0)
    for i in range(30): at(b, bell(rng.uniform(2500, 6000), .25, ratios=(1, 2.1, 3.3), amps=(1, .3, .1), k=14) * rng.uniform(.1, .3), rng.uniform(0, .9) ** 1.5)
    return b
def wind(d): t = T(d); return bp(noise(d), 500, 1800) * (0.5 + .5 * np.sin(2 * np.pi * .7 * t)) * env(t, .5, d * .5) * .25
bank('ice', ice_crack(), shatter(), [81, 85, 88, 93, 97], [69, 76, 81, 85, 88, 93, 97, 100, 105], 'bell', [57, 64, 69, 73], sub=33,
     gl=[69, 76, 81], end_notes=[105, 100, 93], gl_extra=wind(2.4), k=3, ratios=(1, 3.0, 5.8), amps=(1, .3, .1))

# ------------------------------------------------------------------ Crimson Isle: a meteor whoosh and impact, a war horn in F minor, fire
def meteor():
    b = mix(.8); at(b, sweep_noise(.3, 4000, 500, .35) * .7, 0); at(b, thump(100, 40, .45, 7) * 1.0, .3); at(b, crackle(.4, 70, .4), .32); return b
def boom():
    b = mix(1.8); at(b, lp(noise(1.5), 700) * env(T(1.5), .01, 1.2) * 1.5, 0); at(b, thump(80, 28, 1.3, 2.5) * 1.3, 0); at(b, crackle(1.2, 70, .4), .05); return b
def horn(notes, d):
    b = mix(d); t = T(d)
    for m in notes: at(b, lp(osc(N(m), d, 'saw', vib=.006, vibf=4.5), 1300) * env(t, .18, d * .45) * .3, 0)
    return b
S('crm_beat', meteor(), .25); S('crm_burst', boom(), .3)
c1 = mix(1.6); at(c1, horn([53, 60, 65], 1.4), 0); at(c1, chord([77, 80, 84], 1.0, 'pluck', .08, .3), .15); S('crm_reveal', c1, .35)
c2 = mix(3.0); at(c2, horn([41, 48, 53, 60, 65], 2.7), 0); at(c2, thump(N(29) * 2, N(17), 1.4, 2.5), 0); at(c2, chord([77, 80, 84, 89, 92], 1.3, 'pluck', .09, .3), .2); at(c2, crackle(2.2, 40, .3), .2); S('crm_reveal_big', c2, .4)
S('crm_glow', glow([41, 48, 53], shape='saw', extra=crackle(2.4, 35, .25)) * .6, .2); S('crm_glow_big', glow([41, 48, 53], shape='saw', big=True, extra=crackle(2.4, 60, .3)) * .6, .25)
S('crm_end', ending([84, 80, 77], 'pluck'), .35)

# ------------------------------------------------------------------ Rare mobs: a rattling cage, the bars snapping, a gritty arpeggio in G minor
def rattle():
    b = mix(.5)
    for i in range(4): at(b, bell(rng.uniform(900, 1400), .2, ratios=(1, 1.7, 2.6, 3.9), amps=(1, .6, .4, .3), k=18) * .3, i * .045)
    at(b, lp(noise(.15), 3000) * dec(T(.15), 20) * .3, 0); return b
def snap():
    b = mix(1.2); at(b, bell(700, 1.0, ratios=(1, 1.9, 2.8, 4.1), amps=(1, .7, .5, .3), k=5) * .5, 0); at(b, thump(130, 50, .5, 7) * .9, 0)
    for i in range(6): at(b, bell(rng.uniform(1200, 2500), .3, ratios=(1, 2.2), amps=(1, .3), k=12) * .2, .1 + rng.uniform(0, .5))
    return b
S('mob_beat', rattle(), .2); S('mob_burst', snap(), .3)
def grit(notes, d, step):
    b = mix(d + step * len(notes))
    for i, m in enumerate(notes): at(b, lp(osc(N(m), .5, 'sq'), 2500) * dec(T(.5), 6) * .35, i * step)
    return b
m1 = mix(1.5); at(m1, grit([67, 70, 74, 79, 82], .9, .08), 0); at(m1, pad([43, 50, 55], 1.3, 'saw') * .25, 0); S('mob_reveal', m1, .3)
m2 = mix(2.9); at(m2, grit([55, 62, 67, 70, 74, 79, 82, 86, 91], 1.8, .07), .1); at(m2, pad([31, 43, 50, 55], 2.6, 'saw') * .3, 0); at(m2, thump(N(31) * 2, N(19), 1.2, 3), 0); S('mob_reveal_big', m2, .35)
S('mob_glow', glow([43, 50, 55], shape='saw') * .45, .2); S('mob_glow_big', glow([43, 50, 55], shape='saw', big=True) * .45, .25)
S('mob_end', ending([79, 74, 67], 'pluck'), .3)

# ------------------------------------------------------------------ Dyes: a rising drip, a paint splash, a sparkling arpeggio in Bb
def drip():
    b = mix(.4); at(b, osc(700, .09, glide=2200) * env(T(.09), .002, .08) * .6, 0); at(b, osc(1100, .06, glide=2600) * env(T(.06), .002, .05) * .3, .09); return b
def paint_splash():
    b = mix(1.1); at(b, lp(noise(.5), 2500) * env(T(.5), .005, .4) * .7, 0); at(b, thump(220, 90, .25, 10) * .6, 0)
    for i in range(8): at(b, osc(rng.uniform(900, 2000), .07, glide=rng.uniform(2000, 3500)) * env(T(.07), .002, .06) * .25, rng.uniform(.05, .6))
    return b
bank('dye', drip(), paint_splash(), [70, 74, 77, 82, 86], [58, 65, 70, 74, 77, 82, 86, 89, 94], 'bell', [58, 65, 70, 74], sub=34,
     gl=[70, 77, 82], end_notes=[94, 89, 82], k=3.5, ratios=(1, 2.0, 3.01, 4.2), amps=(1, .45, .25, .1))

# ------------------------------------------------------------------ The Collection book
def whoosh(): return sweep_noise(.4, 300, 2500, .35) * .6
def book_open():
    b = mix(1.1); at(b, osc(160, .5, 'saw', glide=210, vib=.03, vibf=28) * env(T(.5), .08, .3) * .08, 0)        # the leather creaks
    at(b, sweep_noise(.35, 500, 1500, .3) * .5, .15); at(b, thump(90, 55, .3, 10) * .7, .5)
    at(b, chord([72, 76, 79, 84], 1.0, 'bell', .06, .22, k=4), .55); return b
def book_land(): b = mix(.6); at(b, thump(110, 60, .25, 14) * .8, 0); at(b, lp(noise(.08), 2000) * dec(T(.08), 40) * .3, 0); return b
def book_close():
    b = mix(.8); at(b, sweep_noise(.25, 1500, 400, .3) * .5, 0); at(b, thump(100, 50, .3, 12) * 1.0, .22); at(b, lp(noise(.06), 3000) * dec(T(.06), 50) * .5, .22); return b
def book_page():
    b = mix(.5); x = bp(noise(.35), 1200, 7000); t = T(.35); at(b, x * np.sin(np.pi * t / .35) ** 2 * .5, 0)
    at(b, hp(noise(.02), 3000) * dec(T(.02), 200) * .3, .3); return b
def book_tab(): b = mix(.3); at(b, lp(noise(.05), 4000) * dec(T(.05), 70) * .5, 0); at(b, mallet(N(84), .2) * .3, .01); return b
def book_locked():
    b = mix(.5); at(b, bell(420, .25, ratios=(1, 2.3, 3.1), amps=(1, .5, .3), k=16) * .5, 0); at(b, bell(380, .25, ratios=(1, 2.3, 3.1), amps=(1, .5, .3), k=16) * .5, .09); return b
def book_equip(): return chord([76, 83, 88], .7, 'bell', .05, .35, k=5)
def book_unequip(): return chord([83, 76], .5, 'bell', .06, .3, k=7)
S('book_whoosh', whoosh(), .1); S('book_open', book_open(), .25); S('book_land', book_land(), .15); S('book_close', book_close(), .2)
S('book_page', book_page(), .1); S('book_tab', book_tab(), .1); S('book_locked', book_locked(), .15); S('book_equip', book_equip(), .25); S('book_unequip', book_unequip(), .2)

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
