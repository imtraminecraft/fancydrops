#!/usr/bin/env python3
"""Draws the Sparkle suffix strip (textures/font/sparkle.png, 60x10, six 10x10 frames): a faceted four-point star with lit and shaded
facets, a bright core, an edge, a drop shadow and a tiny orbiting glint. Run tools/make_suffix_sprites.py afterwards."""
import os, math
from PIL import Image
out = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fancydrops', 'textures', 'font', 'sparkle.png')
RAD = [3.6, 4.1, 4.8, 4.3, 3.8, 3.4]
TOP, LEFT, RIGHT, BOT = (255, 247, 200), (255, 226, 120), (240, 176, 60), (196, 120, 40)
EDGE, CORE = (150, 84, 30), (255, 255, 255)
GLINT = [(1, 1), (8, 1), (8, 8), (1, 8), (1, 1), (8, 1)]
GCOL = [(120, 220, 255), (255, 130, 200)]
def inside(dx, dy, r, p=0.72): return abs(dx) ** p + abs(dy) ** p <= r ** p
img = Image.new('RGBA', (60, 10), (0, 0, 0, 0))
for f in range(6):
    ox = f * 10; r = RAD[f]; cx = cy = 4.5
    def px(x, y, c, a=255):
        if 0 <= x < 10 and 0 <= y < 10: img.putpixel((ox + x, y), (*c, a))
    for y in range(10):                                   # shadow first, one pixel down-right, so the star floats above it
        for x in range(10):
            if inside(x - cx - 1, y - cy - 1, r) and not inside(x - cx, y - cy, r): px(x, y, (40, 24, 10), 120)
    for y in range(10):
        for x in range(10):
            dx, dy = x - cx, y - cy
            if not inside(dx, dy, r): continue
            edge = not (inside(dx - 1, dy, r) and inside(dx + 1, dy, r) and inside(dx, dy - 1, r) and inside(dx, dy + 1, r))
            if abs(dx) < 1 and abs(dy) < 1: c = CORE
            elif edge: c = EDGE if (dx + dy) > 0 else (214, 150, 52)
            elif abs(dy) > abs(dx): c = TOP if dy < 0 else BOT
            else: c = LEFT if dx < 0 else RIGHT
            px(x, y, c)
    gx, gy = GLINT[f]; px(gx, gy, GCOL[f % 2])           # a tiny glint that hops round the star
img.save(out)
