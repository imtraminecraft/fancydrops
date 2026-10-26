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
- Mythological and Bosses (Slayers inside it) are live (since 0.4.37). Dye, Farming, Experiments, Mining and Fishing are WIP: a WIP label in the sidebar and the construction-site page in Drop Library, Animations, Sounds and Chat. Their code stays, it is just hidden until each one is rebuilt.
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

- Chat preview height is measured at the dock's real width. Dice icon is a per-pixel rotating cube: white die, black pips, red 1, grey-blue outline, a shadow under it, hops twice and lands on a 6 (matches the author's reference video). Descriptions never mention a screen position (the player picks it).
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
- Chat: description plus two sub pages ("Announce and Format", with the Test button, and "Chat Enabled"). Placeholders sit beside the Message format field (0.4.15). No Custom Drops.
- Icons are `:name:` only (no `suf_`). New animated icons with depth come later. Sparkle is the only suffix.
- Mythological animation: all three types 5-7 s (user-settable). The first 2.5-4.5 s are the same for all three, then each pivots. No Drops shows no item, text "Nothing :(" / "Better luck next time". Normal shows the item with an effect, no background. RNG has the most unique last 2.5 s, with bars. Order: item appears, background fades while text shows, item fades slowly, text fades 0.5 s later.
- Text placement: animation at Top Center or Center -> text slightly below; Bottom -> text above. RNG with bars: Top/Bottom Center put the text outside the bars; Center follows the normal rule.
- Delivery: rewrite pushed straight to main. The author syncs with `git fetch origin` then `git reset --hard origin/main`.

## Standing requirements added in 0.4.14
- (Reversed in 0.4.15: Treasure is a sub-sub page of Mythological again.) Single-group and single-source pages open their editor or drop list straight away.
- Commands: /fd, /fd help, /fd mute, /fd toggle, /fd simulate Mythological <Mob|Treasure> <Drop 1> <mf>. Nothing else.
- No tip bar on Home.
- Animation timeline lives in Ritual.java (opening = S - 2.5 s, pivot = last 2.5 s). Icon art lives in IconArt.java (16x16, shaded from the top left, outlined, up to 8 frames).
- Icons never copy another company's logo; the community buttons use generic symbols.

## Standing requirements added in 0.4.15
- Mythological is ONE sub page with two sub-sub pages: Mythological Mobs (pet + creatures) and Treasure (spade + Erudite + drops). It reverses the 0.4.14 split. Treasure and Mobs are also the two animation groups of the Animations and Sounds pages.
- The Drop Library has no per-page enable switch. Defaults on first start: every drop Normal, rare drops (rank 2+) RNG; Normal = chat alert OFF, RNG = chat alert ON, set at once when the state button is pressed.
- Animation size: the scene is drawn at 0.75 x the Size slider, so 100% is 25% smaller than in 0.4.14; the slider ends at 120%. After the pivot the scene lasts 3.2 s (Ritual.TAIL).
- Skulls are real 3D blocks that turn smoothly (SkullSpin). Coin icons use the Hypixel coin head.
- Animations and Chat > Message & Channel must not need scrolling. Test is on the Chat landing page; placeholder buttons sit beside the Message format field (not outside the menu).
- Chat Enabled lists each sub page and creature with a switch per drop. "Also post in public chat" lights Public in the channel picker.
- Every on/off is the yellow/grey ON/OFF button.
- Sounds: Play plays only the sounds; the live preview shows a time ruler, one lane per sound with its length, a playhead and the clock.
- Animated menu off: all decoration stands still except icons (icons follow Animated icons).
- No Menu Font setting.

## 0.4.16 additions
- Mythological: one animation shape per creature (Ritual.KINDS / shape()); Animations page keeps a single "Mythological Mobs" group. Treasure uses the digger (Ritual.digger) with a chest for Normal and RNG.
- Creature icons are `mob_<id>` in IconArt.mob(); they appear on the creature tiles and under Mythological Mobs in the sidebar.
- Cosmetics has no search box. Name effects are only the Special sets and Beta (Pulse). Hidden (scrolled-out) tiles are not hoverable.
- Where to edit: icons in IconArt.java, scenes in Ritual.java, timing/cues in AnimationManager.java, item art in ItemIcons.java / SkullSpin.java, defaults in FancyConfig.java.

## 0.4.17
- Item art lives in config/fancydrops/textures/ (<ID>.png flat, <ID>_head.png skin); user files override downloaded art. Creature icons `mob_<id>` draw the creature skull in 3D (ItemIcons MOB_SKINS).

## 0.4.18
- Main (landing) pages must never scroll: build() shrinks tile heights, then hides the intro text, then clips.

## 0.4.19
- Animation 100% = Ritual.SMALLER 0.6 (about a third of the screen width). The inhale pillar is short.

## 0.4.20
- Icons are overridable at runtime from config/fancydrops/icons/<name>.png (Icons.scanOverrides, hot reload). Add new names to Icons.EXPORT.

