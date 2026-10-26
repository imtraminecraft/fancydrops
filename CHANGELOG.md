## 0.5.0
- Farming is live, with three pages: Rare Crops, Harvest Feast and Pest Drops. Each has a Normal and an RNG animation (the old farming scenes, redrawn with more frames: the plant grows and blooms, the pest pops out of the soil), its own sounds, and the old drop lists and chat detection (RARE CROP!, Overbloom).
- Experiments is live: one Experimentation Table page with a Normal and an RNG animation. A book opens, its pages flip with more frames, and the rewards rise from it.
- Two item drops: Slayers and Experiments can show two items side by side, with stacked compact text. Slayer twin drops are paired automatically; Experiment rewards come two per book.
- /fd simulate takes a second drop for Slayer and Experiments, and has new Farming and Experiments commands.
- Drop Library, Animations, Sounds and Chat have pages for all four new groups; the editor Play button has a "2 items" preview for Slayers and Experiments.
- 13 new sounds (scythe, heave, bloom, vacuum, gulp, page flip, book open, rune chord...) and 4 new animated icons.
- Fixed: playing a preview of a drop set to Off crashed.

## 0.4.59
- The moon's glow may spill past the frame again (the unique look); the trees and the rest of the scenery stay inside.

## 0.4.58
- Slayer places: the scenery (the Revenant's moon glow, trees and so on) may spill only 4 px above the frame and 2 px at its sides, and never below it.

## 0.4.57
- Slayer places for the Sven Packmaster, Voidgloom Seraph and Riftstalker Bloodfiend are built around the middle: the moon or sun sits right behind the boss, and the trees, pillars, columns and islands are mirrored on both sides.

## 0.4.56
- Slayer scene: the boss now stands before the first cut instead of after it.
- The panel is see-through, smaller, and has rounded corners; trees, towers and stones in front of it break out over its frame. The Inferno fortress stays inside the panel.

## 0.4.55
- Cleanup: removed the unused "cube" icon, 15 methods and 7 fields nothing used any more, and the Outline and 3D items settings (their code, config fields and editor state). Every sound, texture and font in the mod is referenced, so none were deleted.

## 0.4.54
- Slayer animation: no runes, rings or circle any more. Each boss is a hologram standing in its own place on an opaque panel, with a far and a near layer for depth: a graveyard of grass, dirt and gravestones for the Revenant Horror, a web-hung cave for the Tarantula Broodfather, a snowy pine forest for the Sven Packmaster, the End for the Voidgloom Seraph, the Nether for the Inferno Demonlord and gothic ruins for the Riftstalker Bloodfiend.
- The Inferno blaze skull is looked up under every place the game has kept the picture.

## 0.4.53
- Search: the left menu now lists every Drop Library page that holds a match (the category, and under it the creature, boss or group page), highlighted. Clicking one opens that exact page.

## 0.4.52
- Slayer scene rebuilt on an opaque, boss-coloured panel the same size as the Mythological scene. The three cuts split the hologram into pieces that drift apart along each cut line, then shatter. The cuts land on the same beats as before, so the sounds still match.
- Zombie and blaze skulls: the first drawn heads were saved to your icons folder and kept being reused. They now use a new id and load the real zombie and blaze textures. The drawn stand-in is never saved.
- Position Changes: the locked and open buttons were swapped. Top Center locks "Above the animation", Above Action Bar locks "Below the animation".
- Bar Changes preview: removed the faint band across the ground that showed as horizontal lines.
- Chat > Chat Enabled: the Mythological and Slayer rows use the same creature and boss icons as the Drop Library.

## 0.4.51
- Bar Changes > Soft edge is a smooth fade now (the four visible lines are gone).
- Position Changes > Top Center puts the top of the animation at the very top of the screen, or just under the top cinematic bar when the bars are on.
- The words follow the position: Top Center = below only, Center = either, Above Action Bar = above only. The blocked button shows its pop-up.

## 0.4.50
- Revenant Horror and Inferno Demonlord show the creatures' own heads, read from the game's zombie and blaze pictures, as spinning 3D skulls.
- Slayer animation: three cuts, not four. Each cut now opens the hologram along its line, and the gap widens after the blade passes. The lightning at the end of "No Drops" is gone for slayers.
- Item Changes: the Outline and 3D items buttons are removed (items are always 3D).
- Glints now work: little four-point sparks twinkle in and out around the dropped item.

## 0.4.49
- The page title at the top of the menu stays in the same place on main pages and sub pages (main pages moved up to match).

## 0.4.48
- The Tracked Drops counter is a small chip at the top right of the Drop Library banner (number and label on one line).
- The Animations, Sounds, Chat and Settings main pages now open with the same banner as the Drop Library, followed by their tiles, so the main pages look alike.

## 0.4.47
- The hotbar and hearts now slide away only when the cinematic bars slide in, and come back when they leave (they used to go as soon as the animation started).

## 0.4.46
- The Chat main page no longer has the "Your message" row.
- The Chat Enabled tile reads "79 / 132 drops are live".
- Clicking a result in the side menu while searching now fully resets the search box (no stray caret, no focus left behind).

## 0.4.45
- Revenant Horror and Inferno Demonlord now show real spinning 3D heads: a green zombie head and a yellow blaze head.
- Slayer animation: the ground no longer cracks. The boss hologram is slashed instead, and the cuts stay on it until it shatters.
- Every slayer drop slashes at different angles, positions and lengths.
- The Drop Library shows a "Tracked Drops" counter at the top right.
- Scrolling text on long tiles moves in sync: every line pans for the same time, eased, with the same pause.

## 0.4.44
- Fixed a compile error in the Animations and Sounds pages (the WIP tag on the sub page list).

## 0.4.43
- The Spade row on the Treasure page shows the spade you chose (Ancestral, Archaic or Deific) as its own picture.
- The Erudite reforge icon is an anvil being struck by a hammer, sparks flying.
- The Treasure icon is a spade in the ground beside the gold it dug up, a glint running over it.
- Two unused icons (the iron sword and the cone) are removed.

## 0.4.42
- The cinematic bars, side pillars and rounded corners are no longer tinted by the drop's colour (that was the brown/gold overlay at the screen edges). They are always the neutral bar colour, or the custom one you pick.

## 0.4.41
- Names and small lines that are too long for a tile (Tarantula Broodfather, Riftstalker Bloodfiend...) now slide left, wait, then slide back instead of being cut off.

## 0.4.40
- Revenant Horror's icon is Minecraft's zombie head.
- The Rift and Crimson Isle show a WIP tag in the left menu; the Animations and Sounds page tiles match the Drop Library's.
- Main page buttons now count pages, sub pages and items (drops, animations or sound sets).
- Mythological Mobs tiles are coloured by the Griffin pet rarity each creature first spawns for (Mythic ones are purple).
- Items in animations are 50% bigger (the new 100%).
- Menu previews: the landscape behind them is painted once into a texture instead of hundreds of rectangles every frame.

## 0.4.39
- The slayer animations are rebuilt on the Mythological ritual: the same ground circle and strike beats, with the boss in silhouette (zombie, spider, wolf, enderman, blaze, vampire) and a sword slashing it three times before it shatters and the drop appears. No backdrop, so the game behind stays visible. Same size as the Mythological ones.
- Boss icons are skulls (zombie head, spider, wolf, enderman, blaze and bat pet heads).
- Bosses now has three sub pages: Slayers, The Rift and Crimson Isle (the last two are WIP) in Drop Library, Animations and Sounds, with new animated icons.
- WIP pages show only the icon and page name on the hanging sign.

## 0.4.38
- The Slayers main page is now **Bosses**, with **Slayers** as a sub page and the six slayer bosses as sub sub pages. Animations, Sounds and Chat follow it (Chat has one "Bosses" message). More boss types (Rift, Nether...) can be added later as more sub pages.
- Slayer boss icons are drawn by the mod (they showed wrong wiki pictures before).
- The slayer scene is now the same size as the Mythological ones. The light flashes when each sword slice lands (the last one biggest) instead of after.
- The construction page shows the page's icon and name on a small sign hanging from the top, at the right.
- Menu previews repaint at about 30 fps and the edited-frames folder is checked every 5 s instead of every second, to cut the lag.
- Every animation is 5 seconds by default (existing settings are reset to 5 s once).

## 0.4.37
- Slayers is live (no more construction page) in Drop Library, Animations, Sounds and Chat. The six bosses (Revenant Horror, Tarantula Broodfather, Sven Packmaster, Voidgloom Seraph, Inferno Demonlord, Riftstalker Bloodfiend) list their rare drops, each with its animation state, Chat Alert and Play button.
- Slayer animations: No Drops, Normal and RNG share the same opening (a sword slices the boss three times) and pivot together with the other categories, then the drop springs out. RNG has cinematic bars and blur and hides the hotbar. The scene's look follows the boss.
- Slayer sounds: the slayer bank (slash, cleave, reveal, ember hum, farewell) is back, with Minecraft-sound defaults for the other sound set. Each slot is editable per type on the Sounds page.
- Chat: Slayers has its own message and its own Chat Enabled list. `/fd simulate Slayer <Boss> <Drop|Nothing> <mf>` plays a pretend slayer drop.
- Six new hand-pixeled, animated boss icons.

## 0.4.36
- The construction page now matches the mockup exactly: the same small builders, the finer brick house, the crane that rebuilds it, the unfinished buildings and their tiny workers, lamps, far cranes and fog. The old oversized builders and the corner road signs are gone.
- The Coming Soon board no longer hides a letter (the flicker is gone) and its two builders are the small ones.

## 0.4.35
- The "Coming Soon" tag is now a hanging marquee board: it swings from two chains under the top tape, bulbs chase round its double gold frame, the words glow with one flickering letter and a shine sweeps across. A builder hammers on top (with a paint tin) and a welder works from a plank hung underneath.
- A lamp post moved so it does not stand under the board.

## 0.4.34
- The "Coming Soon" construction site is redone: the house trembles, a loose brick falls, then it collapses in a cloud of dust with a small shake. The foreman panics and shrugs, then the crane lifts the pile in three loads and rebuilds the house row by row until the window lights up. The loop is 14 s.
- More lamp posts along the site, a work light on the crane jib, unfinished steel-frame buildings with tiny workers (hammering, walking planks, welding), far cranes with blinking lights, a water tank, fence, fog and darker edges for depth.
- The title and the "Coming Soon" tag are unchanged.

## 0.4.33
- The dice icon is redone from your video: a white die with black pips (a red 1) hops, tumbles in the air as a real turning cube, lands on a 6 and shines, with a shadow under it.

## 0.4.32
- Animation Changes and Sound Settings use the Settings icon.
- Chat main page: "Your message" shows the whole message (it wraps instead of being cut off).
- The blue line on the left of the category and tile boxes is gone.

## 0.4.31
- Animations and Sounds main pages: the description is back and nothing scrolls. Animation Changes (with the size slider) and Sound Settings (switch, volume, sound set) each open as their own sub page, with a back button.
- The category tiles no longer get squeezed until their words overlap.
- Less lag in the scene drawing (glows and the creature's break-up used a slow square root).

## 0.4.30
- Animations: one Animation size slider under Animation Changes (75% to 125%, the old 50% is now 75%). The Size row is gone from every category.
- Position Changes: the Above/Below buttons now match what you click (they were swapped).
- Text Changes: Exit effects work. Eleven effects (Twinkle, Fade away, Fly up, Shrink, Scatter, Slide in, Flip in, Swing, Gold, Slide out, Melt) were never saved before.
- Bar Changes: Curved corners round the inner corners of the picture. Side pillars are exactly as thick as the bars.
- Item Changes: one page. Floating (Float height hides when it is off), Outline (a rim in the drop's colour), Glints, 3D items and Spin back sit under the sliders.
- Every sub page has a back button to its main page, in the header.
- Settings opens on its own main page with four sub pages (Menu, Cosmetics, Prices, Drop Data).
- Sounds and Chat have new descriptions; the page description now stays unless there is truly no room.
- Community icons really move now (chatting bubbles, a beating heart, a commit travelling up the branch).
- Icons you have not edited are refreshed from the mod when their drawing changes; ones you edited are kept; older exports move to icons/old.
- A fresh /fd opens the Home page, not Resources.

## 0.4.29
- Less lag: the 3D heads are now ray-traced on a background thread (96 steps a turn), so a page of spinning heads no longer stalls the game.
- Animation frames: every scene is written to `config/fancydrops/art/animations` as PNG frames the first time the menu opens. Edit a frame and the mod plays yours.
- Search: buttons inside a search result (the category tiles) now open their page and close the search, instead of doing nothing.
- The back button moved up into the page header.
- Sounds timeline: block text is coloured, and a lane with no sound picked says "none for this animation".
- New short texts for Drop Library, Mythological, Mythological Mobs ("x Mobs • x Drops listed") and Treasure.
- Settings: the Layout tiles are tall enough for their two lines.

## 0.4.28
- Item changes: Spin speed, Spin the other way and Glow now work (the preview shows a 3D head, so you can see them). Glow is a soft light behind the item.
- Bar changes: the opacity slider is now "See-through" (0% solid, higher lets the picture show). Side pillars are back (off by default).
- Text changes: the preview is much bigger and centred.
- Animation size: 50% to 150% (100% is the standard size), with a clearer description.
- Sounds: the timeline preview is cleaner (clock moved out of the ruler, second lines, bigger icons, lit blocks). The sound rows use their own icons.
- Chat: the landing page shows the channel, how many drops announce, and your current message.
- Removed the line of words under every page title.

## 0.4.27
- Fix: the Position/Bar/Text/Item editors are solid now, so the page behind no longer shows through as ghost text.

## 0.4.26
- Fix: the art source files are back in the normal package folder, so the editor resolves `Icons` and the others again. `ART.md` lists which file draws what.

## 0.4.25
- **Animation Changes** (was "Looks for every animation") now has four buttons: Bar, Text, Item and **Position Changes**. The separate Position row is gone. Position Changes shows a still picture of a screen (action bar, crosshair, the animation and its words) and has two settings: Animation Position (Top Center, Center, Above Action Bar) and Text Position (Above / Below). Top Center: words only below. Center: either. Above Action Bar: words only above.
- Bar Changes previews only the bars. Item Changes previews only the floating item (bigger, no scene). Text Changes shows the words large, and the exit effects now play in the middle of the loop so you can see them.
- Chat message limit is 65 letters; the {placeholders} do not count.
- Griffin pet: the text says "Creatures it can't call are hidden" and the icon is the pet's own head. The community buttons (Discord, Patreon, GitHub) animate.
- All 3D heads now turn in step.
- Art gathered in the game folder `config/fancydrops/art/{icons,heads,items,animations}`; `ART.md` lists which source file draws what. Older `icons/` and `textures/` folders are moved there on first run.
- Removed unused code and files (Tex.java, active_on.png, several dead methods and fields).

## 0.4.24
- The menu stays the same size and place on every page. It only moves when a live preview slides in above or below it (the Chat page used to nudge it because it reserved less room for its preview).
- Hidden intro rows on main pages are removed, so they no longer draw over the tiles.
- Icon animations have 16 frames (was 12) at 70 ms. Older exports in `config/fancydrops/icons/` are moved to `icons/old/` (named like `dice_12f.png`) and made again, 256 px tall.

## 0.4.23
- The Spade setting has a shovel icon (blade in the spade tier colour, dips into the ground and flicks up dirt) instead of a sword.

## 0.4.22
- The sidebar is back to how it was (it scrolls on its own). Only the main pages must not scroll.
- Main pages that were too tall now hide the repeated title row first, then make tiles a little shorter (never below a size where their text fits).
- Icon animations have 12 frames (was 8) at 90 ms each. Old 8-frame exports in `config/fancydrops/icons/` are moved to `icons/old/` and made again with 12 frames (192 px tall).

## 0.4.21
- The sidebar no longer scrolls: when a page's sub pages make it too tall, every row gets a little shorter until it fits (Chat and Stats stay visible).
- Main pages (Animations, Sounds...) no longer end up shifted or cut off after scrolling.
- New set emblems: a clean symmetric shield with a centred star, plus a separate Special emblem with a four-point sparkle.

## 0.4.20
- Editable icons: every `:name:` icon is saved once to `config/fancydrops/icons/<name>.png` (16 px wide, 128 tall = the 8 animation frames stacked). Edit and save a file and the game updates it within a second. A 16 x 16 file makes a still icon.

## 0.4.19
- Animations are smaller again: 100% is now 0.6 of the old base size (about 1/3 of the screen width), and the pillar of light stops well below the top of the screen.
- The creature page header shows the creature's own head, not its first drop.

## 0.4.18
- Main pages (Animations, Sounds and the other landing pages) never scroll: their tiles shrink a little until everything fits.
- Every item picture is loaded once when the menu opens, so all of them are in `config/fancydrops/textures/` (before, only the ones you had looked at).

## 0.4.17
- Fixed the 3D head blur: the head texture is now 128 x 128 (was 64) so it stays sharp when big.
- New folder `config/fancydrops/textures/`: every item picture is saved there as `<ITEM_ID>.png` (flat art, any size) and head items as `<ITEM_ID>_head.png` (the skin). Put your own PNG with the same name there to replace it; it loads the next time the item loads.
- The Mythological creature icons are now each creature's own 3D head (Hypixel bestiary skins), with the pixel icon shown until it loads.
- Head skins with missing base64 padding no longer fail to load.

## 0.4.16
- Mythological animations: every creature has its own shape now (Minotaur, Minos Hunter, Siamese Lynxes as two cats, Stranded Nymph, Cretan Bull, Harpy, Gaia Construct, Minos Champion, Sphinx, Minos Inquisitor, Manticore, King Minos). On the Animations page they stay ONE "Mythological Mobs" group; its preview cycles through all twelve.
- Treasure animation remade: a digger in a straw hat with a proper spade (wind up, drive in, prise, lift, fling) onto a growing pile, a hole that deepens, and a chest that rises. Normal treasure shows the chest too.
- 12 new creature icons (`mob_<id>`) on the creature tiles and as a sub-sub menu under Mythological Mobs in the sidebar.
- Cosmetics: no search box on this page; clicking outside the search box (any page) leaves it so typing no longer goes into it; tiles hidden by the scroll clip can no longer be hovered; the set emblem star is centred in the shield; every name effect is gone except the Special and Beta ones.
- Home: removed the "Mythological Rituals come first, Slayers next" line.
- Nothing here is compiled in the cloud; 0 own-code errors in the checker.

## 0.4.15
- Mythological is one sub page again: Drop Library > Mythological opens two sub-sub pages, **Mythological Mobs** (the Griffin pet and the creatures) and **Treasure** (spade, Erudite, treasure drops). The "Diana's Mythological Ritual:" and "Chances for this pet" texts are gone.
- Drop Library: the "switch off to ignore this page" row is gone. Every drop starts Normal (chat alert off); rare drops start RNG (chat alert on). Choosing Normal turns the chat alert off at once, RNG turns it on (also on the "All drops" button).
- Animations are 25% smaller: that is the new 100%. The Size slider goes 50% to 120%. Default length is 7 s; the item and the words stay visible about 1.5 s longer.
- Skulls turn smoothly now: a real 3D head block, ray-traced at 64 x 64 with soft edges, 144 steps per turn. Wiki pictures are averaged down instead of dropped to 16 pixels. The Coins icon is the Hypixel coin head (the wiki "Coins" page, then NEU), with the drawn coin only as an offline fallback.
- The Bar, Text and Item Changes previews work again (the bars preview holds the scene after the drop appears, the text preview loops the words on their own clock, the item preview holds the scene while the item keeps bobbing).
- Chat: Test is on the main Chat page. The placeholder buttons sit right beside the Message format field. "Also post in public chat" lights Public in the channel picker and shows in the Announcing line. Chat Enabled lists every sub page and creature with a switch for each drop.
- Animations and Chat > Message & Channel fit without scrolling. The category tiles are tall tiles with the whole name (no more "Mythologic"), and the WIP tag is a small corner tag.
- Every on/off is the yellow and grey ON/OFF button.
- Sounds: Back works again, Play plays only the sounds at their real times, and the live preview shows a time ruler with a lane for every sound, a block as long as the sound, a moving playhead and the clock. Five new sound icons (Beat, Burst, Reveal, Linger, End).
- New icons: clapperboard with a swinging striped clapper, digging spade, iron sword (Spade picker), thin sword (Slayers), a farmer hoeing (Farming), the enchanting-table book flipping (Experiments), a pickaxe cracking a rock (Mining), a paint drop splashing into a puddle (Dyes), a die rolling a 6 (RNG), a loot bag (Normal), a bookshelf (Drop Library), set emblems (the Cosmetics tiles that showed "?").
- Settings: Menu Font is gone. With Animated menu off, everything moves no more except icons. Sliders no longer glide in from another page's knob, and the Cosmetics speed, amount and size sliders put 100% in the middle.

## 0.4.14
- New Mythological animations, all three types, 5 to 7 seconds. The first part is the same for No Drops, Normal and RNG; the last 2.5 seconds are each their own.
  - Mobs: a ritual circle draws itself, three lightning strikes crack the ground, a minotaur is summoned, then the power is drawn in.
  - Treasure: a spade digs three times, the hole glows, then the light is drawn in.
  - No Drops: the charge fizzles, the circle and the creature crumble to ash (a bone pops out of an empty burrow), grey words.
  - Normal: the creature shatters into light, rings lift the item out of the ground, sparkles circle it. No backdrop.
  - RNG: white flash, shockwaves, god rays, lightning round the item, runes, a fountain of light, a chest for treasure, cinematic bars and blur.
  - For all three the item appears first, the background fades while the words show, and at the end the item fades slowly with the words 0.5 s later.
  - The words sit below the animation (Top and Center) or above it (Bottom), and stay clear of the bars.
  - Sounds are cued from the same timeline (beats for every strike or dig, burst, reveal, linger, end).
- New icon set: every `:name:` icon is shaded pixel art with depth and a few frames of animation. The 25 text-effect tiles show the letter A doing what the effect does. Community buttons use plain symbols instead of other companies' logos.
- Treasure is its own sub page beside Mythological (its own chat switch, spade and Erudite settings on its page).
- Commands are now only `/fd`, `/fd help`, `/fd mute`, `/fd toggle` and `/fd simulate Mythological <Mob|Treasure> <Drop> <mf>`.
- Removed the tip bar from the top of Home.

## 0.4.13
- Full rewrite from the ground up, step 1. The menu look and shell are kept; the pages are new: Home, Drop Library, Animations, Sounds, Chat, Stats (WIP) and Settings (bottom left).
- Sub pages are Mythological, Slayers, Farming, Experiments, Mining, Fishing and Dyes; the creatures of each are sub-sub pages. New ones show up in Drop Library, Animations and Sounds by themselves. Everything except Mythological shows the WIP page.
- Every category has three animations (No Drops, Normal, RNG). Drops pick one of them, or Off. Price no longer pushes a drop to RNG on its own, and the "RNG Drop scene" setting is gone.
- Icons: one kind only, written `:name:`. All old icons and the pixel-art code are deleted; new animated ones come later. Sparkle is the only suffix left, redrawn with depth.
- Removed: Custom Drops, the old Slayer, Dye, Farming and Experiment data, kill and profit trackers, the old scenes, the old sounds and the tests. They stay in git history (commit 3b05554).
- Not compiled or played yet: the animation timeline (5-7 s, shared opening, new pivots) comes in the next step.

## 0.4.12
- Chat preview: no more empty space, the box hugs its messages with a little room above and below.
- RNG dice icon is now a real 3D die that tumbles, hops and settles.
- 3D head icons have depth: brighter top, darker side, a dark outline.
- Removed "above the action bar" from the animation descriptions (you choose the position).
- Items now move like a dropped item: a slow, steady turn that eases in, glides and settles facing front, with a soft bob.
- New Mythological sounds (Bolt, Shatter, Reveal, Glow, Farewell, Dig, Chest, Fizzle, plus bigger twins for RNG). Each fades to silence by itself and the last one is timed to finish with the words.
- Sounds follow the whole animation (words included), in the real scene, the previews and the Sounds page. The Sounds page plays the same pitches as the real scene.
- /fd simulate, the Play buttons, the menu and the previews all start drops through the same two functions (creature and treasure). Simulated treasure now follows the drop's own Off / Normal / RNG state.
- 2D item sprites stay flat and face you (they no longer turn); 3D heads and blocks are the ones that spin, with stronger light and shade. Heads that fail to load are now written to icon-log.txt.
- Home hero shows its words again. Tips, page descriptions, mod description and README now describe the current mod (Mythological live, the rest being rebuilt).
- Cosmetics: 8 new animated suffix icons (Lucky Dice, Shovel, Skull, Moon, Comet earned by rare drops; Watcher, Rocket, Cat for the early players) and 5 new name effects (Rainbow, Aurora for the early players; Ember, Frost, Neon earned by rare drops). Each page header has a bar that fills once when the page opens.
- Construction page: the worker tosses each brick up to the wall from a pile instead of the crane lowering it.
- Chat and tab-list suffixes used old sprite files that no longer matched the menu (that was the Discord egg). Every suffix sprite is rebuilt from its strip (tools/make_suffix_sprites.py), menu icons are drawn from the same strip and centred in the icon box, and a startup check logs any sprite that differs. New Ghost and Lucky Dice art (they looked like eggs).
- Duplicate icon names: a hand-pixeled picture always won over a code-drawn one of the same name, so the new Dye palette never showed. The old animated Dye picture is removed. The RNG icon is the tumbling die. The Discord community button uses the Discord suffix picture.
- Griffin Feather (and Braided) show a feather in the animation, not the Griffin pet.
- Mythological lightning: Normal strikes 3 times and RNG strikes 5 times, at the same pace by default (RNG is about 6.9 s by default).
- Every icon is now written as :name: (the old fd:name still works). The Discord suffix looked like an egg because ":discord:" fell through to the old community-button picture; suffix names are now :suf_discord: and the Discord community button uses the same picture as the suffix.
- Skull icons are real spinning 3D blocks that bob, like a dropped item (they hold still when menu animations are off).
- Slayers: new sound bank (Slash, Cleave, Slayer reveal, Epic reveal, Ember hum, Farewell) is the default for the Slayer scenes. The Slayer pages, drops and sound slots are unchanged.
- More depth in drop animations: a wider drifting mote field behind and in front of the item with a slow camera sway, a floor shadow and a pale pool of light.
- Cosmetics: suffixes are now referred to as :id: (like Discord emoji). Menu tiles, previews, chat and the tab list all read the same strip file, so changing textures/font/suffix_<id>.png changes it everywhere; a missing picture is logged at startup. The Collection row is now a small "Owned x / total" bar under the Search box. The close X is centred.
- Construction page: the hard hats are replaced by swinging road-work signs.
- Drop animation has more depth: a floor shadow under the item, far motes behind it and soft motes drifting past in front.
- Icons of 3D items (anything that is a skull, like Myth the Fish) are now 3D blocks in every page instead of the flat wiki picture.
- Cosmetics: a Collection bar at the top (x / total owned) fills once; the Aura preview has a spotlight, glowing floor ring, rising sparkles and a bobbing character. Fixed the Discord, Legend, Lucky Dice and Skull suffix icons, redrew Secret, and the Special rarity icon is animated. Non-special name effects keep your server colour and only add an effect on top.
- Experiments icon: the yellow star above the book is gone. Dye icon is a still palette with a glint.
- Pages no longer slide or bounce when you switch. The construction page has depth (haze, moon, drifting clouds, a far skyline, a scaffold tower, a lamp post and fireflies) and the bricks ride smoothly from the pile to the worker's hand.

## 0.4.11
- Fixed: Mythological items (Normal and RNG) stay on screen as long as the words, instead of leaving first. The picture, item and bars fade out together with the text
- Changed: the item spin is one smooth turn: it whirls, slows down and stops facing front exactly when the scene ends. The number of turns follows how long the item is on screen (and your spin settings). Flat items turn like a coin, heads like a block

## 0.4.10
- Fixed: skull items (Mythos Fragment and the other heads) show their 3D head in lists, not the wiki picture
- Fixed: clicking a creature under Mythological Mobs in the sidebar opens that creature, and the open page is highlighted
- Fixed: RNG flashes follow the curved corners and stay inside the window (no more flash or rays over the bars or carved corners)
- Changed: /fd simulate lists Mythological and Treasure only (new: /fd simulate Treasure <Drop>); the other kinds come back with their rebuilds
- Removed: the coloured strip on the left of the chat preview

## 0.4.9
- Changed: the construction page shows "Coming Soon" where the old text was, the crane no longer draws a line to the bricks, and three builders chat on a bench at the right
- Changed: the RNG item stays 1 s longer (RNG default length is 6 s) and a head turns to face you before the scene ends. Flat items sway softly after their flips
- Fixed: Mythos Fragment, Myth the Fish and the other skull drops (Brain Food, Cretan Urn, Crochet Tiger Plushie, Dwarf Turtle Shelmet, Fateful Stinger, Manti-core, Minos Relic, Washed-up Souvenir, Mythological Dye) load their real NEU head. Mythological Dye uses the NEU id DYE_MYTHOLOGICAL
- Changed: coin drops use the Total profit coin in the list and in the animation
- Changed: the chat preview keeps each message on one line (no wrapped leftover)
- Changed: new animated Menu icon; Settings > Cosmetics uses the Cosmetics button icon
- Fixed: the Back rows in Animations and Sounds use the back arrow
- Changed: "Also post in public chat" says it only covers RNG drops with Chat Alert on

## 0.4.8
- Added: each creature (Minos Hunter and the rest) is listed under Mythological Mobs in the sidebar and opens on its own
- Fixed: the curved corners of the bars now match the bar opacity (the scene is cut by the same curve)
- Changed: the RNG scene is much lighter on frames (fewer rays, rings, stars and confetti, drawn in bands)
- Added: a stronger start for RNG (lightning bolts into the circle, ground rings, energy gathering, a violet charge)
- Changed: "Call" is now "Lightning", with a lightning icon
- Changed: Mythos Fragment and Myth the Fish use their real head art
- Changed: the construction page is a night site with a skyline, a builder and a crane raising a brick house, then a loose load and a collapse
- Removed: unused code (WipMock, old bulldozer/crane, unused Tex textures, dead stats and cosmetic helpers)

## 0.4.7
- Changed: Stats page has room again (the Mythological list keeps its normal size)
- Changed: the Treasure hub tile just says how many drops. The Spade, Pet and Erudite settings moved to the main Mythological Ritual page, and every drop chance follows them
- Changed: the Drop Library has no mob shards or Enchanted Gold Ingot. Chimera I is an RNG drop. Sub pages open with their page in the sidebar
- Added: Mythological > Treasure pages in Animations and Sounds (Dig, Open, Reveal, Linger, End), with Normal / RNG, their own icons and a play button
- Fixed: Coins, Griffin Feather, Mythos Fragment and Myth the Fish icons
- Changed: Sent in Chat for Mythological includes the Treasure drops
- Changed: placeholders are small buttons at the far left, outside the menu; the menu never moves
- Changed: the construction page slowly builds a wall, then an accident brings it down and it starts again
- Removed: Item Changes. All items are flat except skulls, which stay 3D
- Changed: the Active lever eases over, no bounce

## 0.4.6
- Fixed: RNG Mythological flashes, rays, shockwaves, sparks, stars and feathers are drawn over the whole screen, not inside the scene's box, so no box edge shows with the bars or the blur on or off. The scene's edges are feathered
- Changed: Mythological size is 50% to 200%, 100% is the default, and Normal and RNG both follow it (it no longer copies the Slayers size)
- Changed: the words follow the animation's position: top row = above the animation, middle row = your Text position choice, bottom row = as before. With bars, they sit in the bar on the animation's side
- Fixed: Text Changes preview shows the words at the top or the bottom for the Text position choice

## 0.4.5
- Fixed: Mythological words come in with the reveal and stay about 2.6 s, so they can be read (Normal and RNG)
- Fixed: Text position now says "Top of animation" / "Bottom of animation", and Normal scenes follow it (words above or below the scene)
- Fixed: Text Changes preview shows a current Mythological drop (Daedalus Stick, "1 in 213 | 0.47%")
- Fixed: Animations and Sounds previews are gone on WIP pages (Slayers, Dye, Farming, Experiments, Mining, Fishing). They show the construction page like Slayers
- Fixed: Mining and Fishing show the construction page again; the in-development animation is removed
- Fixed: Chat preview panel hugs its message, no more empty space
- Fixed: update checker shows "No Releases found yet." when GitHub has none
- Fixed: Settings > Cosmetics sliders and switches refresh the look at once; its icon is the Cosmetics icon
- Added: Sounds > Mythological has Mobs and Treasure pages. Treasure has its own sound set (Dig, Open, Reveal, Linger, End)
- Changed: Stats pages match the Drop Library (Mythological first), WIP pages are tagged and show the construction page

## 0.4.4
- Fixed: Settings page crashed the game (a missing tile). Settings now has Menu, Cosmetics, Prices and Drop Data
- Fixed: Chat Alert follows the state. Normal turns it off, RNG turns it on, and rare drops start as RNG
- Fixed: HUD stays visible on Normal and No Drops. Only RNG hides it
- Fixed: RNG scene had a dark square behind it, and the spinning 3D item showed gaps. Both gone
- Fixed: Bar editor shows the bars again. Pulse text effect now pulses the whole line
- Fixed: Treasure icons (coins show gold, Braided Griffin Feather shows string). Items with no art show the vanilla item
- Fixed: Cosmetics sets replace each other. Picking Beta turns off Special's suffix, aura and name effect
- Fixed: update checker offered v0.9.5 after the downgrade. It now takes the newest release
- Changed: RNG scene is flashier: reveal flash, light pillar, shockwaves, sparks, rune ring
- Changed: Mythological is first in Drop Library, Animations and Sounds, and its sidebar lists Mythological Mobs and Treasure
- Changed: Chat preview sits in a panel above the menu
- Changed: Sounds preview matches the live scene; slots are Call, Shatter, Reveal, Linger, End
- Removed: the Comets animation style

## 0.4.3
- Changed: only Mythological is live. Slayers, Dye, Farming and Experiments now show WIP and the construction site (Drop Library, Animations, Sounds, Chat)
- Changed: Drop Library Show filter is Enabled / Normal / RNG / Disabled. Each drop has an Off / Normal / RNG button, a Preview button and a Chat Alert switch. Normal turns Chat Alert off, RNG turns it on
- Changed: Mythological opens a hub with Mythological Mobs and Treasure pages (Griffin pet on Mobs, spade and Erudite on Treasure), with new icons
- Added: RNG Mythological scene (bars, night sky, god rays, rune halos, feathers), 5 s by default, for the creature ritual and the dig
- Added: No Drops scene (small, above the action bar)
- Added: each creature's drop plays that creature's own scene
- Added: coins from a dig show any exact amount ("You dug out 12,500,000 coins!")
- Changed: animation length is 2 to 7 s

## 0.4.2
- Changed: Mythological Mobs and Treasure Normal animations rebuilt. Small, translucent, pixel style, sitting right above the action bar (HUD stays visible)
- Changed: Normal scene plays 3 s, fades out, and the words stay 1.5 s longer
- Changed: Treasure shows "1 in x | % chance", coins show the exact amount, spade line removed
- Added: Treasure drops in the library have a Chat Alert and Animation toggle and a play button
- Added: treasure chat line ("You dug out ...") now triggers the Treasure animation

## 0.4.1
- Changed: version restarted at 0.4.1 for the full rework. The older entries below keep their original numbers

## 0.9.8
- Changed: the "Coming Soon" building site is tidier. Fixed layout on one ground line, one bulldozer, a crane, a few props, no clutter. Nothing to drag
- Removed: the loose welding sparks

## 0.9.7
- Changed: the menu rebuilds its rows when you click, type, scroll or move, and ten times a second otherwise, not on every frame. Lighter on FPS
- Removed: old leftovers (the old rngdrops sounds and data, two empty files)

## 0.9.5
- Changed: every animation is short and sweet like Farming. Only an RNG drop keeps the long scene with the black bars
- New: Drop Library has an RNG button on every item: Auto (by price), RNG (always the big scene) or Off (never)
- New: Drop Library > Mythological > Treasure, with the drops of each spade (Ancestral, Archaic, Deific), the Erudite reforge and the chance of each per burrow

## 0.9.4
- New: Settings > Cosmetics page. Your six cosmetic toggles moved there, with new options: calm mode, hide your aura in first person, show-within distance, most players with an aura, name effect speed, aura amount and aura size
- New: a new icon for each animation style (Splat, Brush, Slash, Rain, Lightning, Comets, Book, Orb)
- New: Mining and Fishing now show a "coming soon" card with a looping concept scene, progress and what is planned
- New: Diana "You dug out" drops (like Griffin Feather) are read correctly
- Fixed: the Co-op / Public tag no longer sits on top of the chat preview text
- Fixed: Farming > Pest with the Rain style now plays
- Fixed: Animation type text follows the style you pick (Experiments > Orb, Dye > Brush and so on)
- Fixed: Stats drop list no longer counts the mod's own echo as a drop, and old junk entries are removed. Prices are looked up in the background
- Fixed: the mod now reads chat before other mods, so SBO's Diana Message Hider cannot hide a drop from it
- Fixed: a scene that fails to draw is skipped and written to the drop log instead of stalling
- Changed: a new close button (a red cross on a dark round badge)
- Changed: the "‹" is gone from back rows, and the "More" header above Resources is gone

## 0.9.3
- New: a second animation style for Dye (Brush), Farming (Rain), Mythological (Comets) and Experiments (Orb). Pick it on each Animations page, and the preview follows
- New: a name cosmetic shows once over other players' heads, in place of their normal name
- Changed: one Chat preview box shows every message the mod will send, with a cooler look
- Changed: "Also tell public chat" has a shorter description
- Changed: two-drop animations put the items further apart, with a light circle for each item
- Changed: the hotbar and hearts slide with a smoother spring
- Changed: new Party (balloons) and Dye (palette) icons
- Changed: other players' auras are smoother
- Changed: lighter on FPS: cosmetics are checked 4 times a second and reflection lookups are cached

## 0.9.2
- New: a grander RNG animation. The item spins like a 3D block, and slayer, mythological and experiment RNG each have their own stage
- New: Item spin and Item pop size moved to Animations > Changes > Item Changes > RNG
- New: a Mining and Fishing preview screen you can click through
- New: /fd editor opens the Animations and Sounds editors
- New: slayer boss spawn and cocoon lines are replaced by "{Boss} spawned. (time)" and "{Boss} cocooned. Get ready!"
- New: Chat page option to also announce RNG drops in public chat, starting with [FancyDrops] |
- New: other players' name cosmetics show over their heads
- Changed: your own drop message in party, co-op or public chat waits until the animation is over
- Changed: the Superpairs reward lines are hidden so the items don't show early
- Changed: Chat preview sits above the Message format. The public message shows below the other one
- Changed: all animations are 1 second shorter
- Changed: two-drop animations have separate halos and the names sit at the left and right
- Changed: Settings is pinned at the bottom left. Resources moved to Home
- Changed: Home is cleaner. "What the mod does" and the profit line are gone
- Changed: smoother cinematic bar curves and a smooth hotbar-then-hearts slide
- Changed: new Update tracker icon and no more jitter. New Party, Mythological and Dye icons
- Fixed: Duplex I and other "(Name)" books now play their animation
- Fixed: drop lines pasted by players no longer trigger animations
- Fixed: Drop Library Enabled hides drops that have both animation and chat alert off
- Fixed: more slur spellings are blocked in the message format
- Fixed: other players' auras shake less when they move

## 0.9.1
- New: RNG items spin and pop out bigger. Each RNG animation page has "Item spin" (Still to 3x) and "Item pop size" sliders
- New: all sounds redone (40 new ones). Every sound is timed to the animation length and rings out instead of cutting off
- New: click Chop, Split, Reveal, Linger or End under the waves to hear just that sound
- Changed: smoother sound waves
- Changed: cooler Active lever
- Changed: the chat preview types out once instead of repeating
- Changed: Party, Co-op and Public icons now have their own animations instead of the shine
- Changed: searching for cosmetics makes the Cosmetics button glow, and it is gone from the pages menu
- Changed: smaller TIP box, new tips about the mod, a new tip every 10 seconds
- New: Home has an Update tracker that checks GitHub for new releases. The Jump to section is gone
- Changed: the GitHub button opens the FancyDrops repository
- Fixed: the Animations icon had black spots on the white bar

## 0.9.0
- Release: everything from 0.8.52 to 0.8.60 (cosmetics settings, animated suffixes and auras, building site, admin save, SkySoft tab fix, Active lever)

## 0.8.60
- Fixed: the animated name effect in SkySoft's Better Tab. It keeps its own cached copy of the tab list, so FancyDrops now tells it to re-read while a name is moving

## 0.8.59
- Changed: the Enabled / Animated / Chat Alert / Disabled filter now sits inside the Drop Library pages, under the header, so it no longer covers the Active lever

## 0.8.58
- Fixed: the new animated suffix icons were only drawn in the menu list, not in chat, the tab list or the preview (those use their own sprite files). Every suffix sprite is now the new animation
- Note: when Active is OFF, your own suffix, name effect and aura are hidden on your screen

## 0.8.57
- The Active switch is now a lever at the top left above the menu: throw it right for on, left for off

## 0.8.56
- Pest icon: the ladybug flaps its wings. Dye (a flask that takes a falling drop and changes colour), Mythological (a flashing lightning bolt in laurel) and Experiments (the book flips its pages) have real animations instead of a shine
- Slayers > Animation Type > Default uses a spinning gold coin instead of a sword
- Drop Library uses the spinning diamond icon
- The Active switch moved above the menu, top right, and looks like a real switch that slides on and off
- The small line under a category title is smaller than the title
- Admin: changes save themselves a moment after every click. The picker now shows exactly what is saved (before, as the owner you always saw everything ticked again)

## 0.8.55
- Newcomer, Discord, First Drop, Collector, Secret, Hoarder and Legend suffixes have new animated icons (the Discord one matches the new Discord logo)
- Auras have a moving animation: while you move, a curtain of light streams out behind you along your path. It follows your speed, so it shortens and slows as you do, fades into the resting aura as you stop, and the idle animation starts after 5 seconds as before
- The log now lists tab-related mods, to help find out why a custom tab list freezes the name effect

## 0.8.54
- Every suffix icon is redrawn with real motion (spinning gem and clover, swinging hammer, beating heart, flickering flame, bobbing ghost...) instead of a shine; Sparkle uses the new star
- Cosmetic tiles animate their icon when you hover them
- Stats > Overview: removed the Categories tiles

## 0.8.53
- Removed the stray line beside the sword icon
- Pest Drop sounds page has its own names (Suck, Pop, Shoot) and icons
- New icons for the announce buttons: Party, Co-op, Public, Off
- The Sounds page now uses the animated megaphone icon
- New Discord, Patreon and GitHub icons, centred in their buttons
- Admin: suffixes, name effects and auras use their set's colour; Save writes cosmetics.json to GitHub from the mod (paste a GitHub token once in the Admin Panel)

## 0.8.52
- Settings > Menu: "Cosmetics" now has switches for your own suffix, name effect and aura, and for other players'
- Chat: removed the Rules section and the minimum price; chat sends whatever you switched on
- RNG Drop scene now plays for expensive drops even if the item is off; a number box (1m to 1b coins) appears under the switch when it is on
- Removed the Serif menu font and the Community buttons switch (the buttons are always shown)
- Building site: things fall with gravity and stack; only items, boulders and bulldozers can be moved (crane, cones and hats are fixed); smaller cones; new wheelbarrow, boulder, planks, sand, pipes, toolbox and cement mixer
- Fixed the white icons (palette letters clashed with the eye and flame colours)
- New rotating gear for Settings and the admin panel
- Page changes no longer bounce (no overshoot on the sidebar, no hopping icons)
- Aura preview: no more turning; a gentle sway, glowing floor rings, light beams and orbiting lights
- Admin picker lists sets and cosmetics by rarity, like the Cosmetics page
- The "Contrib" suffix is now "Builder"

## 0.8.51
- The building site no longer overlaps: bulldozers turn round before they touch anything, and you can drag every item around the box (it slides to a free spot when dropped)
- Fixed the pickaxe and rod handles turning white (a palette letter clashed with the tint colours)
- New Farming icon: a hoe digs up a carrot. Sounds, Chat, Stats and Animations icons now animate, and every menu icon hops up and down
- The Settings gear is symmetric and centred
- Removed the white caps on the sound graph bars
- Ray of Helios has new short sounds (Beam, Flare); all of its sounds end well inside the animation
- Admin panel redesigned as a clean card with check tiles and counts; Beta and Common set counts now include the owner
- Players per cosmetic counts sets by who holds every member

## 0.8.50
- Mining icon: a steel pickaxe swings onto iron ore, cracks it three times, then breaks it into pieces (smaller head, 2 pixel stick)
- Fishing icon: the rod flexes back and casts, the line pays out to a bobber that splashes down and floats over the shimmering river
- The Active button shows a faceted diamond spinning over a glowing ring, with sparkles
- The Default slayer drop has a new sword icon
- Icons can now be made of frames, not only simple motion

## 0.8.49
- Pages that are not built yet say "Coming Soon" with a small TM, and show a busy building site: two bulldozers driving with dust, a swinging crane, brick stacks, barriers and sparks
- Your original FancyDrops picture is back in the menu header and as the mod icon

## 0.8.48
- The menu preview now plays exactly like the real animation: same speed, the words stay 2 seconds after the scene, the same fades, then a short rest before it repeats

## 0.8.47
- New short sound effects for Farming and Pest Drop (Reap, Rustle, Sprout, Breeze, Settle, Vacuum, Squeak). Every sound is now at most 2.6 s, so none run past the animation
- The Sounds graph draws each sound's real loudness, with no random jitter
- New Farming and Pest icons, and a new FancyDrops icon
- Big menu icons float up and down and animate
- Page subtitles under Drop Library, Animations, Sounds and Stats now match their pages
- The menu's X no longer shows over the admin panel

# FancyDrops changelog

Release notes are written here. The release workflow copies the section for the version you pick into the GitHub release, so what you read here is what players see.

## 0.8.46
- New Pest Drop animation (Animations > Farming > Pest): a vacuum drops in over the field, dust and leaves stream into its nozzle, the pest is plucked off the ground and sucked in, the vacuum gulps and puffs it away, and the drop shoots out of the nozzle. It has its own length, sounds and Play button, and plays for every drop from the Pests list.
- Fixed a compile error in the two-item drop code.

## 0.8.45
- Two-item drops redone: the two items now fly out one after the other in an arc, are drawn a little smaller so they no longer overlap, and each one's name and details sit right under it instead of far out at the sides of the screen.

## 0.8.44
- Beta set is now just Flame and Pulse. Crown, Gem, Clover, Heart, Bolt, Snow, Star, Ghost, Shimmer, Ripple and Glow are Rare cosmetics, in dark blue (still held by players who downloaded before v1.0).
- Rarities now run Common, Rare, Beta, Special.
- The gift panel shows the name of the player you are giving to.
- Community buttons: the flat square glow behind them is gone; they and the Cosmetics button glow with a soft rounded halo.

## 0.8.43
- Auras stay with you: they no longer trail behind in the world. While you move the aura swirls round you faster; stand still for 5 seconds and a calm resting animation fades in (two strands of light winding up past your head and a soft ring at your waist).

## 0.8.42
- The Admin Panel is now a button outside the menu (red, above the Cosmetics button; only you see it). It opens a full-screen panel with the players-per-cosmetic toggle and the box to pick what to give.

## 0.8.41
- ImTra now has every cosmetic: the Special sets, the whole Beta line and the earned ones are all in cosmetics.json. In the Admin Panel picker your own ID shows everything ticked, and the Beta set ticks all 13 of its items.

## 0.8.40
- Farming animations no longer hide your hotbar and hearts, and they play faster (about 2.6 seconds, no slow-down).

## 0.8.39
- The hover card on a player's name now matches the chat: the gradient [FancyDrops] on top, a divider, then one tidy line per cosmetic (name in its colour, rarity beside it).
- Custom tab list: the list is now refreshed right away every time a name effect moves, and the log says what happened (look for "[FancyDrops] tab:" lines). Not confirmed fixed.

## 0.8.38
- Commands tidied: only the full names show in Tab now (`/fd simulate`, `/fd magicfind`, `/fd cosmetics`; the short forms are gone).
- `/fd simulate` has a new shape: `Slayer <Boss> <Drop 1|Nothing> [Drop 2] [mf]`, `Mythological <Mob> <Drop 1|Nothing> [Drop 2] [mf]`, `Dye <Dye>`, `Farming <Crop|Pest> <Drop 1> [ob]`, `Experiments <Drop 1> [Drop 2]`. Tab completes every step.
- Mythological mobs now each have their own colours in the ritual scene. Farming simulations use the Overbloom you type.
- Redrawn: the pickaxe (like Minecraft's) and the fishing rod (a river, a bobbing float). Work in Progress pages now look like a construction site with hazard tape, cones and hard hats.
- Chat > "Mythological" label fits. The aura preview is its own stage with no black lines behind the skin, and the aura circles are gone.
- The Cosmetics button glows and sparkles; the community buttons glow softly too.
- Admin Panel in Cosmetics (owner only): see how many players hold each cosmetic and pick what to give someone; it copies a ready cosmetics.json.
- Build: upload-artifact updated to v5 (Node 20 warning).
- Not fixed yet: the name effect in Skyblocker custom tabs.

## 0.8.37
- Much lighter menu: every icon is drawn as one picture from a texture instead of hundreds of tiny rectangles, the skin preview is a single picture, only the tile you point at animates in Cosmetics, and the aura preview draws fewer particles.
- Icons now move the way the thing they show would: the gear and reload ring turn, the pickaxe and fishing rod swing, wheat sways, the rest get a sweep of light. Nothing bobs up and down any more, and the sidebar and community buttons stay put.
- Redrawn: Mining, Fishing, Farming, Animations, Work in Progress (a hard hat) and the FancyDrops chest (also the mod icon). The Slayers sword is back to the old one. Every icon is centred on its square.
- Circles behind icons are gone everywhere. Aura particles no longer have a halo.
- Fixed: Settings > Menu was blank. The aura preview now finds your real skin (several ways, with log lines if it cannot).
- /fd cosmetics (also cosmetic, cosm, cm) opens the cosmetics menu.
- Reload drop data and Copy ID now show a pop-up too.
- The Mini font is removed.

## 0.8.36
- Fixed a build error in the draggable scroll bar (a variable used outside its method).

## 0.8.35
- Redrawn icons: Nothing, Default, Rare, RNG, Home, Mining, Fishing, Reload, the gears (Settings and WIP), Tip card, GitHub, Patreon and more. They no longer sit on circles, and they are centred. The Griffin has a new icon that follows the pet's rarity colour.
- Mythological: the Griffin pet row, its list and the page accent use the rarity colours (Common to Mythic).
- Text Changes: effects now live on four tabs (Entrance, Motion, Colour, Exit) with roomier tiles and plain "1 needed / 1 optional / Up to 2" labels. Six new effects: Slide in, Flip in, Swing, Gold, Slide out, Melt.
- Bar Changes: the Edge line is gone. New Curved corners and Soft edge. Bar, Text and Item previews are centred and the wording under them is gone.
- Fonts: the odd ones are removed. New "Mini font", a tiny pixel font.
- Pop-up notices no longer stack when you click the same thing again.
- Sliders glide instead of snapping. The scroll bar on the right can be grabbed and dragged.
- Auras now trail behind the player in the world. The back of the skin preview no longer shows dark lines.
- The Cosmetics button moved to the bottom left of the menu with sparkles. The LIVE dock icon sits inside its box and the details are coloured.
- WIP banner is cleaner. Category and animation buttons now say what is inside, and Sounds reads "Sound Editor".
- Lighter aura preview for a quicker menu.

## 0.8.34
- Fixed a build error in Gfx (a text type that does not exist under that name in 26.2).

## 0.8.33
- Drop Library: the top button inside a boss / creature now reads "< Slayers" (or the page you came from) and takes you back to where you were in that list.
- New hand-pixeled icons for every Slayer boss and every Mythological creature, plus new icons for Spirit, Animated menu / icons, Tip card, Other players' cosmetics, Back and Cosmetics.
- Animation style (Sword / Spirit): the descriptions sit on their own buttons, and Spirit has its own icon.
- Text Changes: Shrink now shows in the preview. The menu has page tabs (Effects, Layout, Font) and a font picker, bold and outline switches.
- Bar Changes: pages for Size and Colour, with new bar opacity, side pillars and an edge line. Item Changes: pages for Size and Extras, with new float height and spin direction. All three Changes menus share one layout.
- Settings > Menu is tidied into Look, Layout, Notices and Other players' cosmetics, and has a new Menu font picker (Clean, Bold, Serif, Mono, Narrow, Enchanting, Illager).
- Live preview panel: LIVE badge, the animation's icon and name, and a frame in its colour.
- The Cosmetics button moved outside the menu (bottom right, under the community buttons).
- Buttons, tiles, and outlines now have rounded corners.
- Skin preview: logs how many turns it built, so a missing back view can be traced; the aura still turns with the figure.

## 0.8.32
- Menu animations redone: the window springs open and shrinks away on close, sidebar tabs slide in one by one, the tab highlight is a spring, and each page's rows rise in with a stagger.
- Redrew the suffix icons by hand in one consistent style (outline, three tones, one highlight), in the menu, chat and tab list.
- Aura preview: the aura now turns with the figure, so it wraps the back too.

## 0.8.31
- Cosmetics is its own window now. The button is at the bottom left of the menu (no longer in the sidebar); it opens a separate window with no sidebar, with the chat and tab preview above it and the aura preview beside it. The X or Esc takes you back to the page you were on.
- Turning the mod off (the Active switch) also hides cosmetics on your own screen: yours and other players'. Other players still see yours.
- New settings (Settings page): show other players' suffixes, name effects and auras, each on its own switch.
- Name effects now move in custom tab lists too. Skyblocker's tab only copies the player list once in a while, so the mod asks it to copy again a few times a second while a name is moving.
- The TEST strip under the menu is gone.

## 0.8.30
- The Bug Finder set is new and no longer green: Hazard (a blinking warning triangle), Critical (hot red-orange with a warning stripe and a letter flashing white) and Alarm (red and amber sparks circling you, a ring of light climbing your body, sparks flaring as it passes).
- Tile backgrounds: every Special set has its own (twinkling stars, warning stripes, an aqua wave, gold embers), Beta tiles have a passing light, Common tiles have none.
- Less lag on the Cosmetics page: the menu is built once per frame instead of twice, and unlock checks are remembered for a moment. The mod also writes how long each part takes to the log (`[FancyDrops] perf`), so any remaining slowness can be pinned down.

## 0.8.29
- Cosmetic tiles all line up the same way now: the icon in one column on the left and the name right after it, so no tile has a big empty gap on one side.
- Suffix icons sit flush at the bottom of their slot (the crown had a gap under it), and the extra spaces around them in chat and the tab list are gone when a space is already there.

## 0.8.28
- Beta and Common name effects have their own colour now (Shimmer ice-blue, Pulse pink, Ripple teal, Glow gold, Fresh mint). On a white name, like in the tab list, they were all white and could not be seen. A coloured name keeps half of its own colour. Pulse is not as dark any more.
- Name effect tiles tell you what the effect does when you hover them.

## 0.8.27
- Shimmer (Beta) was washing short names out to white: its band of light covered the whole name. The band is narrow now, so your name keeps its colour. Glow is a bit gentler too.

## 0.8.26
- Auras follow you smoothly when you move: they use your exact position for each frame (before, they lagged and jittered). Sparkles, stars and dust that rise off you are now left behind in the world as you run, like real particles, and settle back into the same look when you stand still. Patreon's embers keep their look.
- Cooler name effects: Starlight (gold flowing amber to cream, a white comet with a pink tail, single letters flaring), Glitch (calm green with a scan line, and every couple of seconds the name tears into cyan, magenta and white), Aqua Flow (two crossing waves, foam crests and a sweeping light). They only change colour, so chat lines never shift.
- Suffix icons sit lower, in line with the text, in chat and the tab list.

## 0.8.25
- The LOCKED shake is slower and eases out, so it looks smoother. The Sets header no longer says every set has an aura (Beta and Common do not).

## 0.8.24
- Name effects keep animating on locked tiles (and the lock dimming is lighter), so you can see how the name would look.

## 0.8.23
- Cosmetic tiles: the icon and the name now sit together in the middle of the tile (no empty space on one side).
- The Suffix, Name Effect and Aura sections are now truly in rarity order: Special, then Beta, then Common. Newcomer no longer sits between the Beta items.

## 0.8.22
- One Cosmetics page: Sets, Suffix, Name Effect and Aura, each rarest first. The Special Sets header and the info line at the top are gone.
- Grey is now Common. New Common set (Newcomer + Fresh) and Beta set (Crown + Shimmer): a suffix and a name effect, no aura.
- Hover a tile to see its rarity (with a small pin), Unlocked or Locked, and how to get it. Set tiles also show how many items you have (like 3 / 3). The "Click to wear it" line is gone.
- Every tile of a set shares the set's colours, so the page looks consistent.
- Faster Cosmetics page: the 3D skin is drawn once per angle in the background and then only shown, and the tiles no longer animate heavily. The preview turns when you hold the left mouse button and drag; moving it is gone.
- Fixed the random vertical line in Static and Stardust (the clock was too large for the maths). Nicer Special (two turning rings of stars, bursts), Bug Finder (a scan line) and Contributors (a halo and rising stars) auras. Patreon is unchanged.
- The settings gear is centred.

## 0.8.21
- The Cosmetics page is compact now: Special Sets on top (one row per set: the set tile, then its suffix, name effect and aura), then one Collection grid, rarest first. Beta cosmetics sit in the same grid as the earned ones. Boxes are coloured by rarity: gradient for Special, green for Beta, grey for Earned. Every tile says what it is (Suffix, Name effect, Aura). The Wear set button is now the set tile and only works if you own the set. Icons no longer bounce.
- The main menu stays in the middle of the screen on the Cosmetics page. The chat and tab preview sits above it and the aura preview to its left; the window only gets a little smaller to make room.
- The aura preview shows your skin as a 3D model with a gentle turn. Drag to move it, right-drag to turn it.
- Auras were drawing as huge squares in third person (the camera was not found). Fixed, and the particles are now small and soft. Redesigned: Twinkle (stars and a ring of light), Static (a climbing scan line and red/cyan split pixels), Stardust (a double helix) and Gilded Embers.
- New name effects: Starlight (gold with a running band of light), Glitch (a green scan sweeps over the name), Aqua Flow (flowing water with glints). Royal Gold no longer makes the chat jump around (it used bold letters).
- Name effects now update every frame in the tab list, so they are as smooth as in chat.
- The suffix in the tab preview has its own colours now.
- Shorter cosmetic names so they fit the tiles.

## 0.8.20
- Special cosmetics are now four sets, each a suffix + name effect + aura, with a Wear set button: Special Set (Sparkle, Starlight, Twinkle), Bug Finder Set (Glitch Spark, a calmer Glitch, Static), Contributors Set (Contributor, Aqua Flow, Stardust) and Patreon Set (Patron, Royal Gold, Gilded Embers). Other players see the whole set you were given.
- Auras are drawn with our own particles (no Minecraft particles) and also play in a new Aura preview menu on the left, using your own skin. Drag the skin to move it. Hover an aura tile to preview it.
- The chat and tab preview moved into their own menu above the window. It uses the same animated suffix picture as the real chat.
- Beta name effects are much stronger, and name effect tiles now show the name itself wearing the effect.
- New earned suffixes: Hoarder (100 rare drops) and Legend (500 rare drops).
- Changing what you wear now redoes the chat lines already on screen. Chat name effects try a new way to animate and write what they find to the log.
- Test menu changes also update chat.

## 0.8.19
- Cosmetics page redone: Special Cosmetics first, then Beta, then Earned (was Everyone's). Suffixes, Name effects and Auras each have their own section, the words under the tiles are gone (hover a tile to see how to get it), and the tiles no longer flash.
- Hover a player's name or suffix in chat to see a FancyDrops card: which cosmetics they have and how they got each one.
- Name effects animate in chat now, like in the tab list. Glitch keeps jumping between letters, Royal Gold has a glint running along the name.
- The Secret cosmetic is now given when you find the hidden button (the test menu no longer overrides it).
- The Stardust aura is bigger and easier to see.
- Test menu: add or remove single cosmetics on yourself, give or remove all, reset. The old page switch is gone.

## 0.8.18
- New Cosmetics page with three lines. Beta Cosmetics (the old suffixes plus four gentle name effects) stay with everyone who downloaded before v1.0; anyone who joins later sees them locked. Special Cosmetics are given by hand, one by one: Sparkle, Royal Gold (Patreon), Glitch (bug reports) and Stardust, a particle aura (contributors). Everyone's Cosmetics are earned by playing and stand still: Newcomer, Discord, First Drop, Collector, and a hidden one.
- Name effects (IGN) keep your server colour and add light and movement; they animate in the tab list and are frozen looks in chat.
- Pressing a locked cosmetic shows a LOCKED! pop-up.
- cosmetics.json now lists each Special cosmetic separately (special: sparkle / patreon / bughunter / contributor), so giving one person one cosmetic does not give the others.
- A small test menu under the main menu switches the new page on and off, simulates a new player and unlocks everything. It goes once the page is tested.

## 0.8.17
- A Refresh button on the Cosmetics page (and on the disabled panel for other accounts) that fetches cosmetics.json again right away.

## 0.8.16
- Cosmetics owners are now stored as codes (SHA-256 of the UUID), not as UUIDs, in the mod and in cosmetics.json. New owners and their suffix come from cosmetics.json on GitHub, so adding one needs no new build. The Cosmetics page shows your own ID code with a Copy ID button.
- The Cosmetics preview: the RARE DROP! line now sits above the chat line, and the tab list mock is closer to SkyBlock's (three columns, ranks, header and footer) with moving ping bars.

## 0.8.15
- Everyone with the mod now sees ImTra's suffix, in chat and in the tab list. What it is comes from cosmetics.json in the GitHub repo.
- The Cosmetics page and picking a suffix are only for ImTra's account, in every build.
- The tab list now uses the same animated picture as chat, so the two always run at the same speed.
- Removed the leftover resource-pack code and the unused font files.

## 0.8.14
- The suffix in chat no longer covers the colon after the name (a small gap), and the tab suffix plays at the same speed as the chat one.

## 0.8.13
- The chat suffix is animated again, this time as a small animated picture inside the mod itself (no resource pack, no flicker). If this Minecraft can't draw it, chat falls back to the still glyph and the reason is printed in the game log.

## 0.8.12
- Rare drops this mod doesn't list (like "VERY RARE DROP! (Duplex I)") are now tracked too; the name in brackets was being read as a stat and the whole line was dropped.
- Any boss kill plays exactly one animation: a drop we don't list no longer cancels the "nothing dropped" scene, and it doesn't play that scene over a real drop.
- Animations play about 30% slower.
- The chat suffix now uses the same glyph as the tab list (standing still); the odd flickering one is gone, and the built-in resource pack is removed, so there is nothing to install or switch on.
- A cocooned boss plays its scene faster, because the next boss is already coming. One kill is counted once even when "cocooned" and "quest complete" arrive seconds apart.
- While searching, the sidebar lists only the sub pages that match.
- The kill line is now written to the drop log (config/fancydrops/drop-log.txt) to help trace a missing animation.

## 0.8.10
- The animated chat-suffix resource pack is now built into the mod: installed and turned on automatically (can be switched off in Options > Resource Packs).

## 0.8.9

### Drops and tracking
- Drops are counted the moment they drop. A slow or failed price lookup can no longer lose one.
- A drop line that prints a moment after the kill line is no longer missed. A kill now waits about 1.6 seconds for a drop before the "Nothing :(" scene plays.
- Drop names are matched loosely (case, spaces and symbols are ignored), so a drop is not lost to a small spelling difference.
- A rare-drop line the mod does not list stays in chat instead of vanishing.
- New log for troubleshooting: `config/fancydrops/drop-log.txt` records every drop line heard and what happened to it.

### Animations
- **Animations > Changes** has three editors: Bar Changes, Text Changes and Item Changes. All three now share one wide layout with a big live preview on the left and the controls on the right.
- **Text Changes:** 19 effects in four groups: Entrance (pick 1, required), Motion (up to 1), Colour (up to 2) and Exit (up to 1). Hover an effect to see it play on the preview. Saving needs an Entrance effect. Text size, title size, sub title size and delay sliders, text position and what to show (drop chance, price, price source, Magic Find and Overbloom icons).
- **Item Changes:** item size, spin, glow and bob sliders. Extras for 3D items, floating, glints and an outline. Every click restarts the item preview.
- The words now stay on screen 2 seconds longer than the scene.
- The bottom-centre position sits above the held item's name.
- While a scene plays, the hearts, bars and hotbar slide down. Afterwards the hearts slide up first, then the hotbar.
- Animation length runs from 5 to 10 seconds per animation.
- Farming text appears 0.5 seconds after the reveal and was redrawn to look cleaner.

### Cosmetics
- New **Cosmetics** page with the preview at the top: a chat line and a tab-list mock-up with your suffix.
- 10 animated suffixes: Sparkle, Crown, Gem, Flame, Lucky Clover (v0.5.0) and Heartbeat, Lightning, Snowflake, Shooting Star, Ghost (v0.8.0).
- The suffix shows in the SkyBlock tab list, animated. In chat it shows as a still image. The optional **FancyDrops Animated Suffix** resource pack (download below) makes the chat suffix animate too.
- Open to everyone in the Dev build, whitelisted players only in the Main build.

### Menu
- Home: a rotating tip card with 14 tips.
- **Search** opens whole pages. Typing `cosmetics` or `mythological` shows the page (or every page of that name) first, then single matching settings. The sidebar shows only pages with a match, each with all its sub pages. `/fd <word>` does the same.
- Every main page has its own colour, its sub pages a lighter one.
- **Active** is now a battery: full when on, drained when off.
- New round **X** buttons: one on the main menu, one on every Changes editor. In an editor the X discards your edits straight away, without the Esc-twice warning.
- New **Settings > Menu** options: tip card on or off, community buttons on or off.
- Drop Library: boss tiles and a Show filter strip. Experiments sub pages have the Preview, Animation and Chat Alert columns and a switch-all row.
- Stats > Overview was rebuilt with clickable category tiles.
- Sounds: pausing and resuming the live sound preview carries on with the sound that was playing.
- A new floor-drop icon replaces the old hero icon.
- Cornucopia now uses the wiki's own picture.

### Build
- The build label (Main or Dev) follows the git branch, so a build from `main` says Main. Use `-Pchannel=dev` to force Dev.
- Releases are no longer created automatically. Pushing only builds the jar (download it from the workflow run). Publish a release by hand with the **Release** workflow.

### Animated chat suffix pack
Download `FancyDrops-Animated-Suffix.zip` from this release, put it in your `resourcepacks` folder and switch it on in Options > Resource Packs. If the game calls it incompatible, enable it anyway.
