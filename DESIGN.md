# FancyDrops design requirements

Standing rules from the author (ImTra). Read this before changing the menu, animations or drop settings, and keep it up to date. When a change is made to one page, change every other page and button it affects (for example, if the Mythological Animations page is copied to Slayers, the Slayers buttons must describe the Slayers scenes).

## Working rules
- Short UI text. Releases are manual (the author runs the "Release FancyDrops" workflow with the version).
- Per update: bump `version=` in `gradle.properties`, add a `## x.y.z` section at the top of `CHANGELOG.md`, commit and push to `main`. The version was restarted at 0.4.1 for the full rework (author request): count up from there, 0.4.2, 0.4.3, 0.4.4, 0.4.5, 0.4.6, 0.4.7, 0.4.8, 0.4.9, 0.4.10, 0.4.11 and so on.
- Commit as `tra` (`git -c user.name=tra -c user.email=imtraminecraft@gmail.com commit`).
- Make a mockup first for anything visual and big, and wait for approval before building it.
- Nothing can be compiled or run in the cloud workspace. Say so honestly. Check for duplicate methods and variable names by hand.
- Icons must be hand-pixeled, centred in their 16x16 square, animated, and unique to the thing they show. They must not look AI-made.
- The mod is free, with Patreon supporters getting extra customisation later.

## Drop Library
- The "Show" filter has four buttons: **Enabled, Normal, RNG, Disabled**. Make it obvious these are the three animation states.
  - Enabled: anything with Normal on, RNG on, or Chat Alert on.
  - Normal: only items whose animation state is Normal.
  - RNG: only items whose animation state is RNG.
  - Disabled: items whose animation state is Disabled, or whose Chat Alert is off.
- Each item row has three controls: the **animation state** button (short name: Off / Normal / RNG), the **Chat Alert** toggle, and the **Preview** button.
- Choosing Normal turns Chat Alert OFF. Choosing RNG turns Chat Alert ON. The user can then toggle Chat Alert by hand.

## Animation states
- Disabled, Normal, RNG. A fourth scene (No Drops) exists but the user cannot pick it.
- **Normal** and **No Drops**: a short scene shown above the SkyBlock action bar. The hotbar, hearts and the rest of the HUD stay visible. 3 seconds by default.
- **RNG**: the big scene. Black bars come in and the whole HUD (hotbar and everything) is hidden. 5 seconds by default.
- The user can change each duration in the Animations tab. The maximum is **7 seconds**.
- The chat alert is sent first. The message stays hidden from the player until the animation has fully played.
- If the animation text is at the bottom and the scene position is at the bottom, the text must not be covered by the SkyBlock action bar. Move the scene up a little and draw the text over the scene (so the scene does not cover the middle of the screen).
- When animations change, re-check every text and position setting so none is left broken.

## Animations tab
- The "Changes" buttons (black bars, animation text, item changes) sit at the very top.
- The Animation scenes page stays as it is. Length maximum is 7 seconds.

## Sounds
- The sound system stays. The sound waves are redrawn so they look clean.

## Chat
- The Message format editor is visible without scrolling. Previews live outside the main menu panel.

## Mythological Rituals (first finished category; Slayers, Dye, Farming, Experiments, Mining and Fishing follow)
- Mythological opens a hub with two sub pages:
  - **Mythological Mobs**: the creatures, and the Griffin Pet picker.
  - **Treasure**: the spade picker, the Erudite reforge and the treasure drops with their chances.
- Redo all icons for Mythological, and make sure each one is correct.
- Redo the Mythological scenes. Mobs keep the lightning idea. Treasure shows someone digging with a shovel until the treasure is found. As cool as possible within the rules above.
- ONLY Mythological is live. Slayers, Dye, Farming and Experiments are WIP like Mining and Fishing: a WIP label in the sidebar and the construction-site page in Drop Library, Animations, Sounds and Chat. Their code stays, it is just hidden until each one is rebuilt.
- Categories that are not finished keep the "Coming Soon" construction-site page (hazard tape, hard hats, crane, bulldozer). The author wants it kept but tidy: fixed layout on one ground line, few props, nothing overlapping, nothing interactive. (A crane-builds-then-explodes scene was an earlier idea; the author later said to keep this page.)