## 0.4.21
- The sidebar squeezes (FancyScreen.sq, 0.7 to 1) instead of scrolling. Emblems: `emblem` (tinted shield + star) and `emblem_special`.

## 0.4.22
- Icons have 12 frames (IconArt.FRAMES). The sidebar is not squeezed (sq stays 1); main pages never scroll.

## 0.4.25
- "Animation Changes" has Bar, Text, Item and Position Changes. Position rules: Top Center = words below only, Center = above or below, Above Action Bar = words above only (FancyConfig.textAbove()).
- Chat format limit: 65 letters, placeholders not counted.
- Source art files stay in src/main/java/dev/fancydrops (the IDE needs package = folder; see ART.md); runtime art in config/fancydrops/art/{icons,heads,items,animations} (DataFiles.art). All heads turn in sync.

## Standing requirements added in 0.4.28
- Page headers show the title only: no line of words under it.
- Item changes sliders (spin, glow, float) and toggles must change what the animation draws; the Item preview uses a 3D head so spin shows. Bar "See-through" 0% is solid. Animation Size is 50-150%.

## Standing requirements added in 0.4.29
- The back button lives in the page header (under the title), never as a row in the list.
- A button inside a search result must work like on its own page: it moves to that page and clears the search.
- Heads are traced off the render thread. Animation frames are real PNGs in art/animations; an edited frame replaces the drawn one.

## Standing requirements added in 0.4.30
- Animation size is one global setting (Animations > Animation Changes), shown 75-125%. Categories have no size.
- Item Changes is a single page. Floating off hides Float height. Bar pillars are as thick as the bars.
- Every sub page has a header back button to its main page; every tab with sub pages has a main page (Settings too).
- Exported icon and animation PNGs never overwrite a file the author edited, but refresh when untouched (original.txt lists what the mod wrote).

## Standing requirements added in 0.4.31
- Animations and Sounds main pages show their description, one row that opens the shared settings (Animation Changes / Sound Settings) and the category tiles. The settings live on their own sub page so the main page never scrolls.

## Standing requirements added in 0.4.34
- The construction page loop (14 s): stand, tremble, collapse with dust, foreman reacts, the crane lifts the pile in three loads and rebuilds, window lights, restart. Nothing floats: every hanging thing hangs from the crane or a lamp. Keep the title and Coming Soon tag as they are.
- Depth layers on the construction page: far skyline and cranes, unfinished buildings with tiny workers, fog, a dark foreground edge. Several lamp posts.

## Standing requirements added in 0.4.35
- The Coming Soon tag on the construction page is a hanging marquee board (chains from the top tape, chasing bulbs, glow, flicker, shine) with two builders on it (hammer on top, welder underneath). No road signs: the author found them ugly. No ladder worker.

## Standing requirements added in 0.4.36
- The construction scene lives in Site.java, a port of the approved mockup in its own 421 x 169 grid (ground at y 150) that the page shifts onto the panel's ground line. When the mockup changes, change Site.java the same way. Builders are 12 px tall. No road signs at the top corners.

## Standing requirements added in 0.4.37
- The main page is **Bosses** (id `bosses`), with one sub page **Slayers** (group `slayer`, types No Drops, Normal, RNG; keys `slayer.nodrops|normal|rng`) and the six bosses as sub sub pages. Future boss types (Rift, Nether...) are new groups of Bosses. Chat has one message for Bosses.
- Slayer data lives in `slayers.json` (same shapes as `mythological.json`, loaded by `SlayerData`). Drop ranks come from the old odds names (Occasional 0, Rare 1, Extraordinary 2, Pray RNGesus / RNGesus Incarnate 3); rank 2 and up default to RNG.
- The slayer scene is `SlayerScene` (112 x 96, software 3D, Sword style only, the Spirit style is gone) drawn by `Ritual.paintSlayer` into the shared 320 x 180 canvas. Its clock is warped so its three slices fill the opening and its tail fills TAIL: all three types pivot at the same moment as every other category. Ritual draws no item for slayers; the scene draws it with the Item Changes settings.
- Bars, blur, flash, words and the HUD hiding come from Ritual like every other category. The boss decides the look (zombie, spider, wolf, enderman, blaze, vampire); Tarantula Broodfather has extra background effects, the others have depth layers (Depth.java).
- The slayer sound bank is `slay_*` (slash, cleave, reveal, reveal_epic, glow, end). Slice cues sit at 0.536, 0.753 and 0.943 of the opening.
- Every page of Slayers must describe slayer scenes (text on the Animations page says a sword slices the boss).

## Standing requirements added in 0.4.38
- Every animation is 5 seconds by default. The slayer scene is as wide as the Mythological circle (scale 1.9 x size). Its light flashes sit on the sword slices (0.536, 0.753, 0.943 of the opening).
- WIP pages show the page's icon and name on a hanging sign at the top right (the Coming Soon board stays centred). Boss icons are `:boss_<id>:`, drawn by IconArt, never wiki pictures.
- Menu previews repaint the scene at about 30 fps (`AnimationManager.previewing`).

