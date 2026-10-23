# FancyDrops changelog

Release notes are written here. The release workflow copies the section for the version you pick into the GitHub release, so what you read here is what players see.

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