## Animation look (author feedback, Oct 25)
- Scenes must be properly animated, never bland: real motion, particles, light, a distinct idea per category.
- Normal and No Drops scenes are wide banners (not square), sitting right above the SkyBlock action bar with only a small gap. Not floating in the middle of the screen.
- The text keeps the earlier look (name, price, magic find lines); make it cooler where possible.

## Mythological Rituals Normal scenes (author feedback, Oct 25)
- Work on ONE part at a time (Mythological Mobs + Treasure first). Do not mock up other categories until asked.
- Built like the old Farming scene: no box. Ground patch with shadow, glows, strikes, soil glow, column of light, rings, grains of light, item springs out. Small and translucent, not square.
- Items can be 3D (blocks like gold blocks): show those rotating in 3D, flat icons stay flat.
- Mobs: a ritual circle, three lightning strikes, the minotaur is summoned, struck, shatters, item springs out. Treasure: a digger with a shovel digs three times, a chest is found and opens.
- Chat alert (to do when we reach the Chat page): lay the message out like the author's reference video: a centred block, "RARE DROP!" header in gold, the item name in its rarity colour, a small odds/source line, the price in gold, and the price source in dim grey. Every line must be customisable.

## Mythological state (Oct 25, round 2)
- Drop Library > Mythological opens a hub with two tiles: Mythological Mobs and Treasure. The sidebar lists them too.
- Per-item controls (right to left): Chat Alert, Preview, Animation state (Off / Normal / RNG). The Show filter is Enabled / Normal / RNG / Disabled.
- Normal = never the big scene. RNG = the big scene. There is no price-based auto escalation any more.
- Scenes: Normal (small, above the action bar, 3 s, text 1.5 s longer), RNG (big, bars, HUD hidden, 5 s, same story with night sky, god rays, halos, feathers), No Drops (small, one weak strike, a pale minotaur crumbles). Animation length is 2 to 7 s.
- Chat lines: `RARE DROP! You dug out a {Drop}!`, `{Random words} You dug out {amount} coins!` (any amount), `RARE DROP! {drop}` for creatures. A creature's drop plays that creature's own scene (the Minotaur has its pixel sprite, the others use their silhouettes).
- Not triggerable yet: No Drops for a real Diana kill (no known chat line). Preview and `/fd` only.

## Standing requirements added in 0.4.4
- Chat Alert follows the state: Normal off, RNG on. Rare drops default to RNG.
- HUD is hidden only by the RNG scene. Normal and No Drops keep it.
- New pages go straight to the sidebar under their category (Mythological lists Mobs and Treasure first). Mythological is always the first category.
- Choosing a cosmetic set turns off the previous set's suffix, aura and name effect.
- The Chat preview lives in its own panel above the menu, not inside it.
- No Comets style. Remove buttons and pages that do nothing, but keep the WIP pages.

## Standing requirements added in 0.4.5
- Every preview must show the CURRENT look and text of the page it belongs to. When a page is rebuilt, update every preview that reads it (dock, editors, chat, sound waves) in the same change.
- WIP pages never show a preview (animation dock, sound waves). They show the construction page. Sounds works like Animations.
- Mythological has Mobs and Treasure pages in Drop Library, Sounds and Stats. Stats lists the same categories as the Drop Library, Mythological first, with WIP tags.
- Words must stay readable: they enter with the reveal and stay at least ~2 s.
- Text position labels: "Top of animation" / "Bottom of animation".
- No releases on GitHub: the update line says "No Releases found yet."

## Standing requirements added in 0.4.6
- Scene effects (flashes, rays, rings) are drawn over the whole screen, never inside the scene's picture box.
- The words follow the animation's position: top = words above it, middle = words above or below by choice, bottom = as before.
- Mythological size: 50-200%, 100% default.

## Standing requirements added in 0.4.7
- Items are flat 2D in animations; only skull items are 3D. There is no Item Changes setting.
- Placeholders for the chat format are small buttons outside the menu, far left, never shifting the menu.
- The construction page loops: slow build, an accident, collapse, restart.
- The Active lever eases over with no bounce.
- Mythological Treasure has its own Animations and Sounds pages. Spade, Pet and Erudite live on the main Ritual page.

## Standing requirements added in 0.4.8
- Mythological is the baseline for every drop library. Slayers is next, and the existing Slayer boss identification stays untouched (a spawned boss that drops "RARE FIND!" must keep tracking).
- Each creature or source is a sidebar entry under its page.
- Bars and scene share one curve (curveR / blitRounded).
- Heavy effects are drawn in a few bands, not hundreds of fills.

