# FancyDrops changelog

Release notes are written here. The release workflow copies the section for the version you pick into the GitHub release, so what you read here is what players see.

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
