#!/usr/bin/env python3
"""Draws the Name Icon strips (textures/font/suffix_<id>.png, 60x10, six 10x10 frames), adds their bitmap providers to font/emoji.json,
and (with --sheet out.png) writes a contact sheet to look at. Run tools/make_suffix_sprites.py afterwards (it builds the chat sprites).
Every icon is drawn pixel by pixel here: a body lit from the top left, a darker underside, a one-pixel outline and a small motion."""
import os, sys, json, math
from PIL import Image

RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fancydrops')
F = 6

def hx(s): return ((s >> 16) & 255, (s >> 8) & 255, s & 255)
def new(): return [[None] * 10 for _ in range(10)]
def px(g, x, y, c):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < 10 and 0 <= y < 10 and c is not None: g[y][x] = hx(c) if isinstance(c, int) else c
def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): px(g, x, y, c)
def disc(g, cx, cy, r, c):
    for y in range(10):
        for x in range(10):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6: px(g, x, y, c)
def line(g, x0, y0, x1, y1, c):
    n = int(math.ceil(max(abs(x1 - x0), abs(y1 - y0), 1)))
    for i in range(n + 1): px(g, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, c)

def outline(g, col):
    if col is None: return g
    o = [row[:] for row in g]
    for y in range(10):
        for x in range(10):
            if g[y][x] is None and any(0 <= x + dx < 10 and 0 <= y + dy < 10 and g[y + dy][x + dx] is not None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[y][x] = hx(col)
    return o

ICONS = {}
def icon(name, edge):
    def deco(fn): ICONS[name] = (fn, edge); return fn
    return deco

# ---------------------------------------------------------------- Special
@icon('bug', 0x3A1410)
def bug(f, g):                                                    # a red beetle seen from above: legs and feelers move
    for lx, ly in ((1, 5), (1, 7), (8, 5), (8, 7)):
        px(g, lx - (1 if f % 2 == (ly == 5) else 0) * (1 if lx < 5 else -1) * 0, ly + (1 if (f + lx + ly) % 2 else 0) * 0, 0x2A1410)
    px(g, 1, 5 + (f % 2), 0x2A1410); px(g, 8, 5 + ((f + 1) % 2), 0x2A1410); px(g, 1, 7 - (f % 2), 0x2A1410); px(g, 8, 7 - ((f + 1) % 2), 0x2A1410)
    fx = 1 + (f % 3 == 0); px(g, 2 + (f % 2), 1, 0x2A1410); px(g, 7 - (f % 2), 1, 0x2A1410); px(g, 3, 2, 0x2A1410); px(g, 6, 2, 0x2A1410)
    rect(g, 3, 3, 6, 4, 0x2A1410); px(g, 3, 3, 0xFFE27A); px(g, 6, 3, 0xFFE27A)
    rect(g, 2, 5, 7, 8, 0xB8402A); rect(g, 3, 4, 6, 4, 0xB8402A); rect(g, 3, 9, 6, 9, 0x8A2E1E)
    rect(g, 2, 8, 7, 8, 0x8A2E1E)
    rect(g, 2, 5, 3, 6, 0xE8704A); px(g, 3, 5, 0xF8A080)
    rect(g, 4, 4, 5, 8, 0x4A1A12)
    for sx, sy in ((2, 7), (7, 6), (3, 6)): px(g, sx, sy, 0x2A1010)
    t = (f + 0) % 6
    if t < 3: px(g, 2 + t, 5 + t // 2 * 0, 0xFFFFFF)                # a glint slides over the shell

@icon('patron', 0x5A2E08)
def patron(f, g):                                                 # the Patreon mark in gold with a glint that sweeps over it
    rect(g, 1, 1, 2, 8, 0xE89A10); rect(g, 1, 1, 1, 8, 0xFFD060)
    disc(g, 6.5, 4.2, 2.3, 0xF8B21C)
    for y in range(10):
        for x in range(10):
            if g[y][x] == hx(0xF8B21C) and ((x - 6.5) + (y - 4.2)) > 1.6: g[y][x] = hx(0xC77C0C)
    px(g, 5, 2, 0xFFE48A); px(g, 6, 2, 0xFFE48A); px(g, 5, 3, 0xFFE48A)
    k = f * 3 - 2
    for i in range(10):
        x, y = i, k - i + 1
        if 0 <= x < 10 and 0 <= y < 10 and g[y][x] is not None and f < 5: g[y][x] = hx(0xFFFFFF)

# ---------------------------------------------------------------- Legendary
@icon('minos', 0x4A2A04)
def minos(f, g):                                                  # a gold crown, the gem pulses
    rect(g, 1, 4, 8, 7, 0xF2B01E); rect(g, 1, 4, 8, 4, 0xFFE27A); rect(g, 1, 7, 8, 7, 0xB87810)
    for x in (1, 4, 5, 8): rect(g, x, 2, x, 3, 0xFFD24A)
    px(g, 1, 1 + 0, None)
    for x in (1, 8): px(g, x, 2, 0xFFE27A)
    px(g, 4, 1, 0xFFE27A); px(g, 5, 1, 0xFFE27A)
    rect(g, 4, 2, 5, 3, 0xFFD24A)
    pul = (0, 1, 2, 2, 1, 0)[f]
    gem = [0xB0201A, 0xE03A2E, 0xFF6A5A][pul]
    rect(g, 4, 5, 5, 6, gem); px(g, 4, 5, 0xFFC0B8 if pul else 0xE8665A)
    for x in (2, 7): px(g, x, 5, 0xFFFFFF if (f + x) % 6 == 0 else 0xE89A10)

@icon('dice', 0x3A4258)
def dice(f, g):                                                   # a white die that hops and lands on a six
    hop = (0, 2, 3, 2, 0, 0)[f]
    y0 = 2 + (0 if hop == 0 else -hop // 2 + 0) + (1 if hop == 0 else 0)
    y0 = {0: 3, 1: 2, 2: 1, 3: 2, 4: 3, 5: 3}[f]
    rect(g, 2, y0, 7, y0 + 5, 0xF4F6FB); rect(g, 2, y0, 7, y0, 0xFFFFFF); rect(g, 7, y0 + 1, 7, y0 + 5, 0xB8C0D4); rect(g, 2, y0 + 5, 7, y0 + 5, 0xA0A8BE)
    pips = {0: [(3, 1), (6, 4)], 1: [(3, 1), (6, 4), (4, 2)], 2: [(3, 1), (6, 1), (3, 4), (6, 4), (4, 2)], 3: [(3, 1), (6, 1), (3, 4), (6, 4)],
            4: [(3, 1), (6, 1), (3, 2), (6, 3), (3, 4), (6, 4)], 5: [(3, 1), (6, 1), (3, 2), (6, 3), (3, 4), (6, 4)]}[f]
    for x, y in pips: px(g, x, y0 + y, 0xE23A3A if (f < 5 and len(pips) == 2) else 0x181820)
    if f == 5: px(g, 2, 0, 0xFFE27A); px(g, 7, 0, 0xFFE27A)
    if hop == 0 and f != 0: pass
    rect(g, 3, 9, 6, 9, 0x00000000) if False else None

# ---------------------------------------------------------------- Rare
@icon('chest', 0x2A1A08)
def chest(f, g):                                                  # a wooden chest that creaks open and glows gold
    op = (0, 0, 1, 2, 2, 1)[f]
    rect(g, 1, 5, 8, 8, 0x8A5A2C); rect(g, 1, 5, 8, 5, 0xA87440); rect(g, 1, 8, 8, 8, 0x5A3A1C)
    rect(g, 1, 6, 8, 6, 0xF2B01E); rect(g, 4, 6, 5, 7, 0xFFE27A); px(g, 4, 7, 0x3A2A10)
    top = 2 - 0
    if op == 0: rect(g, 1, 3, 8, 4, 0x9A6A34); rect(g, 1, 3, 8, 3, 0xB88048); rect(g, 1, 4, 8, 4, 0xF2B01E)
    else:
        rect(g, 1, 3 - op, 8, 4 - op, 0x9A6A34); rect(g, 1, 3 - op, 8, 3 - op, 0xB88048)
        rect(g, 2, 4, 7, 4, 0xFFE27A if op > 1 else 0xF2B01E)
        for x in range(2, 8): px(g, x, 4 - 1, 0xFFF0A0) if op == 2 and x % 2 == 0 else None

@icon('anvil', 0x20242E)
def anvil(f, g):                                                  # a grey anvil that throws a spark when struck
    rect(g, 1, 3, 8, 4, 0x9AA3B5); rect(g, 1, 3, 8, 3, 0xD0D6E4); rect(g, 8, 4, 9, 4, 0x9AA3B5); rect(g, 0, 3, 1, 3, 0x9AA3B5)
    rect(g, 3, 5, 6, 6, 0x7A8398); rect(g, 2, 7, 7, 8, 0x5A6275); rect(g, 2, 7, 7, 7, 0x7A8398)
    if f in (2, 3): px(g, 2 + f, 1, 0xFFE27A); px(g, 6 - (f - 2), 0, 0xFFFFFF); px(g, 7, 2, 0xFFB03A)
    if f == 3: px(g, 3, 2, 0xFFD060)
    if f == 4: px(g, 5, 1, 0xFFB03A)

@icon('disc', 0x10101A)
def disc_i(f, g):                                                 # a black record that spins, the label glints
    disc(g, 4.5, 4.5, 3.6, 0x22242E)
    for y in range(10):
        for x in range(10):
            if g[y][x] is not None:
                r = ((x - 4.5) ** 2 + (y - 4.5) ** 2) ** 0.5
                if 1.2 < r < 3.2 and (int(r * 2) % 2 == 0): g[y][x] = hx(0x34374A)
    disc(g, 4.5, 4.5, 1.2, 0xE8505A)
    px(g, 4, 4, 0x181820);
    a = f * math.pi / 3
    px(g, 4.5 + math.cos(a) * 2.6, 4.5 + math.sin(a) * 2.6, 0xFFFFFF); px(g, 4.5 - math.cos(a) * 2.6, 4.5 - math.sin(a) * 2.6, 0x8A93B8)

# ---------------------------------------------------------------- Common
@icon('rod', 0x1C2A38)
def rod(f, g):                                                    # a fishing rod, the bobber dips
    line(g, 1, 8, 7, 1, 0x8A5A2C); px(g, 8, 1, 0xC49A6C)
    line(g, 8, 1, 8, 4 + (f in (2, 3)) + (f == 3), 0xD8E4F0)
    by = 5 + (f in (2, 3)) + (f == 3)
    rect(g, 7, by, 8, by + 1, 0xE23A3A); rect(g, 7, by + 1, 8, by + 1, 0xFFFFFF)
    if f in (3, 4): px(g, 6, by + 2, 0x9AD8FF); px(g, 9, by + 2, 0x9AD8FF)
    px(g, 2, 8, 0x5A3A1C)

@icon('tractor', 0x10301A)
def tractor(f, g):                                                # a small green tractor, the wheels turn
    rect(g, 1, 0, 4, 3, 0x3AA848); rect(g, 2, 1, 3, 2, 0xBFE8FF); rect(g, 1, 0, 4, 0, 0x6ACB5A)
    rect(g, 0, 4, 8, 5, 0x2E8A3A); rect(g, 0, 4, 8, 4, 0x6ACB5A); rect(g, 5, 3, 8, 3, 0x2E8A3A)
    px(g, 7, 2, 0x3A3A44); px(g, 7, 1 - (f % 2), 0xE8E8E8)
    for wx, wy, r in ((2, 7.5, 2.0), (7, 8, 1.2)):
        disc(g, wx, wy, r, 0x1A1A20)
        a = f * math.pi / 3
        px(g, wx, wy, 0xE8B83A)
        if r > 1.5: px(g, wx + math.cos(a) * 1.4, wy + math.sin(a) * 1.4, 0x9A9AA6)

@icon('bell', 0x4A3008)
def bell(f, g):                                                   # a gold bell that swings and rings
    sw = (0, 1, 1, 0, -1, -1)[f]
    rect(g, 4 + sw // 1, 0, 5 + sw, 0, 0xB87810)
    rect(g, 3 + sw, 1, 6 + sw, 2, 0xF2B01E); rect(g, 2 + sw, 3, 7 + sw, 5, 0xF2B01E); rect(g, 1 + sw, 6, 8 + sw, 7, 0xF2B01E)
    rect(g, 3 + sw, 1, 4 + sw, 5, 0xFFE27A); rect(g, 6 + sw, 3, 7 + sw, 7, 0xC98A18); rect(g, 1 + sw, 7, 8 + sw, 7, 0xB87810)
    px(g, 4 - sw // 1 + (1 if sw == 0 else 0), 8, 0x5A3A06); px(g, 5 - sw, 8, 0x5A3A06)
    if sw != 0: px(g, 4 - sw * 4 // 1 + 4 if sw < 0 else 0, 2, 0xFFFFFF)

@icon('clock', 0x1C2030)
def clock(f, g):                                                  # a white clock face, the minute hand ticks round
    disc(g, 4.5, 4.5, 3.6, 0xF2F4FA)
    for y in range(10):
        for x in range(10):
            if g[y][x] is not None and (x - 4.5) ** 2 + (y - 4.5) ** 2 > 10: g[y][x] = hx(0x7A8398)
    px(g, 4, 1, 0xE23A3A); px(g, 4, 8, 0x7A8398); px(g, 1, 4, 0x7A8398); px(g, 8, 4, 0x7A8398)
    a = f * math.pi / 3 - math.pi / 2
    line(g, 4.5, 4.5, 4.5 + math.cos(a) * 2.6, 4.5 + math.sin(a) * 2.6, 0x181820)
    line(g, 4.5, 4.5, 4.5 + 1.5, 4.5 + 0.5, 0xE23A3A); px(g, 4, 4, 0x181820)

@icon('code', None)
def code(f, g):                                                   # aqua code brackets </>, a white glint runs over them
    A, D = 0x38D2FF, 0x1A8AC0
    for x, y in ((3, 1), (2, 2), (1, 3), (1, 4), (2, 5), (3, 6), (2, 3), (2, 4)): px(g, x, y, A)
    for x, y in ((6, 1), (7, 2), (8, 3), (8, 4), (7, 5), (6, 6), (7, 3), (7, 4)): px(g, x, y, D)
    line(g, 5, 0, 4, 8, 0xE8FAFF)
    k = f * 2 - 1
    for i in range(10):
        x, y = i, k - i + 2
        if 0 <= x < 10 and 0 <= y < 10 and g[y][x] is not None and f < 5: g[y][x] = hx(0xFFFFFF)

@icon('betamark', 0x0C3A1C)
def betamark(f, g):                                               # a green beta sign with a white shine sweeping over it
    rows = ["..GGG.....", ".GgG.GG...", ".GgG.GG...", ".GGGGD....", ".GgG.GGD..", ".GgG..GD..", ".GgG.GGD..", ".GGGGD...."]
    pal = {'G': 0x3FD16A, 'g': 0x8AF0A8, 'D': 0x1E8A40}
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in pal: px(g, x, y + 1, pal[ch])
    k = f * 3 - 3
    for i in range(10):
        x, y = i, k - i + 1
        if 0 <= x < 10 and 0 <= y < 10 and g[y][x] is not None and f < 5: g[y][x] = hx(0xFFFFFF)

ORDER = ['bug', 'code', 'patron', 'betamark', 'minos', 'dice', 'chest', 'anvil', 'disc', 'rod', 'tractor', 'bell', 'clock']
BASE = 0xE100

def build(name):
    fn, edge = ICONS[name]
    strip = Image.new('RGBA', (60, 10), (0, 0, 0, 0))
    for f in range(F):
        g = new(); fn(f, g); g = outline(g, edge)
        for y in range(10):
            for x in range(10):
                if g[y][x] is not None: strip.putpixel((f * 10 + x, y), (*g[y][x], 255))
    return strip

if __name__ == '__main__':
    sheet = sys.argv[sys.argv.index('--sheet') + 1] if '--sheet' in sys.argv else None
    prov = os.path.join(RES, 'font', 'emoji.json'); data = json.load(open(prov))
    data['providers'] = [p for p in data['providers'] if not p.get('file', '').startswith('fancydrops:font/suffix_')]
    for k, name in enumerate(ORDER):
        build(name).save(os.path.join(RES, 'textures', 'font', f'suffix_{name}.png'))
        base = BASE + 0x10 * k
        data['providers'].append({'type': 'bitmap', 'file': f'fancydrops:font/suffix_{name}.png', 'ascent': 8, 'height': 9, 'chars': [''.join(chr(base + i) for i in range(6))]})
    json.dump(data, open(prov, 'w'), indent=2)
    if sheet:
        S = 8; im = Image.new('RGBA', (6 * 10 * S + 40, len(ORDER) * (10 * S + 6)), (58, 70, 96, 255))
        for k, name in enumerate(ORDER):
            st = build(name).resize((60 * S, 10 * S), Image.NEAREST); im.alpha_composite(st, (4, k * (10 * S + 6) + 3))
        im.save(sheet)
