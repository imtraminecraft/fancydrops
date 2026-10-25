#!/usr/bin/env python3
"""Rebuilds the chat/tab suffix sprites (textures/gui/sprites/suffix/<id>.png, 10x60, six frames) from the strips
(textures/font/suffix_<id>.png, 60x10). Run it after changing a strip, so chat, the tab list and the menu all show the same picture."""
import os, re
from PIL import Image
root = os.path.join(os.path.dirname(__file__), '..', 'src', 'main')
res = os.path.join(root, 'resources', 'assets', 'fancydrops', 'textures')
ids = re.findall(r'new Suffix\("([a-z0-9_]+)"', open(os.path.join(root, 'java', 'dev', 'fancydrops', 'Cosmetics.java')).read())
os.makedirs(os.path.join(res, 'gui', 'sprites', 'suffix'), exist_ok=True)
for i in ids:
    p = os.path.join(res, 'font', f'suffix_{i}.png')
    if not os.path.exists(p) and i == 'sparkle': p = os.path.join(res, 'font', 'sparkle.png')
    if not os.path.exists(p): print('no strip for', i); continue
    st = Image.open(p).convert('RGBA'); out = Image.new('RGBA', (10, 60))
    for f in range(6): out.paste(st.crop((f * 10, 0, f * 10 + 10, 10)), (0, f * 10))
    dst = os.path.join(res, 'gui', 'sprites', 'suffix', f'{i}.png'); out.save(dst); open(dst + '.mcmeta', 'w').write('{"animation": {"frametime": 2}}')