## Standing requirements added in 0.4.9
- Item art comes from NEU (skull items are 3D heads that face front at the end). Every drop id must exist in the NEU repo; coins use the Total profit coin.
- The construction page: Coming Soon sits under the title, the scene is a looping story (build, accident, collapse) plus idle builders.

## Standing requirements added in 0.4.10
- /fd simulate only lists finished pages. Add a kind when its page is rebuilt.
- Skull items always show the 3D head in lists.

## Standing requirements added in 0.4.11
- The item stays up as long as the words. Its spin is sized to that time and ends facing front.

- Chat preview height is measured at the dock's real width. Dice icon is a per-pixel rotating cube. Descriptions never mention a screen position (the player picks it).
- Items turn slowly and steadily like a dropped item and end facing front. The Mythological sound bank (`myth_*`) is the default; `_big` twins play in RNG. Sounds are timed to the whole animation (words included) from one function used by the real scene, previews and the Sounds page. `/fd simulate`, Play buttons and menu use `launchMyth` / `launchTreasure`.
- Cosmetics: the page layout was approved as a mockup (try-on previews follow the hovered tile; headers show a bar that fills once). New suffixes need a 60x10 strip (six 10x10 frames) in textures/font, a provider in font/emoji.json, a Cosmetics.SUFFIXES entry and a CosmeticData.Def. New name effects live in NameFx.color.

- 2D item sprites stay flat in the animation (no turning); only 3D heads/blocks spin. Switching pages never moves the content (no slide or bounce). The Cosmetics page keeps its layout and has a Collection bar on top; the Aura preview is a lit stage with a bobbing character.

## Rewrite brief (0.4.13, author, Oct 26) - supersedes older rules where they differ
- Idea: a mini animation when a player drops something, fully customisable. Mythological first, then Slayers, Dyes, Farming, Experiments, Mining, Fishing.
- Main pages (no scrolling): Home, Drop Library, Animations, Sounds, Chat, Stats (WIP), Settings (bottom left). Keep Cosmetics, the Community buttons (top right) and the Active lever.
- Sub pages: Mythological, Slayers, Farming, Experiments, Mining, Fishing, Dyes. Sub-sub pages: the mobs of each. New ones appear in Drop Library, Animations and Sounds automatically. Unfinished ones are WIP.
- Home: description, update tracker, Resources sub page.
- Drop Library: drops sorted by source; per-item animation and chat alert; icons match NEU; skulls are 3D and animated.
- Animations: main settings (Bar, Text, Item) on top, then sub menus that open the editor. Types: No Drops, Normal, RNG, each with preview, length, sounds, position (Bottom / Center / Top Center) and size. Only RNG has cinematic bars and blur (toggleable). Previews always show the current settings.
- Sounds: main settings on top, editor with live preview below, ONE sound set for now, a clean wave drawing (no overlaying colours).
- Chat: description plus two sub pages ("Announce and Format", with the Test button, and "Chat Enabled"). Placeholders sit top left, outside the menu; a click inserts them. No Custom Drops.
- Icons are `:name:` only (no `suf_`). New animated icons with depth come later. Sparkle is the only suffix.
- Mythological animation: all three types 5-7 s (user-settable). The first 2.5-4.5 s are the same for all three, then each pivots. No Drops shows no item, text "Nothing :(" / "Better luck next time". Normal shows the item with an effect, no background. RNG has the most unique last 2.5 s, with bars. Order: item appears, background fades while text shows, item fades slowly, text fades 0.5 s later.
- Text placement: animation at Top Center or Center -> text slightly below; Bottom -> text above. RNG with bars: Top/Bottom Center put the text outside the bars; Center follows the normal rule.
- Delivery: rewrite pushed straight to main. The author syncs with `git fetch origin` then `git reset --hard origin/main`.

## Standing requirements added in 0.4.14
- Treasure is a sub page beside Mythological (not inside it). Single-group and single-source pages open their editor or drop list straight away.
- Commands: /fd, /fd help, /fd mute, /fd toggle, /fd simulate Mythological <Mob|Treasure> <Drop 1> <mf>. Nothing else.
- No tip bar on Home.
- Animation timeline lives in Ritual.java (opening = S - 2.5 s, pivot = last 2.5 s). Icon art lives in IconArt.java (16x16, shaded from the top left, outlined, up to 8 frames).
- Icons never copy another company's logo; the community buttons use generic symbols.
